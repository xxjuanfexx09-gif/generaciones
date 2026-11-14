package com.generaciones.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.generaciones.ModContent;
import com.generaciones.block.GeneradorBlock;
import com.generaciones.block.GeneradorBlockEntity;
import com.generaciones.boss.Guardian;

/** Pruebas de las eras: herramientas, máquinas y jefe final. */
public class EraGameTest {

	private static ServerPlayer jugador(GameTestHelper helper, int nivelPico, Vec3 rel, float xRot) {
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "jugador-prueba"), false);
		var level = helper.getLevel();
		ServerPlayer sp = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		var conn = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(conn);
		level.getServer().getPlayerList().placeNewPlayer(conn, sp, cookie);
		Vec3 abs = helper.absoluteVec(rel);
		sp.snapTo(abs.x, abs.y, abs.z, 0f, xRot);
		sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.PICOS[nivelPico - 1]));
		return sp;
	}

	/** Pico nivel 2: el hierro crudo sale ya fundido. */
	@GameTest
	public void picoFunde(GameTestHelper helper) {
		BlockPos ore = new BlockPos(2, 1, 2);
		helper.setBlock(ore, Blocks.IRON_ORE);
		ServerPlayer sp = jugador(helper, 2, new Vec3(2.5, 2, 2.5), 90f);
		sp.gameMode.destroyBlock(helper.absolutePos(ore));
		helper.assertBlockNotPresent(Blocks.IRON_ORE, ore);
		helper.assertItemEntityPresent(Items.IRON_INGOT, ore, 2.0);
		helper.assertItemEntityNotPresent(Items.RAW_IRON, ore, 2.0);
		helper.succeed();
	}

	/** Pico nivel 3: rompe un cuadrado de 3×3 de una vez. */
	@GameTest
	public void pico3x3(GameTestHelper helper) {
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
			}
		}
		ServerPlayer sp = jugador(helper, 3, new Vec3(2.5, 3, 2.5), 90f);
		sp.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 2, 2)));
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.assertBlockNotPresent(Blocks.STONE, new BlockPos(x, 2, z));
			}
		}
		helper.assertItemEntityPresent(Items.COBBLESTONE, new BlockPos(2, 2, 2), 3.0);
		helper.succeed();
	}

	/** Colector: pasa las chispas del generador pegado al cofre de encima. */
	@GameTest(maxTicks = 120)
	public void colectorJunta(GameTestHelper helper) {
		BlockPos col = new BlockPos(2, 1, 2);
		helper.setBlock(col, ModContent.COLECTOR);
		helper.setBlock(col.above(), Blocks.CHEST);
		helper.setBlock(col.east(), ModContent.GENERADOR);
		helper.getBlockEntity(col.east(), GeneradorBlockEntity.class).giveBack(20);
		helper.runAfterDelay(60, () -> {
			helper.assertContainerContains(col.above(), ModContent.PAQUETE_CHISPAS);
			helper.assertTrue(helper.getBlockEntity(col.east(), GeneradorBlockEntity.class).stored() < 20, "el generador se vació");
			helper.succeed();
		});
	}

	/** Minero: excava gastando chispas y guarda lo minado en el cofre. */
	@GameTest(maxTicks = 200)
	public void mineroExcava(GameTestHelper helper) {
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		BlockPos m = new BlockPos(2, 2, 2);
		helper.setBlock(m, ModContent.MINERO);
		helper.setBlock(m.above(), Blocks.CHEST);
		helper.setBlock(m.east(), ModContent.GENERADOR);
		helper.getBlockEntity(m.east(), GeneradorBlockEntity.class).giveBack(100);
		helper.runAfterDelay(120, () -> {
			helper.assertContainerContains(m.above(), Items.COBBLESTONE);
			long left = helper.getBlockEntity(m.east(), GeneradorBlockEntity.class).stored();
			helper.assertTrue(left < 100, "gastó chispas (quedan " + left + ")");
			helper.setBlock(m, Blocks.AIR); // detenerlo para no cavar el mundo de pruebas
			helper.succeed();
		});
	}

	/** Granja: cosecha el trigo maduro, lo resiembra y guarda la cosecha. */
	@GameTest(maxTicks = 200)
	public void granjaCosecha(GameTestHelper helper) {
		BlockPos g = new BlockPos(1, 2, 1);
		helper.setBlock(g, ModContent.GRANJA);
		helper.setBlock(g.above(), Blocks.CHEST);
		BlockPos crop = new BlockPos(3, 2, 3);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		CropBlock wheat = (CropBlock) Blocks.WHEAT;
		helper.setBlock(crop, wheat.getStateForAge(wheat.getMaxAge()));
		helper.runAfterDelay(120, () -> {
			helper.assertContainerContains(g.above(), Items.WHEAT);
			helper.assertBlockPresent(Blocks.WHEAT, crop);
			helper.assertTrue(!wheat.isMaxAge(helper.getBlockState(crop)), "se volvió a sembrar");
			helper.succeed();
		});
	}

	/** Jefe: invoca a El Eclipse; es invulnerable en su entrada y al morir suelta el Núcleo Estelar. */
	@GameTest(maxTicks = 100)
	public void jefeSueltaNucleo(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		Guardian.summon(helper.getLevel(), helper.absolutePos(pos));
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20);
		var bosses = helper.getLevel().getEntitiesOfClass(com.generaciones.boss.Eclipse.class, area);
		helper.assertTrue(bosses.size() == 1, "apareció El Eclipse");
		var boss = bosses.get(0);
		helper.assertTrue(boss.getMaxHealth() >= 900, "tiene 900 de vida");
		helper.assertTrue(boss.intro() > 0, "empieza con la cinemática de entrada");
		helper.assertTrue(!boss.hurtServer(helper.getLevel(), helper.getLevel().damageSources().magic(), 50f),
				"no recibe daño durante la entrada");
		boss.kill(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			var drops = helper.getLevel().getEntities(EntityTypes.ITEM, area,
					e -> e.getItem().is(ModContent.NUCLEO_7));
			helper.assertTrue(!drops.isEmpty(), "soltó el Núcleo Estelar");
			helper.killAllEntities();
			helper.succeed();
		});
	}

	/** La armadura estelar completa se reconoce como set. */
	@GameTest
	public void armaduraCompleta(GameTestHelper helper) {
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(!ModContent.armaduraCompleta(p), "sin armadura no hay set");
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModContent.CASCO_ESTELAR));
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModContent.PETO_ESTELAR));
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModContent.GREBAS_ESTELARES));
		helper.assertTrue(!ModContent.armaduraCompleta(p), "3 piezas no son el set");
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModContent.BOTAS_ESTELARES));
		helper.assertTrue(ModContent.armaduraCompleta(p), "4 piezas = set completo");
		helper.succeed();
	}

	/** El Núcleo Estelar sube un generador Gen VI a Gen VII. */
	@GameTest
	public void genSiete(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModContent.GENERADOR.defaultBlockState().setValue(GeneradorBlock.GEN, 6));
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.NUCLEO_7));
		helper.useBlock(pos, p);
		helper.assertValueEqual(helper.getBlockState(pos).getValue(GeneradorBlock.GEN), 7, "Gen VII");
		helper.succeed();
	}
}
