package com.generaciones.boss;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** Corazón Estelar: clic derecho sobre el suelo para invocar al Guardián de Chispas. */
public class CorazonItem extends Item {
	public CorazonItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		if (!(ctx.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			if (ctx.getPlayer() != null) {
				ctx.getPlayer().sendOverlayMessage(Component.translatable("msg.generaciones.guardian.pacifico").withStyle(ChatFormatting.RED));
			}
			return InteractionResult.FAIL;
		}
		Guardian.summon(level, ctx.getClickedPos());
		ctx.getItemInHand().consume(1, ctx.getPlayer());
		if (ctx.getPlayer() != null) {
			ctx.getPlayer().sendSystemMessage(Component.translatable("msg.generaciones.guardian.invocado").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("tooltip.generaciones.corazon").withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable("tooltip.generaciones.corazon.aviso").withStyle(ChatFormatting.RED));
	}
}
