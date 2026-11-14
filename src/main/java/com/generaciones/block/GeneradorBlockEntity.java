package com.generaciones.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.generaciones.Balance;
import com.generaciones.ModContent;

/**
 * Guarda cuándo se recogió por última vez. La producción se calcula con el tiempo del mundo
 * solo cuando alguien interactúa: cero trabajo por tick, sin importar cuántos generadores haya.
 */
public class GeneradorBlockEntity extends BlockEntity {
	private long lastTime = -1;
	private long stored = 0;
	private double partial = 0;

	public GeneradorBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.GENERADOR_BE, pos, state);
	}

	public int gen() {
		return getBlockState().getValue(GeneradorBlock.GEN);
	}

	public long stored() {
		return stored;
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if (lastTime < 0 && !level.isClientSide()) {
			lastTime = level.getGameTime();
		}
	}

	/** Suma lo producido desde la última vez. Barato: unas pocas operaciones. */
	public void update() {
		if (level == null || level.isClientSide()) {
			return;
		}
		long now = level.getGameTime();
		if (lastTime < 0) {
			lastTime = now;
			return;
		}
		long elapsed = now - lastTime;
		lastTime = now;
		if (elapsed <= 0) {
			return;
		}
		int gen = gen();
		long cap = Balance.capacity(gen);
		if (stored >= cap) {
			partial = 0;
			setChanged();
			return;
		}
		double add = elapsed * Balance.perMinute(gen) / 1200.0 + partial;
		long whole = (long) Math.floor(add);
		partial = add - whole;
		stored = Math.min(cap, stored + whole);
		if (stored >= cap) {
			partial = 0;
		}
		setChanged();
	}

	/** Saca todo lo guardado (lo usa el Colector). */
	public long takeAll() {
		update();
		long n = stored;
		stored = 0;
		if (n > 0) {
			setChanged();
		}
		return n;
	}

	/** Devuelve chispas que no cupieron en el cofre. */
	public void giveBack(long n) {
		if (n > 0) {
			stored += n;
			setChanged();
		}
	}

	/** Gasta chispas como combustible (lo usa el Minero). */
	public boolean consume(long n) {
		update();
		if (stored < n) {
			return false;
		}
		stored -= n;
		setChanged();
		return true;
	}

	public void collect(Player player) {
		update();
		int gen = gen();
		long cap = Balance.capacity(gen);
		if (stored <= 0) {
			double perTick = Balance.perMinute(gen) / 1200.0;
			long ticks = (long) Math.ceil((1.0 - partial) / perTick);
			long seconds = Math.max(1, (ticks + 19) / 20);
			player.sendOverlayMessage(Component.translatable("msg.generaciones.vacio",
					Balance.roman(gen), (long) Balance.perMinute(gen), seconds).withStyle(ChatFormatting.GRAY));
			return;
		}
		long amount = stored;
		stored = 0;
		setChanged();
		for (ItemStack stack : pack(amount)) {
			player.getInventory().add(stack);
			if (!stack.isEmpty()) {
				Block.popResource(level, player.blockPosition(), stack);
			}
		}
		level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.4f);
		player.sendOverlayMessage(Component.translatable("msg.generaciones.recogiste",
				amount, Balance.roman(gen), (long) Balance.perMinute(gen), cap).withStyle(ChatFormatting.GOLD));
	}

	/** Convierte una cantidad de chispas en la menor cantidad de objetos posible. */
	public static List<ItemStack> pack(long amount) {
		List<ItemStack> out = new ArrayList<>();
		long[] values = {Balance.ESTRELLA, Balance.CRISTAL, Balance.PAQUETE, 1};
		Item[] items = {ModContent.ESTRELLA_CHISPAS, ModContent.CRISTAL_CHISPAS, ModContent.PAQUETE_CHISPAS, ModContent.CHISPA};
		for (int i = 0; i < values.length; i++) {
			long count = amount / values[i];
			amount -= count * values[i];
			while (count > 0) {
				int n = (int) Math.min(64, count);
				out.add(new ItemStack(items[i], n));
				count -= n;
			}
		}
		return out;
	}

	/** Si rompen el generador, suelta lo que tenía guardado. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		update();
		if (level != null && stored > 0) {
			for (ItemStack stack : pack(stored)) {
				Block.popResource(level, pos, stack);
			}
			stored = 0;
		}
		super.preRemoveSideEffects(pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("last_time", lastTime);
		output.putLong("stored", stored);
		output.putDouble("partial", partial);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lastTime = input.getLongOr("last_time", -1);
		stored = input.getLongOr("stored", 0);
		partial = input.getDoubleOr("partial", 0);
	}
}
