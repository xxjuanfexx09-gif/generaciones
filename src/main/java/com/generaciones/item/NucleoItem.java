package com.generaciones.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.generaciones.Balance;

/** Núcleo que sube un Generador a la generación indicada. */
public class NucleoItem extends Item {
	public final int targetGen;

	public NucleoItem(Properties properties, int targetGen) {
		super(properties);
		this.targetGen = targetGen;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("tooltip.generaciones.nucleo.uso",
				Balance.roman(targetGen - 1), Balance.roman(targetGen)).withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable("tooltip.generaciones.nucleo.produce",
				(long) Balance.perMinute(targetGen)).withStyle(ChatFormatting.GOLD));
	}
}
