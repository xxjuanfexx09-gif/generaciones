package com.generaciones.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Objeto con una explicación corta al pasar el mouse por encima. */
public class TooltipItem extends Item {
	private final String[] lines;

	public TooltipItem(Properties properties, String... lineKeys) {
		super(properties);
		this.lines = lineKeys;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		for (String key : lines) {
			out.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
		}
	}
}
