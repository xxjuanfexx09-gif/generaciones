package com.generaciones.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Recetas que se muestran al lado del objetivo (por si no hay JEI/EMI instalado).
 * Copia de las recetas de data/generaciones/recipe, en forma de cuadrícula 3×3.
 */
public final class Recetas {
	/** Una receta: 9 casillas (null = vacía) y el resultado. */
	public record Receta(Item[] grid, Item resultado, boolean sinForma) {
	}

	private static final Map<String, Receta> POR_PASO = new HashMap<>();
	private static final Map<Item, Receta> POR_ITEM = new HashMap<>();

	private static Item g(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("generaciones", path));
	}

	private static Receta forma(String[] filas, Map<Character, Item> llave, Item res) {
		Item[] grid = new Item[9];
		for (int r = 0; r < filas.length; r++) {
			for (int c = 0; c < filas[r].length(); c++) {
				grid[r * 3 + c] = llave.get(filas[r].charAt(c));
			}
		}
		return new Receta(grid, res, false);
	}

	private static Receta sinForma(Item res, Item... items) {
		Item[] grid = new Item[9];
		System.arraycopy(items, 0, grid, 0, items.length);
		return new Receta(grid, res, true);
	}

	public static Receta para(String paso) {
		if (POR_PASO.isEmpty()) {
			cargar();
		}
		return POR_PASO.get(paso);
	}

	private static void cargar() {
		Item P = g("paquete_chispas"), K = g("cristal_chispas"), E = g("estrella_chispas");
		Item RB = Items.REDSTONE_BLOCK;
		POR_PASO.put("generador", forma(new String[] {"SSS", "SRS", "SSS"},
				Map.of('S', Items.COBBLESTONE, 'R', Items.REDSTONE), g("generador")));
		POR_PASO.put("varios", forma(new String[] {"SSS", "SRS", "SSS"},
				Map.of('S', Items.COBBLESTONE, 'R', Items.REDSTONE), g("generador")));
		POR_PASO.put("nucleo_2", forma(new String[] {"PCP", "CRC", "PCP"},
				Map.of('P', P, 'C', Items.COPPER_INGOT, 'R', RB), g("nucleo_2")));
		POR_PASO.put("pico_1", forma(new String[] {"PNP", " S ", " S "},
				Map.of('P', P, 'N', g("nucleo_2"), 'S', Items.STICK), g("pico_chispa_1")));
		POR_PASO.put("colector", forma(new String[] {"PHP", "SNS", "SSS"},
				Map.of('P', P, 'H', Items.HOPPER, 'S', Items.COBBLESTONE, 'N', g("nucleo_2")), g("colector")));
		POR_PASO.put("cristal", forma(new String[] {"PPP", "PPP", "PPP"}, Map.of('P', P), K));
		POR_PASO.put("nucleo_3", forma(new String[] {"KIP", "INI", "PIP"},
				Map.of('K', K, 'P', P, 'I', Items.IRON_INGOT, 'N', RB), g("nucleo_3")));
		POR_PASO.put("pico_2", sinForma(g("pico_chispa_2"), g("pico_chispa_1"), g("nucleo_3"), K));
		POR_PASO.put("nucleo_4", forma(new String[] {"KGK", "GNG", "KGK"},
				Map.of('K', K, 'G', Items.GOLD_INGOT, 'N', RB), g("nucleo_4")));
		POR_PASO.put("pico_3", sinForma(g("pico_chispa_3"), g("pico_chispa_2"), g("nucleo_4"), K, K));
		POR_PASO.put("minero", forma(new String[] {"BKB", "INI", "BKB"},
				Map.of('B', Items.IRON_BLOCK, 'K', K, 'I', Items.IRON_PICKAXE, 'N', g("nucleo_4")), g("minero")));
		POR_PASO.put("estrella", forma(new String[] {"KKK", "KKK", "KKK"}, Map.of('K', K), E));
		POR_PASO.put("nucleo_5", forma(new String[] {"EDK", "DND", "KDK"},
				Map.of('E', E, 'K', K, 'D', Items.DIAMOND, 'N', RB), g("nucleo_5")));
		POR_PASO.put("granja", forma(new String[] {"KHK", "CNC", "KWK"},
				Map.of('K', K, 'H', Items.IRON_HOE, 'C', Items.COMPOSTER, 'W', Items.WATER_BUCKET, 'N', g("nucleo_5")), g("granja")));
		POR_PASO.put("armadura", forma(new String[] {"E E", "ENE", "EEE"},
				Map.of('E', E, 'N', g("nucleo_5")), g("peto_estelar")));
		POR_PASO.put("nucleo_6", forma(new String[] {"EXE", "XNX", "EXE"},
				Map.of('E', E, 'X', Items.NETHERITE_SCRAP, 'N', RB), g("nucleo_6")));
		POR_PASO.put("pico_5", sinForma(g("pico_chispa_5"), g("pico_chispa_4"), g("nucleo_6"), E, E));
		POR_PASO.put("corazon", forma(new String[] {"EDE", "DND", "EDE"},
				Map.of('E', E, 'D', Items.DIAMOND_BLOCK, 'N', g("nucleo_6")), g("corazon_estelar")));

		// recetas que no son un paso del objetivo, pero sirven al hacer clic en un ingrediente
		for (Receta r : POR_PASO.values()) {
			POR_ITEM.put(r.resultado(), r);
		}
		Item C = g("chispa");
		POR_ITEM.put(P, forma(new String[] {"CCC", "CCC", "CCC"}, Map.of('C', C), P));
		POR_ITEM.put(g("pico_chispa_4"), sinForma(g("pico_chispa_4"), g("pico_chispa_3"), g("nucleo_5"), E));
		POR_ITEM.put(g("espada_chispa_1"), forma(new String[] {" P ", " N ", " S "},
				Map.of('P', P, 'N', g("nucleo_2"), 'S', Items.STICK), g("espada_chispa_1")));
		Item[][] mejoras = {{g("nucleo_3"), K}, {g("nucleo_4"), K, K}, {g("nucleo_5"), E}, {g("nucleo_6"), E, E}};
		for (int lvl = 2; lvl <= 5; lvl++) {
			Item[] extra = mejoras[lvl - 2];
			Item[] items = new Item[extra.length + 1];
			items[0] = g("espada_chispa_" + (lvl - 1));
			System.arraycopy(extra, 0, items, 1, extra.length);
			POR_ITEM.put(g("espada_chispa_" + lvl), sinForma(g("espada_chispa_" + lvl), items));
		}
		Map<Character, Item> arm = Map.of('E', E, 'N', g("nucleo_5"));
		POR_ITEM.put(g("casco_estelar"), forma(new String[] {"ENE", "E E"}, arm, g("casco_estelar")));
		POR_ITEM.put(g("grebas_estelares"), forma(new String[] {"ENE", "E E", "E E"}, arm, g("grebas_estelares")));
		POR_ITEM.put(g("botas_estelares"), forma(new String[] {"E E", "ENE"}, arm, g("botas_estelares")));
	}

	/** Receta para fabricar este objeto (null si no tiene: se consigue en el mundo). */
	public static Receta deItem(Item it) {
		if (POR_PASO.isEmpty()) {
			cargar();
		}
		return it == null ? null : POR_ITEM.get(it);
	}

	public static ItemStack stack(Item it) {
		return it == null ? ItemStack.EMPTY : new ItemStack(it);
	}

	private Recetas() {
	}
}
