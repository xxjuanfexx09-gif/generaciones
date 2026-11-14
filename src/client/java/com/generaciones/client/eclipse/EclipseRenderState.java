package com.generaciones.client.eclipse;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class EclipseRenderState extends LivingEntityRenderState {
	/** 0 = terminó la entrada; 1 = recién aparece. */
	public float intro;
	public int fase = 1;
	/** Ticks de la animación de golpe (30 → 0). */
	public float golpe;
}
