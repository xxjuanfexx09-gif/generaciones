package com.generaciones.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base de las máquinas. Trabajan cada {@link #interval()} ticks, repartidas en el tiempo
 * según su posición para que no trabajen todas en el mismo tick.
 */
public abstract class MachineEntity extends BlockEntity {
	protected MachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	protected abstract int interval();

	protected abstract void work();

	public abstract Component status();

	final void serverTick() {
		if (level == null) {
			return;
		}
		long offset = Math.floorMod(worldPosition.asLong(), interval());
		if ((level.getGameTime() + offset) % interval() == 0) {
			work();
		}
	}
}
