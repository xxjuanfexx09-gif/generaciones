package com.generaciones.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.generaciones.Balance;
import com.generaciones.ModContent;
import com.generaciones.block.GeneradorBlockEntity;

/** Colector (Era II): cada 2 s saca las chispas de los generadores pegados y las guarda en el cofre de encima. */
public class ColectorEntity extends MachineEntity {
	private long total = 0;

	public ColectorEntity(BlockPos pos, BlockState state) {
		super(ModContent.COLECTOR_BE, pos, state);
	}

	@Override
	protected int interval() {
		return 40;
	}

	@Override
	protected void work() {
		Container out = Machines.outputAbove(level, worldPosition);
		if (out == null) {
			return;
		}
		Machines.forNeighborGenerators(level, worldPosition, gen -> {
			long n = gen.takeAll();
			if (n <= 0) {
				return;
			}
			long back = 0;
			for (ItemStack stack : GeneradorBlockEntity.pack(n)) {
				ItemStack rest = Machines.insert(out, stack);
				back += Balance.valueOf(rest);
			}
			gen.giveBack(back);
			total += n - back;
			setChanged();
		});
	}

	@Override
	public Component status() {
		if (Machines.outputAbove(level, worldPosition) == null) {
			return Component.translatable("msg.generaciones.colector.sin_cofre").withStyle(ChatFormatting.RED);
		}
		int[] gens = {0};
		Machines.forNeighborGenerators(level, worldPosition, g -> gens[0]++);
		return Component.translatable("msg.generaciones.colector.estado", gens[0], total).withStyle(ChatFormatting.GOLD);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("total", total);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		total = input.getLongOr("total", 0);
	}
}
