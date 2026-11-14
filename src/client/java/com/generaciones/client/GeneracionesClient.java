package com.generaciones.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import com.generaciones.Generaciones;

/** Cliente: recuadro de objetivo, barras de cine y el modelo del Eclipse. */
public class GeneracionesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.addLast(Generaciones.id("objetivo"), new ObjetivoHud());
		RecetaPanel.register();
		HudElementRegistry.addLast(Generaciones.id("cinematica"), new com.generaciones.client.eclipse.Cinematica());
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
				.register(com.generaciones.client.eclipse.Cinematica::tick);
		net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
				com.generaciones.client.eclipse.EclipseRenderer.LAYER, com.generaciones.client.eclipse.EclipseModel::createLayer);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.generaciones.ModContent.ECLIPSE, com.generaciones.client.eclipse.EclipseRenderer::new);
	}
}
