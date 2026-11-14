package com.generaciones.machine;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.generaciones.ModContent;

/**
 * Minero automático (Era IV): excava un área de 5×5 hacia abajo, un bloque cada medio segundo.
 * Gasta 1 chispa por bloque de un generador pegado y deja todo en el cofre de encima.
 * No toca cofres, líquidos ni bloques irrompibles.
 */
public class MineroEntity extends MachineEntity {
	private static final int RADIO = 2;
	private static final ItemStack HERRAMIENTA = new ItemStack(Items.NETHERITE_PICKAXE);

	private int dx = -RADIO;
	private int dz = -RADIO;
	private int y = Integer.MIN_VALUE;
	private long minados = 0;
	private boolean terminado = false;
	private String falta = "";

	public MineroEntity(BlockPos pos, BlockState state) {
		super(ModContent.MINERO_BE, pos, state);
	}

	@Override
	protected int interval() {
		return 10;
	}

	@Override
	protected void work() {
		if (terminado || !(level instanceof ServerLevel server)) {
			return;
		}
		if (y == Integer.MIN_VALUE) {
			y = worldPosition.getY() - 1;
		}
		Container out = Machines.outputAbove(server, worldPosition);
		if (out == null) {
			falta = "cofre";
			return;
		}
		if (!Machines.hasFreeSlot(out)) {
			falta = "lleno";
			return;
		}
		// busca el siguiente bloque que valga la pena (máximo 30 revisiones por turno)
		for (int intentos = 0; intentos < 30; intentos++) {
			if (y <= server.getMinY()) {
				terminado = true;
				setChanged();
				return;
			}
			BlockPos p = new BlockPos(worldPosition.getX() + dx, y, worldPosition.getZ() + dz);
			BlockState st = server.getBlockState(p);
			boolean minable = !st.isAir() && st.getDestroySpeed(server, p) >= 0 && st.getFluidState().isEmpty()
					&& server.getBlockEntity(p) == null && !p.equals(worldPosition);
			if (!minable) {
				advance();
				continue;
			}
			boolean[] pagado = {false};
			Machines.forNeighborGenerators(server, worldPosition, g -> {
				if (!pagado[0] && g.consume(1)) {
					pagado[0] = true;
				}
			});
			if (!pagado[0]) {
				falta = "chispas";
				return;
			}
			falta = "";
			List<ItemStack> drops = Block.getDrops(st, server, p, null, null, HERRAMIENTA);
			server.levelEvent(2001, p, Block.getId(st));
			server.removeBlock(p, false);
			Machines.insertAll(server, worldPosition, out, drops);
			minados++;
			advance();
			setChanged();
			return;
		}
	}

	private void advance() {
		dx++;
		if (dx > RADIO) {
			dx = -RADIO;
			dz++;
			if (dz > RADIO) {
				dz = -RADIO;
				y--;
			}
		}
	}

	@Override
	public Component status() {
		if (terminado) {
			return Component.translatable("msg.generaciones.minero.terminado", minados).withStyle(ChatFormatting.GOLD);
		}
		return switch (falta) {
			case "cofre" -> Component.translatable("msg.generaciones.minero.sin_cofre").withStyle(ChatFormatting.RED);
			case "lleno" -> Component.translatable("msg.generaciones.minero.lleno").withStyle(ChatFormatting.RED);
			case "chispas" -> Component.translatable("msg.generaciones.minero.sin_chispas").withStyle(ChatFormatting.RED);
			default -> Component.translatable("msg.generaciones.minero.estado", y == Integer.MIN_VALUE ? worldPosition.getY() - 1 : y, minados)
					.withStyle(ChatFormatting.GOLD);
		};
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("dx", dx);
		output.putInt("dz", dz);
		output.putInt("y", y);
		output.putLong("minados", minados);
		output.putInt("terminado", terminado ? 1 : 0);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		dx = input.getIntOr("dx", -RADIO);
		dz = input.getIntOr("dz", -RADIO);
		y = input.getIntOr("y", Integer.MIN_VALUE);
		minados = input.getLongOr("minados", 0);
		terminado = input.getIntOr("terminado", 0) == 1;
	}
}
