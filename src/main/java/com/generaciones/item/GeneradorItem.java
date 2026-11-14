package com.generaciones.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import com.generaciones.Balance;
import com.generaciones.block.GeneradorBlock;

/** El Generador como objeto: muestra su generación y lo que produce. */
public class GeneradorItem extends BlockItem {
	public GeneradorItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static int genOf(ItemStack stack) {
		BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
		if (props != null) {
			Integer gen = props.get(GeneradorBlock.GEN);
			if (gen != null) {
				return gen;
			}
		}
		return 1;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		int gen = genOf(stack);
		out.accept(Component.translatable("tooltip.generaciones.generador.gen", Balance.roman(gen))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		out.accept(Component.translatable("tooltip.generaciones.generador.produce", (long) Balance.perMinute(gen))
				.withStyle(ChatFormatting.GOLD));
		out.accept(Component.translatable("tooltip.generaciones.generador.uso1").withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable("tooltip.generaciones.generador.uso2").withStyle(ChatFormatting.GRAY));
	}
}
