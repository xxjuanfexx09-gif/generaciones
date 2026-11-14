package com.generaciones.boss;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.generaciones.ModContent;

/**
 * El jefe final: un Devastador mejorado llamado "Guardián de Chispas".
 * Tiene barra de jefe, lanza pulsos que empujan y, al morir, suelta el Núcleo Estelar.
 * Solo trabaja mientras existe un Guardián; el resto del tiempo no cuesta nada.
 */
public final class Guardian {
	public static final String TAG = "generaciones_guardian";
	private static final float VIDA = 400f;
	private static final Map<UUID, ServerBossEvent> BARRAS = new HashMap<>();
	private static final Map<UUID, ServerLevel> MUNDOS = new HashMap<>();

	/** Invoca al jefe final: El Eclipse (con su cinemática de entrada). */
	public static void summon(ServerLevel level, BlockPos pos) {
		Eclipse boss = com.generaciones.ModContent.ECLIPSE.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return;
		}
		boss.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
		level.addFreshEntity(boss);
	}

	private static void track(Entity boss, ServerLevel level) {
		BARRAS.computeIfAbsent(boss.getUUID(), id -> new ServerBossEvent(id,
				Component.translatable("entity.generaciones.guardian"),
				BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10));
		MUNDOS.put(boss.getUUID(), level);
	}

	public static void register() {
		// si el mundo se recarga con un Guardián vivo, vuelve a mostrar su barra
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.entityTags().contains(TAG)) {
				track(entity, level);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(Guardian::tick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!entity.entityTags().contains(TAG) || !(entity.level() instanceof ServerLevel level)) {
				return;
			}
			BlockPos pos = entity.blockPosition();
			Block.popResource(level, pos, new ItemStack(ModContent.NUCLEO_7));
			Block.popResource(level, pos, new ItemStack(ModContent.ESTRELLA_CHISPAS, 3));
			level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1f, 1f);
			for (ServerPlayer p : level.players()) {
				if (p.distanceToSqr(entity) < 96 * 96) {
					p.sendSystemMessage(Component.translatable("msg.generaciones.guardian.vencido").withStyle(ChatFormatting.GOLD));
				}
			}
			ServerBossEvent bar = BARRAS.remove(entity.getUUID());
			MUNDOS.remove(entity.getUUID());
			if (bar != null) {
				bar.removeAllPlayers();
			}
		});
	}

	private static void tick(MinecraftServer server) {
		if (BARRAS.isEmpty() || server.getTickCount() % 10 != 0) {
			return;
		}
		Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARRAS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ServerBossEvent> e = it.next();
			ServerLevel level = MUNDOS.get(e.getKey());
			Entity ent = level == null ? null : level.getEntity(e.getKey());
			ServerBossEvent bar = e.getValue();
			if (!(ent instanceof LivingEntity boss) || !boss.isAlive()) {
				if (ent != null && !ent.isAlive()) {
					bar.removeAllPlayers();
					it.remove();
					MUNDOS.remove(e.getKey());
				}
				continue;
			}
			bar.setProgress(boss.getHealth() / boss.getMaxHealth());
			for (ServerPlayer p : level.players()) {
				if (p.distanceToSqr(boss) < 64 * 64) {
					bar.addPlayer(p);
				} else {
					bar.removePlayer(p);
				}
			}
			// cada 5 s: pulso de chispas que daña y empuja a quien esté cerca
			if (server.getTickCount() % 100 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY() + 1.5, boss.getZ(), 80, 3, 1, 3, 0.3);
				level.playSound(null, boss.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.6f, 1.6f);
				for (ServerPlayer p : level.players()) {
					if (!p.isCreative() && !p.isSpectator() && p.distanceToSqr(boss) < 7 * 7) {
						p.hurtServer(level, level.damageSources().magic(), 5f);
						p.knockback(1.6, boss.getX() - p.getX(), boss.getZ() - p.getZ(), level.damageSources().magic(), 0f);
					}
				}
			}
		}
	}

	private Guardian() {
	}
}
