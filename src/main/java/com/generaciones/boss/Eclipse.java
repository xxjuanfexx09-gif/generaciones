package com.generaciones.boss;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.generaciones.ModContent;

/**
 * EL ECLIPSE, Devorador de Estrellas: el jefe final.
 * Un núcleo flotante con un anillo de cristales y 4 manos. Entra con una cinemática,
 * dispara rayos, golpea el suelo con las manos y en la fase 2 hace llover cristales.
 */
public class Eclipse extends Monster {
	public static final int INTRO = 100;
	private static final float VIDA = 900f;

	private static final EntityDataAccessor<Integer> DATA_INTRO = SynchedEntityData.defineId(Eclipse.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FASE = SynchedEntityData.defineId(Eclipse.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_GOLPE = SynchedEntityData.defineId(Eclipse.class, EntityDataSerializers.INT);

	private final ServerBossEvent barra = new ServerBossEvent(Mth.createInsecureUUID(this.random),
			Component.translatable("entity.generaciones.eclipse").withStyle(ChatFormatting.DARK_PURPLE),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

	private int rayo = 60;
	private int golpe = 120;
	private int lluvia = 60;
	/** Ticks que le quedan "cansado" en el suelo después de golpear (se le puede pegar con espada). */
	private int cansado;
	/** Golpes de manos hechos desde el último descanso: solo descansa cada 3 golpes. */
	private int golpesSinDescanso;
	private double baseY = Double.NaN;
	private Vec3 objetivoGolpe;
	private final List<long[]> impactos = new ArrayList<>(); // x, y, z, tick en que cae

	public Eclipse(EntityType<? extends Eclipse> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.noPhysics = true;
		this.xpReward = 200;
		this.setPersistenceRequired();
		this.barra.setDarkenScreen(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, VIDA)
				.add(Attributes.ARMOR, 10)
				.add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_INTRO, INTRO);
		builder.define(DATA_FASE, 1);
		builder.define(DATA_GOLPE, 0);
	}

	public int intro() {
		return this.entityData.get(DATA_INTRO);
	}

	public int fase() {
		return this.entityData.get(DATA_FASE);
	}

	/** Ticks restantes de la animación de golpe de manos (0 = quietas). */
	public int golpeAnim() {
		return this.entityData.get(DATA_GOLPE);
	}

	// ---------------------------------------------------------------- IA

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (Double.isNaN(baseY)) {
			baseY = getY();
		}
		int intro = intro();
		if (intro > 0) {
			tickIntro(level, intro);
			return;
		}
		if (fase() == 1 && getHealth() < getMaxHealth() * 0.5f) {
			entrarFase2(level);
		}
		barra.setProgress(getHealth() / getMaxHealth());

		Player target = level.getNearestPlayer(getX(), getY(), getZ(), 48, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		if (cansado > 0) {
			tickCansado(level, target);
			return;
		}
		mover(target);
		if (target == null) {
			return;
		}
		lookAtTarget(target);
		boolean f2 = fase() == 2;

		if (--rayo <= 0) {
			rayo = f2 ? 30 : 50;
			disparar(level, target);
		}
		int g = golpeAnim();
		if (g > 0) {
			entityData.set(DATA_GOLPE, g - 1);
			if (g == 12 && objetivoGolpe != null) {
				impactoManos(level, objetivoGolpe);
			}
			if (g == 1 && ++golpesSinDescanso >= 3) {
				golpesSinDescanso = 0;
				empezarCansancio(level);
			}
		} else if (--golpe <= 0) {
			golpe = f2 ? 100 : 140;
			objetivoGolpe = target.position();
			entityData.set(DATA_GOLPE, 30);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.6f);
		}
		if (f2) {
			if (--lluvia <= 0) {
				lluvia = 40;
				for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40), EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
					for (int i = 0; i < 2; i++) {
						double x = p.getX() + random.nextGaussian() * 3;
						double z = p.getZ() + random.nextGaussian() * 3;
						impactos.add(new long[] {Math.round(x * 100), Math.round(p.getY() * 100), Math.round(z * 100), level.getGameTime() + 25});
					}
				}
			}
			tickLluvia(level);
		}
	}

	private void empezarCansancio(ServerLevel level) {
		cansado = fase() == 2 ? 80 : 100;
		sonido(level, SoundEvents.WITHER_HURT, 2f, 0.5f);
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(this) < 48 * 48) {
				p.sendOverlayMessage(Component.translatable("entity.generaciones.eclipse.cansado").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
			}
		}
	}

	/** Baja al suelo junto al jugador y se queda quieto, mareado: es el momento de pegarle con la espada. */
	private void tickCansado(ServerLevel level, Player target) {
		cansado--;
		double sueloY = target != null ? target.getY() : baseY;
		Vec3 destino = new Vec3(getX(), sueloY + 0.2, getZ());
		if (target != null && distanceToSqr(target) > 36) {
			// si quedó lejos, se acerca un poco al jugador mientras cae
			destino = destino.add(target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.6));
		}
		Vec3 d = destino.subtract(position());
		setDeltaMovement(d.scale(0.15));
		move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
		if (tickCount % 5 == 0) {
			level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 3.2, getZ(), 6, 0.6, 0.2, 0.6, 0.05);
		}
		if (cansado == 0) {
			sonido(level, SoundEvents.WARDEN_ROAR, 2f, 0.9f);
		}
	}

	private void tickIntro(ServerLevel level, int intro) {
		int t = INTRO - intro;
		if (t == 0) {
			anunciar(level, Component.literal("EL ECLIPSE").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
					Component.translatable("entity.generaciones.eclipse.subtitulo").withStyle(ChatFormatting.LIGHT_PURPLE));
			sonido(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 4f, 0.6f);
			sonido(level, SoundEvents.END_PORTAL_SPAWN, 3f, 0.7f);
		}
		// sube lentamente mientras las chispas giran hacia él
		setPos(getX(), baseY + 6.0 * t / INTRO, getZ());
		setDeltaMovement(Vec3.ZERO);
		double r = 8.0 * (1.0 - t / (double) INTRO) + 1.5;
		for (int i = 0; i < 6; i++) {
			double a = t * 0.35 + i * Math.PI / 3;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(a) * r, getY() + 1.5 + Math.sin(t * 0.2 + i) * 1.5,
					getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a + 0.5) * r, getY() + 1.5, getZ() + Math.sin(a + 0.5) * r, 2, 0.2, 0.2, 0.2, 0);
		}
		if (t % 25 == 0 && t > 0) {
			rayoVisual(level, getX() + random.nextGaussian() * 6, baseY, getZ() + random.nextGaussian() * 6);
		}
		if (intro == 1) {
			sonido(level, SoundEvents.WARDEN_ROAR, 4f, 0.5f);
			sonido(level, SoundEvents.WITHER_SPAWN, 2f, 1.2f);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 2, 1, 1, 1, 0);
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 150, 0.5, 0.5, 0.5, 0.6);
		}
		entityData.set(DATA_INTRO, intro - 1);
	}

	private void mover(Player target) {
		Vec3 destino;
		if (target == null) {
			destino = new Vec3(getX(), baseY + 6, getZ());
		} else {
			double a = tickCount * 0.02;
			destino = target.position().add(Math.cos(a) * 4, 1 + Math.sin(tickCount * 0.05) * 0.5, Math.sin(a) * 4);
		}
		Vec3 d = destino.subtract(position());
		double vel = fase() == 2 ? 0.07 : 0.05;
		setDeltaMovement(getDeltaMovement().scale(0.8).add(d.normalize().scale(Math.min(d.length(), 1.0) * vel)));
		move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
	}

	private void lookAtTarget(Player target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	private void disparar(ServerLevel level, Player target) {
		Vec3 from = position().add(0, 1.5, 0);
		Vec3 to = target.getEyePosition();
		Vec3 dir = to.subtract(from);
		int pasos = (int) (dir.length() * 2);
		for (int i = 0; i <= pasos; i++) {
			Vec3 p = from.add(dir.scale(i / (double) pasos));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2f, 1.6f);
		if (hasLineOfSight(target)) {
			target.hurtServer(level, level.damageSources().indirectMagic(this, this), dano(target, fase() == 2 ? 7f : 5f));
		}
	}

	private void impactoManos(ServerLevel level, Vec3 pos) {
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, 0.6f);
		level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 3, 1, 0.2, 1, 0);
		for (int i = 0; i < 32; i++) {
			double a = i * Math.PI / 16;
			level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x + Math.cos(a) * 4, pos.y + 0.3, pos.z + Math.sin(a) * 4, 1, 0, 0, 0, 0);
		}
		for (Player p : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(5, 3, 5),
				EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
			p.hurtServer(level, level.damageSources().mobAttack(this), dano(p, fase() == 2 ? 10f : 7f));
			if (!ModContent.armaduraCompleta(p)) p.knockback(1.2, pos.x - p.getX(), pos.z - p.getZ(), level.damageSources().mobAttack(this), 0f);
		}
	}

	private void tickLluvia(ServerLevel level) {
		long now = level.getGameTime();
		impactos.removeIf(imp -> {
			double x = imp[0] / 100.0, y = imp[1] / 100.0, z = imp[2] / 100.0;
			if (now < imp[3]) {
				// aviso: círculo de partículas donde va a caer el cristal
				if (now % 4 == 0) {
					for (int i = 0; i < 10; i++) {
						double a = i * Math.PI / 5;
						level.sendParticles(ParticleTypes.WITCH, x + Math.cos(a) * 2, y + 0.2, z + Math.sin(a) * 2, 1, 0, 0, 0, 0);
					}
				}
				return false;
			}
			rayoVisual(level, x, y, z);
			level.sendParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 40, 0.3, 0.3, 0.3, 0.3);
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 3f, 0.5f);
			for (Player p : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(x, y, z, x, y, z).inflate(2.5, 3, 2.5),
					EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
				p.hurtServer(level, level.damageSources().indirectMagic(this, this), dano(p, 5f));
			}
			return true;
		});
	}

	private void entrarFase2(ServerLevel level) {
		entityData.set(DATA_FASE, 2);
		anunciar(level, Component.translatable("entity.generaciones.eclipse.fase2").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.translatable("entity.generaciones.eclipse.fase2.sub").withStyle(ChatFormatting.LIGHT_PURPLE));
		sonido(level, SoundEvents.WARDEN_ROAR, 4f, 0.7f);
		sonido(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 4f, 0.5f);
		level.getServer().setWeatherParameters(0, 2400, true, true);
		barra.setColor(BossEvent.BossBarColor.RED);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
	}

	// ---------------------------------------------------------------- utilidades

	/** La armadura estelar completa reduce a la mitad todo el daño del Eclipse. */
	private static float dano(Player p, float base) {
		return ModContent.armaduraCompleta(p) ? base * 0.5f : base;
	}

	private void anunciar(ServerLevel level, Component titulo, Component sub) {
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(this) < 80 * 80) {
				p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
				p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
				p.connection.send(new ClientboundSetTitleTextPacket(titulo));
			}
		}
	}

	private void sonido(ServerLevel level, SoundEvent s, float vol, float pitch) {
		level.playSound(null, getX(), getY(), getZ(), s, SoundSource.HOSTILE, vol, pitch);
	}

	private static void rayoVisual(ServerLevel level, double x, double y, double z) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(x, y, z, 0, 0);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
	}

	// ---------------------------------------------------------------- reglas de jefe

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (intro() > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		// cansado recibe un 50 % más de daño
		return super.hurtServer(level, source, cansado > 0 ? damage * 1.25f : damage);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		BlockPos pos = blockPosition();
		Block.popResource(level, pos, new ItemStack(ModContent.NUCLEO_7));
		Block.popResource(level, pos, new ItemStack(ModContent.ESTRELLA_CHISPAS, 3));
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			barra.removeAllPlayers();
			sonido(level, SoundEvents.WITHER_DEATH, 3f, 0.8f);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 3, 1.5, 1.5, 1.5, 0);
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 200, 1, 1, 1, 0.8);
			anunciar(level, Component.translatable("entity.generaciones.eclipse.vencido").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.translatable("entity.generaciones.eclipse.vencido.sub").withStyle(ChatFormatting.YELLOW));
			for (ServerPlayer p : level.players()) {
				if (p.distanceToSqr(this) < 96 * 96) {
					p.sendSystemMessage(Component.translatable("msg.generaciones.guardian.vencido").withStyle(ChatFormatting.GOLD));
				}
			}
			level.getServer().setWeatherParameters(6000, 0, false, false);
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		barra.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		barra.removePlayer(player);
	}

	@Override
	public void checkDespawn() {
		if (level().getDifficulty() == Difficulty.PEACEFUL) {
			discard();
		}
	}

	@Override
	public boolean removeWhenFarAway(double dist) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float mult, DamageSource source) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("intro", intro());
		out.putInt("fase", fase());
		out.putDouble("base_y", Double.isNaN(baseY) ? getY() : baseY);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		entityData.set(DATA_INTRO, in.getIntOr("intro", 0));
		entityData.set(DATA_FASE, in.getIntOr("fase", 1));
		baseY = in.getDoubleOr("base_y", getY());
		if (fase() == 2) {
			barra.setColor(BossEvent.BossBarColor.RED);
		}
	}
}
