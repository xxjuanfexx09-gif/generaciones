package com.generaciones.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Bloque como objeto, con una explicación corta. */
public class TooltipBlockItem extends BlockItem {
	private final String[] lines;

	public TooltipBlockItem(Block block, Properties properties, String... lines) {
		super(block, properties);
		this.lines = lines;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		for (String key : lines) {
			out.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
		}
	}
}
