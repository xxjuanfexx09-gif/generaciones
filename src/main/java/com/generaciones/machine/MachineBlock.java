package com.generaciones.machine;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bloque de máquina genérico: crea su BlockEntity, la hace trabajar en el servidor y muestra su estado con clic derecho. */
public class MachineBlock extends BaseEntityBlock {
	private final BiFunction<BlockPos, BlockState, MachineEntity> factory;
	private final Supplier<BlockEntityType<? extends MachineEntity>> type;

	public MachineBlock(Properties properties, BiFunction<BlockPos, BlockState, MachineEntity> factory,
			Supplier<BlockEntityType<? extends MachineEntity>> type) {
		super(properties);
		this.factory = factory;
		this.type = type;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return factory.apply(pos, state);
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		if (level.isClientSide() || blockEntityType != type.get()) {
			return null;
		}
		return (lvl, pos, st, be) -> ((MachineEntity) be).serverTick();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineEntity m) {
			player.sendOverlayMessage(m.status());
		}
		return InteractionResult.SUCCESS;
	}
}
