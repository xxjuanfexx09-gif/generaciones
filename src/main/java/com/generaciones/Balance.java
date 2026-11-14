package com.generaciones;

/** Todos los números del mod en un solo lugar. Cambia aquí la velocidad o la capacidad. */
public final class Balance {
	public static final int MAX_GEN = 7;

	/** Producción por minuto: 6, 18, 54, 162, 486, 1458, 4374 (cada generación ×3). */
	public static double perMinute(int gen) {
		return 6.0 * Math.pow(3, gen - 1);
	}

	/** Cuántas guarda antes de llenarse: 10 minutos de producción. */
	public static long capacity(int gen) {
		return Math.round(perMinute(gen) * 10);
	}

	/** Valor en chispas de cada forma compacta. */
	public static final long PAQUETE = 9;
	public static final long CRISTAL = 81;
	public static final long ESTRELLA = 729;

	/** Valor en chispas de un objeto de chispas (0 si no es de chispas). */
	public static long valueOf(net.minecraft.world.item.ItemStack stack) {
		if (stack.is(ModContent.CHISPA)) return stack.getCount();
		if (stack.is(ModContent.PAQUETE_CHISPAS)) return stack.getCount() * PAQUETE;
		if (stack.is(ModContent.CRISTAL_CHISPAS)) return stack.getCount() * CRISTAL;
		if (stack.is(ModContent.ESTRELLA_CHISPAS)) return stack.getCount() * ESTRELLA;
		return 0;
	}

	public static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			default -> String.valueOf(n);
		};
	}

	private Balance() {
	}
}
