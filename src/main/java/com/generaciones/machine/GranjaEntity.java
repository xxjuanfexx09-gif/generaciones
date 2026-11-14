package com.generaciones.machine;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.generaciones.ModContent;

/**
 * Granja automática (Era V): cada 5 s cosecha los cultivos maduros en 9×9 a su alrededor,
 * los vuelve a sembrar y deja la cosecha en el cofre de encima.
 */
public class GranjaEntity extends MachineEntity {
	private static final int RADIO = 4;
	private long cosechados = 0;

	public GranjaEntity(BlockPos pos, BlockState state) {
		super(ModContent.GRANJA_BE, pos, state);
	}

	@Override
	protected int interval() {
		return 100;
	}

	@Override
	protected void work() {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		Container out = Machines.outputAbove(server, worldPosition);
		if (out == null) {
			return;
		}
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -RADIO; dx <= RADIO; dx++) {
				for (int dz = -RADIO; dz <= RADIO; dz++) {
					BlockPos p = worldPosition.offset(dx, dy, dz);
					BlockState st = server.getBlockState(p);
					if (!(st.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(st)) {
						continue;
					}
					List<ItemStack> drops = Block.getDrops(st, server, p, null);
					Item semilla = crop.asItem();
					for (ItemStack d : drops) {
						if (d.is(semilla)) {
							d.shrink(1); // una semilla vuelve a la tierra
							break;
						}
					}
					server.setBlock(p, crop.getStateForAge(0), Block.UPDATE_ALL);
					server.levelEvent(2001, p, Block.getId(st));
					Machines.insertAll(server, worldPosition, out, drops);
					cosechados++;
				}
			}
		}
		setChanged();
	}

	@Override
	public Component status() {
		if (Machines.outputAbove(level, worldPosition) == null) {
			return Component.translatable("msg.generaciones.granja.sin_cofre").withStyle(ChatFormatting.RED);
		}
		return Component.translatable("msg.generaciones.granja.estado", cosechados).withStyle(ChatFormatting.GOLD);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("cosechados", cosechados);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		cosechados = input.getLongOr("cosechados", 0);
	}
}
