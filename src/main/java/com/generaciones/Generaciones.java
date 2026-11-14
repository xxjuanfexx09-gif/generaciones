package com.generaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class Generaciones implements ModInitializer {
	public static final String MOD_ID = "generaciones";
	public static final String MANUAL_TAG = "generaciones_manual_entregado";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModContent.init();
		ToolEvents.register();
		com.generaciones.boss.Guardian.register();
		// La primera vez que un jugador entra al mundo recibe el manual.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player.entityTags().contains(MANUAL_TAG)) {
				return;
			}
			player.addTag(MANUAL_TAG);
			ItemStack book = ManualBook.create();
			player.getInventory().add(book);
			if (!book.isEmpty()) {
				Block.popResource(player.level(), player.blockPosition(), book);
			}
			player.sendSystemMessage(Component.translatable("msg.generaciones.manual").withStyle(ChatFormatting.LIGHT_PURPLE));
		});
		LOG.info("Generaciones listo: generadores Gen I a Gen {}", Balance.roman(Balance.MAX_GEN));
	}
}
