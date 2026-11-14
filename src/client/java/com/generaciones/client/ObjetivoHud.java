package com.generaciones.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import com.generaciones.Generaciones;

/**
 * Recuadro arriba a la izquierda con el objetivo actual.
 * Lee el progreso de los logros de la guía: el primer paso sin completar es el objetivo.
 * Solo recalcula 4 veces por segundo, así que no cuesta FPS.
 */
public class ObjetivoHud implements HudElement {

	/** Pasos en orden: nombre del logro, objeto que se muestra, era. */
	private record Paso(String logro, String icono, String era) {
	}

	private static final List<Paso> PASOS = List.of(
			new Paso("generador", "generador", "I"),
			new Paso("chispas", "chispa", "I"),
			new Paso("varios", "generador", "I"),
			new Paso("nucleo_2", "nucleo_2", "I"),
			new Paso("pico_1", "pico_chispa_1", "II"),
			new Paso("colector", "colector", "II"),
			new Paso("cristal", "cristal_chispas", "II"),
			new Paso("nucleo_3", "nucleo_3", "II"),
			new Paso("pico_2", "pico_chispa_2", "III"),
			new Paso("nucleo_4", "nucleo_4", "III"),
			new Paso("pico_3", "pico_chispa_3", "IV"),
			new Paso("minero", "minero", "IV"),
			new Paso("estrella", "estrella_chispas", "IV"),
			new Paso("nucleo_5", "nucleo_5", "IV"),
			new Paso("granja", "granja", "V"),
			new Paso("armadura", "peto_estelar", "V"),
			new Paso("nucleo_6", "nucleo_6", "V"),
			new Paso("pico_5", "pico_chispa_5", "VI"),
			new Paso("corazon", "corazon_estelar", "VI"),
			new Paso("jefe", "nucleo_7", "VI"),
			new Paso("gen7", "nucleo_7", "VII"));

	private static final int ANCHO = 168;

	private int actual = -1;
	private long ultimoCalculo;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.getConnection() == null || mc.getDebugOverlay().showDebugScreen()
				|| com.generaciones.client.eclipse.Cinematica.activa || mc.gui.screen() != null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - ultimoCalculo > 250) {
			ultimoCalculo = now;
			actual = calcular(mc.getConnection().getAdvancements());
		}
		if (actual < 0) {
			return;
		}
		Font font = mc.font;
		boolean fin = actual >= PASOS.size();
		Paso paso = fin ? PASOS.get(PASOS.size() - 1) : PASOS.get(actual);

		Component titulo = fin
				? Component.translatable("hud.generaciones.completado")
				: Component.translatable("hud.generaciones.titulo", paso.era());
		Component texto = Component.translatable(fin ? "hud.generaciones.fin" : "hud.generaciones.paso." + paso.logro());

		int textoX = 24;
		List<FormattedCharSequence> lineas = font.split(texto, ANCHO - textoX - 6);
		boolean hayReceta = !fin && Recetas.para(paso.logro()) != null;
		Component pista = Component.translatable("hud.generaciones.pista", mc.options.keyInventory.getTranslatedKeyMessage());
		int alto = 6 + font.lineHeight + 3 + lineas.size() * font.lineHeight + 3 + 6 + 4 + (hayReceta ? font.lineHeight + 2 : 0);
		alto = Math.max(alto, 30);
		int x = 4;
		int y = 4;

		marco(g, x, y, ANCHO, alto, fin);

		ItemStack icono = new ItemStack(BuiltInRegistries.ITEM.getValue(Generaciones.id(paso.icono())));
		g.item(icono, x + 5, y + 6);

		int ty = y + 5;
		g.text(font, titulo, x + textoX, ty, 0xFFFFD24A, true);
		ty += font.lineHeight + 3;
		for (FormattedCharSequence l : lineas) {
			g.text(font, l, x + textoX, ty, 0xFFFFFFFF, true);
			ty += font.lineHeight;
		}
		ty += 3;
		// barra de progreso
		int barX = x + textoX;
		int barW = ANCHO - textoX - 6;
		int hecho = (int) (barW * Math.min(actual, PASOS.size()) / (float) PASOS.size());
		g.fill(barX, ty + 3, barX + barW, ty + 6, 0xFF2A1E40);
		g.fill(barX, ty + 3, barX + hecho, ty + 6, 0xFFB06BFF);
		g.fill(barX, ty + 3, barX + hecho, ty + 4, 0xFFE0B8FF);
		if (hayReceta) {
			g.text(font, pista, x + textoX, ty + 10, 0xFF9A90B0, false);
		}

	}

	/** Marco al estilo de los cuadros de información de Minecraft: fondo oscuro y borde morado degradado. */
	private static void marco(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean fin) {
		int fondo = 0xE8100010;
		int bordeArriba = fin ? 0xFFFFD24A : 0xFF9050FF;
		int bordeAbajo = fin ? 0xFFB07A10 : 0xFF3A1880;
		g.fill(x + 1, y, x + w - 1, y + h, fondo);
		g.fill(x, y + 1, x + w, y + h - 1, fondo);
		for (int i = 1; i < h - 1; i++) {
			float t = i / (float) (h - 1);
			int c = mezcla(bordeArriba, bordeAbajo, t);
			g.fill(x + 1, y + i, x + 2, y + i + 1, c);
			g.fill(x + w - 2, y + i, x + w - 1, y + i + 1, c);
		}
		g.fill(x + 1, y + 1, x + w - 1, y + 2, bordeArriba);
		g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, bordeAbajo);
	}

	private static int mezcla(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (gg << 8) | bb;
	}

	/** Paso actual (nombre del logro) o null si no hay datos o ya terminó. */
	public static String pasoActual() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) {
			return null;
		}
		int i = calcular(mc.getConnection().getAdvancements());
		return i < 0 || i >= PASOS.size() ? null : PASOS.get(i).logro();
	}

	/** Índice del primer paso sin completar (PASOS.size() si terminó todo, -1 si aún no hay datos). */
	private static int calcular(ClientAdvancements adv) {
		if (adv.progress().isEmpty()) {
			return -1;
		}
		for (int i = 0; i < PASOS.size(); i++) {
			AdvancementHolder holder = adv.get(Generaciones.id("guia/" + PASOS.get(i).logro()));
			AdvancementProgress p = holder == null ? null : adv.progress().get(holder);
			if (p == null || !p.isDone()) {
				return i;
			}
		}
		return PASOS.size();
	}
}
