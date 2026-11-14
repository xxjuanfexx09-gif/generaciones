package com.generaciones.client;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

/**
 * Panel con la receta del objetivo actual, al estilo de la interfaz de Minecraft.
 * Solo aparece al lado del inventario o de la mesa de crafteo.
 * Clic izquierdo en un ingrediente: muestra cómo se fabrica ese ingrediente.
 * Clic derecho en el panel (o en "Volver"): regresa a la receta anterior.
 */
public final class RecetaPanel {
	private static final int ANCHO = 112;
	private static Field leftPos;
	private static Field topPos;

	/** Objetos que se están mirando (vacío = la receta del objetivo). */
	private static final Deque<Item> HISTORIAL = new ArrayDeque<>();

	// zona del último dibujo, para saber dónde se hizo clic
	private static int px, py, palto;
	private static Recetas.Receta mostrada;

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (screen instanceof InventoryScreen || screen instanceof CraftingScreen) {
				HISTORIAL.clear();
				mostrada = null;
				ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> dibujar(s, g, mx, my));
				ScreenMouseEvents.allowMouseClick(screen).register((s, ev) -> !clic(ev.x(), ev.y(), ev.button()));
			}
		});
	}

	/** Para las pruebas: abre la receta de un ingrediente como si se hiciera clic. */
	public static void abrir(Item it) {
		HISTORIAL.push(it);
	}

	private static Recetas.Receta actual() {
		if (!HISTORIAL.isEmpty()) {
			return Recetas.deItem(HISTORIAL.peek());
		}
		String paso = ObjetivoHud.pasoActual();
		return paso == null ? null : Recetas.para(paso);
	}

	private static void dibujar(Screen screen, GuiGraphicsExtractor g, int mx, int my) {
		Recetas.Receta r = actual();
		mostrada = null;
		if (r == null || !(screen instanceof AbstractContainerScreen<?> cs)) {
			return;
		}
		int x = pos(cs, true) - ANCHO - 4;
		if (x < 2) {
			return;
		}
		int y = pos(cs, false);
		boolean viendoIngrediente = !HISTORIAL.isEmpty();
		int alto = 80 + (r.sinForma() ? 12 : 0) + 12;
		px = x;
		py = y;
		palto = alto;
		mostrada = r;
		Font font = Minecraft.getInstance().font;

		panel(g, x, y, ANCHO, alto);
		Component titulo = viendoIngrediente
				? r.resultado().getName(Recetas.stack(r.resultado()))
				: Component.translatable("hud.generaciones.receta");
		g.text(font, font.plainSubstrByWidth(titulo.getString(), ANCHO - 14), x + 7, y + 6, 0xFF404040, false);

		int sx0 = x + 7;
		int sy0 = y + 18;
		for (int i = 0; i < 9; i++) {
			int sx = sx0 + (i % 3) * 18;
			int sy = sy0 + (i / 3) * 18;
			ranura(g, sx, sy, 18);
			Item it = r.grid()[i];
			if (it != null) {
				g.item(Recetas.stack(it), sx + 1, sy + 1);
				boolean encima = mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18;
				boolean tieneReceta = Recetas.deItem(it) != null;
				if (tieneReceta) {
					// puntito verde: este ingrediente se puede abrir
					g.fill(sx + 13, sy + 2, sx + 16, sy + 5, 0xFF55FF55);
				}
				if (encima) {
					g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x80FFFFFF);
					g.setTooltipForNextFrame(font, Recetas.stack(it), mx, my);
				}
			}
		}
		int ax = sx0 + 58;
		int ay = sy0 + 23;
		g.fill(ax, ay + 3, ax + 10, ay + 5, 0xFF8B8B8B);
		for (int k = 0; k < 4; k++) {
			g.fill(ax + 10 + k, ay + k, ax + 11 + k, ay + 8 - k, 0xFF8B8B8B);
		}
		int rx = sx0 + 76;
		int ry = sy0 + 14;
		ranura(g, rx, ry, 26);
		g.item(Recetas.stack(r.resultado()), rx + 5, ry + 5);
		if (mx >= rx && mx < rx + 26 && my >= ry && my < ry + 26) {
			g.setTooltipForNextFrame(font, Recetas.stack(r.resultado()), mx, my);
		}

		int ty = sy0 + 56;
		if (r.sinForma()) {
			g.text(font, Component.translatable("hud.generaciones.sin_forma"), x + 7, ty, 0xFF606060, false);
			ty += 12;
		}
		Component pie = viendoIngrediente
				? Component.translatable("hud.generaciones.volver")
				: Component.translatable("hud.generaciones.clic_ingrediente");
		g.text(font, font.plainSubstrByWidth(pie.getString(), ANCHO - 14), x + 7, ty, viendoIngrediente ? 0xFF2050A0 : 0xFF606060, false);
	}

	/** Devuelve true si el clic fue dentro del panel (y no debe llegar al inventario). */
	private static boolean clic(double mx, double my, int boton) {
		if (mostrada == null || mx < px || mx >= px + ANCHO || my < py || my >= py + palto) {
			return false;
		}
		if (boton == 1) {
			atras();
			return true;
		}
		int sx0 = px + 7;
		int sy0 = py + 18;
		for (int i = 0; i < 9; i++) {
			int sx = sx0 + (i % 3) * 18;
			int sy = sy0 + (i / 3) * 18;
			if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
				Item it = mostrada.grid()[i];
				if (it != null && Recetas.deItem(it) != null) {
					HISTORIAL.push(it);
					sonido();
				}
				return true;
			}
		}
		// clic en la línea de abajo ("< Volver")
		if (!HISTORIAL.isEmpty() && my >= py + palto - 16) {
			atras();
		}
		return true;
	}

	private static void atras() {
		if (!HISTORIAL.isEmpty()) {
			HISTORIAL.pop();
			sonido();
		}
	}

	private static void sonido() {
		Minecraft.getInstance().getSoundManager().play(
				net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
		g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
		g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
		g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
		g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
	}

	private static void ranura(GuiGraphicsExtractor g, int x, int y, int s) {
		g.fill(x, y, x + s, y + s, 0xFF8B8B8B);
		g.fill(x, y, x + s - 1, y + 1, 0xFF373737);
		g.fill(x, y, x + 1, y + s - 1, 0xFF373737);
		g.fill(x + 1, y + s - 1, x + s, y + s, 0xFFFFFFFF);
		g.fill(x + s - 1, y + 1, x + s, y + s, 0xFFFFFFFF);
	}

	private static int pos(AbstractContainerScreen<?> cs, boolean izquierda) {
		try {
			if (leftPos == null) {
				leftPos = AbstractContainerScreen.class.getDeclaredField("leftPos");
				topPos = AbstractContainerScreen.class.getDeclaredField("topPos");
				leftPos.setAccessible(true);
				topPos.setAccessible(true);
			}
			return (izquierda ? leftPos : topPos).getInt(cs);
		} catch (ReflectiveOperationException e) {
			return izquierda ? (cs.width - 176) / 2 : (cs.height - 166) / 2;
		}
	}

	private RecetaPanel() {
	}
}
