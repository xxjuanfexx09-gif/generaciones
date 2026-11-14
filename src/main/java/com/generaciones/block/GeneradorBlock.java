package com.generaciones.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.generaciones.Balance;
import com.generaciones.item.NucleoItem;

/**
 * El Generador. Su generación (1–6) es parte del estado del bloque, así cambia de textura y de luz.
 * No tiene "ticker": la producción se calcula solo cuando lo usas, así que no gasta rendimiento.
 */
public class GeneradorBlock extends BaseEntityBlock {
	public static final IntegerProperty GEN = IntegerProperty.create("gen", 1, Balance.MAX_GEN);

	public GeneradorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(GEN, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(GEN);
	}

	public static int light(BlockState state) {
		return Math.min(15, 3 + state.getValue(GEN) * 2);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GeneradorBlockEntity(pos, state);
	}

	/** Clic derecho con la mano vacía (o con cualquier cosa que no sea un núcleo): recoger. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof GeneradorBlockEntity generador) {
			generador.collect(player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Clic derecho con un núcleo: subir de generación. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof NucleoItem nucleo)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		int gen = state.getValue(GEN);
		if (nucleo.targetGen != gen + 1) {
			player.sendOverlayMessage(gen >= Balance.MAX_GEN
					? Component.translatable("msg.generaciones.max").withStyle(ChatFormatting.GOLD)
					: Component.translatable("msg.generaciones.nucleo_incorrecto",
							Balance.roman(gen), Balance.roman(gen + 1)).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		if (level.getBlockEntity(pos) instanceof GeneradorBlockEntity generador) {
			generador.update(); // guarda lo producido con la velocidad vieja antes de subir
		}
		level.setBlock(pos, state.setValue(GEN, gen + 1), Block.UPDATE_ALL);
		stack.consume(1, player);
		level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 1.2f);
		if (level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					24, 0.4, 0.4, 0.4, 0.05);
		}
		player.sendOverlayMessage(Component.translatable("msg.generaciones.subio",
				Balance.roman(gen + 1), (long) Balance.perMinute(gen + 1)).withStyle(ChatFormatting.LIGHT_PURPLE));
		return InteractionResult.SUCCESS;
	}

	/** Chispas decorativas: solo en el cliente, de vez en cuando y solo para bloques cercanos. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(5) != 0) {
			return;
		}
		double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
		double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
		level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.02, z, 0, 0.02, 0);
	}

	/** Al colocar un generador: cuenta los que hay cerca y enseña que tener varios es la clave. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity by, ItemStack stack) {
		super.setPlacedBy(level, pos, state, by, stack);
		if (!(level instanceof ServerLevel server) || !(by instanceof net.minecraft.server.level.ServerPlayer sp)) {
			return;
		}
		int total = 0;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-12, -6, -12), pos.offset(12, 6, 12))) {
			if (server.getBlockState(p).is(this)) {
				total++;
			}
		}
		sp.sendOverlayMessage(Component.translatable(total >= 4 ? "msg.generaciones.varios_listo" : "msg.generaciones.varios", total)
				.withStyle(total >= 4 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
		if (total >= 4) {
			var holder = server.getServer().getAdvancements().get(com.generaciones.Generaciones.id("guia/varios"));
			if (holder != null) {
				sp.getAdvancements().award(holder, "hecho");
			}
		}
	}
}
