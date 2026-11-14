package com.generaciones.machine;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.generaciones.block.GeneradorBlockEntity;

/** Ayudantes compartidos por las máquinas: el cofre de encima y los generadores de al lado. */
public final class Machines {

	/** El cofre (o barril, tolva…) que está encima de la máquina. */
	public static @Nullable Container outputAbove(Level level, BlockPos pos) {
		BlockPos up = pos.above();
		BlockState state = level.getBlockState(up);
		if (state.getBlock() instanceof ChestBlock chest && level.getBlockEntity(up) instanceof ChestBlockEntity) {
			Container big = ChestBlock.getContainer(chest, state, level, up, true);
			if (big != null) {
				return big;
			}
		}
		BlockEntity be = level.getBlockEntity(up);
		return be instanceof Container c ? c : null;
	}

	/** Mete objetos en el cofre; devuelve lo que no cupo. */
	public static ItemStack insert(Container container, ItemStack stack) {
		return HopperBlockEntity.addItem(null, container, stack, Direction.DOWN);
	}

	public static boolean hasFreeSlot(Container container) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			if (container.getItem(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Generadores pegados a la máquina, en cualquiera de los 6 lados. */
	public static void forNeighborGenerators(Level level, BlockPos pos, java.util.function.Consumer<GeneradorBlockEntity> action) {
		for (Direction d : Direction.values()) {
			if (level.getBlockEntity(pos.relative(d)) instanceof GeneradorBlockEntity g) {
				action.accept(g);
			}
		}
	}

	public static void insertAll(Level level, BlockPos pos, Container container, List<ItemStack> stacks) {
		for (ItemStack s : stacks) {
			ItemStack rest = insert(container, s);
			if (!rest.isEmpty()) {
				net.minecraft.world.level.block.Block.popResource(level, pos.above(2), rest);
			}
		}
	}

	private Machines() {
	}
}
