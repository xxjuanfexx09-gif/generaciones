package com.generaciones.client.eclipse;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import com.generaciones.boss.Eclipse;

/** Barras negras de cine mientras el Eclipse está apareciendo. */
public class Cinematica implements HudElement {
	public static boolean activa;
	private static float barras; // 0..1

	/** Se llama cada tick del cliente: solo busca al jefe cerca (barato). */
	public static void tick(Minecraft mc) {
		boolean hay = false;
		if (mc.level != null && mc.player != null) {
			for (Eclipse e : mc.level.getEntitiesOfClass(Eclipse.class, mc.player.getBoundingBox().inflate(80))) {
				if (e.intro() > 0) {
					hay = true;
					break;
				}
			}
		}
		activa = hay;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		float objetivo = activa ? 1f : 0f;
		barras += (objetivo - barras) * 0.08f;
		if (barras < 0.01f) {
			return;
		}
		int alto = (int) (g.guiHeight() * 0.13f * barras);
		g.fill(0, 0, g.guiWidth(), alto, 0xFF000000);
		g.fill(0, g.guiHeight() - alto, g.guiWidth(), g.guiHeight(), 0xFF000000);
	}
}
