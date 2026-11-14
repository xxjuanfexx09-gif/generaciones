package com.generaciones.test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.generaciones.Generaciones;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/** Abre el juego real, coloca los 6 generadores, toma capturas y mide si 1000 generadores dan lag. */
@SuppressWarnings("UnstableApiUsage")
public class GeneracionesClientGameTest implements FabricClientGameTest {


	private static void promo(ClientGameTestContext context, String name) {
		context.runOnClient(c -> c.gui.hud.toggle());
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080));
		context.runOnClient(c -> c.gui.hud.toggle());
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			TestServerContext server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			server.runCommand("time set 1000");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");

			// el manual llega solo al entrar: abrirlo con clic derecho
			context.waitTicks(40);
			context.getInput().pressKey(o -> o.keyUse);
			context.waitTicks(20);
			context.takeScreenshot("generaciones-0-manual");
			context.setScreen(() -> null);
			context.waitTicks(10);

			context.waitTicks(20);
			context.takeScreenshot("generaciones-0-objetivo-inicio");
			context.getInput().pressKey(o -> o.keyInventory);
			context.waitTicks(10);
			context.takeScreenshot("generaciones-0-receta-inventario");
			context.setScreen(() -> null);
			context.waitTicks(5);

			// logros: al tener un generador se completa el primer paso de la guía
			server.runCommand("give @a generaciones:generador");
			server.runCommand("give @a generaciones:chispa 5");
			context.waitTicks(40);
			context.getInput().pressKey(o -> o.keyAdvancements);
			context.waitTicks(20);
			context.takeScreenshot("generaciones-0-logros");
			context.setScreen(() -> null);
			context.waitTicks(10);

			BlockPos p = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition());
			int x = p.getX();
			int y = p.getY();
			int z = p.getZ() + 4;

			// los 6 generadores en fila, Gen I a Gen VI
			for (int g = 1; g <= 7; g++) {
				server.runCommand("setblock " + (x - 5 + g * 2 - 2) + " " + y + " " + z + " generaciones:generador[gen=" + g + "]");
			}
			server.runCommand("tp @a " + x + " " + (y + 1) + " " + (z - 5) + " facing " + x + " " + y + " " + z);
			server.runCommand("clear @a");
			server.runCommand("give @a generaciones:nucleo_2");
			server.runCommand("give @a generaciones:nucleo_3");
			server.runCommand("give @a generaciones:nucleo_4");
			server.runCommand("give @a generaciones:nucleo_5");
			server.runCommand("give @a generaciones:nucleo_6");
			server.runCommand("give @a generaciones:chispa 32");
			server.runCommand("give @a generaciones:paquete_chispas 12");
			server.runCommand("give @a generaciones:cristal_chispas 5");
			server.runCommand("give @a generaciones:estrella_chispas 1");
			context.waitTicks(60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("generaciones-1-fila");
			promo(context, "promo-generadores");

			// medir rendimiento del servidor antes y después de 1000 generadores
			context.waitTicks(100);
			double before = server.computeOnServer(s -> s.getAverageTickTimeNanos() / 1_000_000.0);
			server.runCommand("fill " + (x - 5) + " " + (y + 20) + " " + (z + 20) + " " + (x + 4) + " " + (y + 29) + " " + (z + 29)
					+ " generaciones:generador[gen=6]");
			context.waitTicks(200);
			double after = server.computeOnServer(s -> s.getAverageTickTimeNanos() / 1_000_000.0);
			Generaciones.LOG.info("RENDIMIENTO ms por tick: antes={} con 1000 generadores={}", String.format("%.3f", before), String.format("%.3f", after));
			int fps = context.computeOnClient(c -> c.getFps());
			Generaciones.LOG.info("FPS en el renderizador por software de la prueba: {}", fps);

			// usar el generador del centro y ver el mensaje
			server.runCommand("tp @a " + (x - 1) + " " + (y + 1) + " " + (z - 3) + " facing " + (x - 1) + " " + y + " " + z);
			server.runCommand("clear @a");
			context.waitTicks(20);
			server.runOnServer(s -> {
				ServerPlayer sPlayer = s.getPlayerList().getPlayers().get(0);
				var level = sPlayer.level();
				BlockPos target = new BlockPos(x - 1, y, z);
				if (level.getBlockEntity(target) instanceof com.generaciones.block.GeneradorBlockEntity be) {
					be.collect(sPlayer);
				}
			});
			context.waitTicks(10);
			context.takeScreenshot("generaciones-2-recoger");

			// v2: herramientas y máquinas de cada era
			int mz = z - 8;
			server.runCommand("fill " + (x - 6) + " " + y + " " + (mz - 3) + " " + (x + 6) + " " + (y + 4) + " " + (mz + 3) + " air");
			String[] maq = {"colector", "minero", "granja"};
			for (int i = 0; i < 3; i++) {
				int mx = x - 3 + i * 3;
				server.runCommand("setblock " + mx + " " + y + " " + mz + " generaciones:" + maq[i]);
				server.runCommand("setblock " + mx + " " + (y + 1) + " " + mz + " chest");
				server.runCommand("setblock " + (mx + 1) + " " + y + " " + mz + " generaciones:generador[gen=" + (2 + i * 2) + "]");
			}
			server.runCommand("tp @a " + x + " " + (y + 2) + " " + (mz - 5) + " facing " + x + " " + y + " " + mz);
			server.runCommand("clear @a");
			for (int i = 1; i <= 5; i++) {
				server.runCommand("give @a generaciones:pico_chispa_" + i);
			}
			server.runCommand("give @a generaciones:espada_chispa_5");
			server.runCommand("give @a generaciones:corazon_estelar");
			server.runCommand("give @a generaciones:nucleo_7");
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("generaciones-3-eras");
			context.waitTicks(140);
			context.takeScreenshot("generaciones-5-objetivo");
			context.getInput().pressKey(o -> o.keyInventory);
			context.waitTicks(10);
			context.takeScreenshot("generaciones-5c-receta-colector");
			context.runOnClient(c -> com.generaciones.client.RecetaPanel.abrir(com.generaciones.ModContent.NUCLEO_2));
			context.waitTicks(5);
			context.takeScreenshot("generaciones-5d-receta-nucleo");
			context.setScreen(() -> null);
			context.waitTicks(5);

			// armadura estelar puesta, vista en tercera persona
			server.runCommand("item replace entity @a armor.head with generaciones:casco_estelar");
			server.runCommand("item replace entity @a armor.chest with generaciones:peto_estelar");
			server.runCommand("item replace entity @a armor.legs with generaciones:grebas_estelares");
			server.runCommand("item replace entity @a armor.feet with generaciones:botas_estelares");
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("generaciones-5b-armadura");
			promo(context, "promo-armadura");
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

			// jefe final: El Eclipse (quita el cubo de 1000 generadores de la prueba de rendimiento)
			server.runCommand("fill " + (x - 5) + " " + (y + 20) + " " + (z + 20) + " " + (x + 4) + " " + (y + 29) + " " + (z + 29) + " air");
			server.runCommand("difficulty normal");
			server.runCommand("gamemode creative @a");
			server.runCommand("fill " + (x - 12) + " " + y + " " + (mz - 4) + " " + (x + 12) + " " + (y + 20) + " " + (mz + 20) + " air");
			server.runCommand("tp @a " + (x + 14) + " " + (y + 4) + " " + (mz - 6) + " facing " + x + " " + (y + 5) + " " + (mz + 8));
			server.runOnServer(s -> {
				ServerPlayer sPlayer = s.getPlayerList().getPlayers().get(0);
				com.generaciones.boss.Guardian.summon(sPlayer.level(), new BlockPos(x, y, mz + 8));
			});
			context.waitTicks(25);
			context.takeScreenshot("generaciones-4a-eclipse-entrada");
			context.waitTicks(30);
			context.takeScreenshot("generaciones-4b-eclipse-entrada");
			context.waitTicks(70);
			context.takeScreenshot("generaciones-6-eclipse");
			server.runOnServer(s -> {
				var sp2 = s.getPlayerList().getPlayers().get(0);
				for (var e : s.overworld().getEntitiesOfClass(com.generaciones.boss.Eclipse.class, sp2.getBoundingBox().inflate(60))) {
					s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),
							String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f facing entity @e[type=generaciones:eclipse,limit=1] eyes",
									e.getX() + 6, e.getY() - 1.5, e.getZ() - 6));
				}
			});
			context.waitTicks(20);
			promo(context, "promo-eclipse");
			server.runOnServer(s -> {
				for (var e : s.overworld().getEntitiesOfClass(com.generaciones.boss.Eclipse.class,
						s.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(60))) {
					e.setHealth(200f);
				}
			});
			context.waitTicks(40);
			context.takeScreenshot("generaciones-7-eclipse-furia");
			context.waitTicks(60);
			promo(context, "promo-furia");
		}
	}
}
