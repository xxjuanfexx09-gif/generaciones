package com.generaciones;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

/**
 * Poderes del Pico de chispa:
 * nivel 2+ funde los minerales crudos al picarlos, nivel 3+ pica en 3×3.
 * Solo trabaja cuando picas un bloque con el pico: no cuesta nada el resto del tiempo.
 */
public final class ToolEvents {

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp) || sp.isCreative()) {
				return true;
			}
			ItemStack tool = sp.getMainHandItem();
			int nivel = ModContent.picoLevel(tool);
			if (nivel < 2 || !tool.isCorrectToolForDrops(state)) {
				return true; // el juego normal se encarga
			}
			List<BlockPos> targets = new ArrayList<>();
			targets.add(pos);
			if (nivel >= 3 && !sp.isShiftKeyDown()) {
				targets.addAll(plane(pos, facing(sp)));
			}
			for (BlockPos p : targets) {
				BlockState st = server.getBlockState(p);
				if (st.isAir() || st.getDestroySpeed(server, p) < 0) {
					continue;
				}
				if (!p.equals(pos)) {
					// en el 3×3 solo rompe lo que el pico puede romper bien, y nunca cofres u otros bloques con datos
					if (!tool.isCorrectToolForDrops(st) || server.getBlockEntity(p) != null || !st.getFluidState().isEmpty()) {
						continue;
					}
				}
				BlockEntity be = server.getBlockEntity(p);
				List<ItemStack> drops = Block.getDrops(st, server, p, be, sp, tool);
				st.spawnAfterBreak(server, p, tool, true);
				server.levelEvent(2001, p, Block.getId(st));
				server.removeBlock(p, false);
				for (ItemStack drop : drops) {
					Block.popResource(server, p, smelt(server, drop));
				}
				if (!tool.isEmpty()) {
					tool.hurtAndBreak(1, server, sp, item -> { });
				}
			}
			return false; // ya lo rompimos nosotros
		});
	}

	/** Funde solo minerales crudos (raw_*) y escombros ancestrales, para no convertir piedra en otra cosa. */
	static ItemStack smelt(ServerLevel level, ItemStack drop) {
		String path = BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath();
		if (!path.startsWith("raw_") && !path.equals("ancient_debris")) {
			return drop;
		}
		SingleRecipeInput input = new SingleRecipeInput(drop.copyWithCount(1));
		Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
		if (recipe.isEmpty()) {
			return drop;
		}
		ItemStack out = recipe.get().value().assemble(input);
		return out.copyWithCount(out.getCount() * drop.getCount());
	}

	private static Direction facing(ServerPlayer sp) {
		HitResult hit = sp.pick(sp.blockInteractionRange(), 0, false);
		if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
			return bhr.getDirection();
		}
		return sp.getXRot() > 45 || sp.getXRot() < -45 ? Direction.UP : sp.getDirection().getOpposite();
	}

	/** Los 8 bloques alrededor en el plano perpendicular a la cara picada. */
	private static List<BlockPos> plane(BlockPos center, Direction face) {
		List<BlockPos> out = new ArrayList<>();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a == 0 && b == 0) {
					continue;
				}
				switch (face.getAxis()) {
					case Y -> out.add(center.offset(a, 0, b));
					case X -> out.add(center.offset(0, a, b));
					case Z -> out.add(center.offset(a, b, 0));
				}
			}
		}
		return out;
	}

	private ToolEvents() {
	}
}
