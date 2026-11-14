package com.generaciones.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.generaciones.Balance;
import com.generaciones.ModContent;
import com.generaciones.block.GeneradorBlock;
import com.generaciones.block.GeneradorBlockEntity;

/** Pruebas automáticas que corren dentro de un servidor de Minecraft real. */
public class GeneracionesGameTest {

	private static final BlockPos POS = new BlockPos(1, 1, 1);

	private static long chispasEnInventario(Player p) {
		return p.getInventory().countItem(ModContent.CHISPA)
				+ p.getInventory().countItem(ModContent.PAQUETE_CHISPAS) * Balance.PAQUETE
				+ p.getInventory().countItem(ModContent.CRISTAL_CHISPAS) * Balance.CRISTAL
				+ p.getInventory().countItem(ModContent.ESTRELLA_CHISPAS) * Balance.ESTRELLA;
	}

	/** Gen I produce 6 por minuto: en 30 s (600 ticks) deben salir 3 chispas al recoger. */
	@GameTest(maxTicks = 700)
	public void produceYRecoge(GameTestHelper helper) {
		helper.setBlock(POS, ModContent.GENERADOR);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.runAfterDelay(600, () -> {
			helper.useBlock(POS, player);
			helper.assertValueEqual(3L, chispasEnInventario(player), "chispas tras 30 s en Gen I");
			GeneradorBlockEntity be = helper.getBlockEntity(POS, GeneradorBlockEntity.class);
			helper.assertValueEqual(0L, be.stored(), "el generador queda vacío después de recoger");
			// recoger otra vez enseguida no da nada
			helper.useBlock(POS, player);
			helper.assertValueEqual(3L, chispasEnInventario(player), "no se duplican chispas");
			helper.succeed();
		});
	}

	/** El núcleo correcto sube la generación y se gasta; uno incorrecto no hace nada. */
	@GameTest
	public void subeDeGeneracion(GameTestHelper helper) {
		helper.setBlock(POS, ModContent.GENERADOR);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.NUCLEO_4));
		helper.useBlock(POS, player);
		helper.assertValueEqual(1, helper.getBlockState(POS).getValue(GeneradorBlock.GEN), "núcleo incorrecto no sube");
		helper.assertValueEqual(1, player.getMainHandItem().getCount(), "núcleo incorrecto no se gasta");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.NUCLEO_2, 2));
		helper.useBlock(POS, player);
		helper.assertValueEqual(2, helper.getBlockState(POS).getValue(GeneradorBlock.GEN), "núcleo Gen II sube a Gen II");
		helper.assertValueEqual(1, player.getMainHandItem().getCount(), "se gasta un núcleo");

		helper.useBlock(POS, player);
		helper.assertValueEqual(2, helper.getBlockState(POS).getValue(GeneradorBlock.GEN), "no se puede repetir el mismo núcleo");

		helper.assertTrue(helper.getBlockEntity(POS, GeneradorBlockEntity.class) != null, "el bloque conserva sus datos al subir");
		helper.succeed();
	}

	/** Gen III (54/min) durante 200 ticks guarda 9; al romperlo suelta el generador y las chispas. */
	@GameTest(maxTicks = 300)
	public void romperSueltaTodo(GameTestHelper helper) {
		helper.setBlock(POS, ModContent.GENERADOR.defaultBlockState().setValue(GeneradorBlock.GEN, 3));
		helper.runAfterDelay(200, () -> {
			BlockPos abs = helper.absolutePos(POS);
			helper.getLevel().destroyBlock(abs, true);
			helper.assertItemEntityPresent(ModContent.GENERADOR_ITEM, POS, 2.0);
			helper.assertItemEntityPresent(ModContent.PAQUETE_CHISPAS, POS, 2.0);
			helper.succeed();
		});
	}

	/** Las chispas se entregan en la forma más compacta. */
	@GameTest
	public void empaquetaCompacto(GameTestHelper helper) {
		List<ItemStack> stacks = GeneradorBlockEntity.pack(1000);
		helper.assertValueEqual(4, stacks.size(), "1000 = 1 estrella + 3 cristales + 3 paquetes + 1 chispa");
		helper.assertTrue(stacks.get(0).is(ModContent.ESTRELLA_CHISPAS) && stacks.get(0).getCount() == 1, "1 estrella");
		helper.assertTrue(stacks.get(1).is(ModContent.CRISTAL_CHISPAS) && stacks.get(1).getCount() == 3, "3 cristales");
		helper.assertTrue(stacks.get(2).is(ModContent.PAQUETE_CHISPAS) && stacks.get(2).getCount() == 3, "3 paquetes");
		helper.assertTrue(stacks.get(3).is(ModContent.CHISPA) && stacks.get(3).getCount() == 1, "1 chispa");
		helper.succeed();
	}

	/** Rendimiento: el generador no registra ningún "ticker", así que no hace trabajo cada tick. */
	@GameTest
	public void noUsaTicks(GameTestHelper helper) {
		helper.setBlock(POS, ModContent.GENERADOR);
		var state = helper.getBlockState(POS);
		helper.assertTrue(ModContent.GENERADOR instanceof GeneradorBlock, "es un generador");
		helper.assertTrue(((GeneradorBlock) ModContent.GENERADOR).getTicker(helper.getLevel(), state, ModContent.GENERADOR_BE) == null,
				"el generador no tiene ticker");
		helper.succeed();
	}
}
