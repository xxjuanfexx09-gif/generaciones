package com.generaciones.client.eclipse;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.generaciones.Generaciones;
import com.generaciones.boss.Eclipse;

public class EclipseRenderer extends MobRenderer<Eclipse, EclipseRenderState, EclipseModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Generaciones.id("eclipse"), "main");
	private static final Identifier TEXTURA = Generaciones.id("textures/entity/eclipse.png");
	private static final Identifier TEXTURA_FURIA = Generaciones.id("textures/entity/eclipse_furia.png");

	public EclipseRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new EclipseModel(ctx.bakeLayer(LAYER)), 2.0f);
	}

	@Override
	public EclipseRenderState createRenderState() {
		return new EclipseRenderState();
	}

	@Override
	public void extractRenderState(Eclipse e, EclipseRenderState s, float partial) {
		super.extractRenderState(e, s, partial);
		int intro = e.intro();
		s.intro = intro > 0 ? (intro - partial) / Eclipse.INTRO : 0f;
		s.fase = e.fase();
		int g = e.golpeAnim();
		s.golpe = g > 0 ? Math.max(0f, g - partial) : 0f;
	}

	@Override
	protected void scale(EclipseRenderState s, PoseStack pose) {
		// durante la entrada se va formando: crece desde casi nada
		float k = 2.0f * (1f - s.intro * s.intro * 0.9f);
		pose.scale(k, k, k);
	}

	@Override
	public Identifier getTextureLocation(EclipseRenderState s) {
		return s.fase >= 2 ? TEXTURA_FURIA : TEXTURA;
	}

	@Override
	protected int getBlockLightLevel(Eclipse e, BlockPos pos) {
		return 15;
	}

	@Override
	protected float getFlipDegrees() {
		return 0f;
	}
}
