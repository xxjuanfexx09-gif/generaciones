package com.generaciones.client.eclipse;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Modelo del Eclipse: núcleo con ojo y púas, anillo de 8 cristales que giran
 * y 4 manos flotantes que suben y golpean el suelo.
 */
public class EclipseModel extends EntityModel<EclipseRenderState> {
	private static final int CRISTALES = 8;
	private static final float CENTRO_Y = 4f;

	private final ModelPart nucleo;
	private final ModelPart[] cristales = new ModelPart[CRISTALES];
	private final ModelPart[] manos = new ModelPart[4];

	public EclipseModel(ModelPart root) {
		super(root);
		this.nucleo = root.getChild("nucleo");
		for (int i = 0; i < CRISTALES; i++) {
			cristales[i] = root.getChild("cristal" + i);
		}
		for (int i = 0; i < 4; i++) {
			manos[i] = root.getChild("mano" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition nucleo = root.addOrReplaceChild("nucleo",
				CubeListBuilder.create().texOffs(0, 0).addBox(-8, -8, -8, 16, 16, 16),
				PartPose.offset(0, CENTRO_Y, 0));
		// ojo (sobresale por delante)
		nucleo.addOrReplaceChild("ojo", CubeListBuilder.create().texOffs(0, 32).addBox(-4, -4, -9, 8, 8, 1),
				PartPose.ZERO);
		// púas de la corona
		CubeListBuilder pua = CubeListBuilder.create().texOffs(40, 32).addBox(-1, -6, -1, 2, 6, 2);
		for (int i = 0; i < 4; i++) {
			float a = i * Mth.HALF_PI + Mth.PI / 4;
			nucleo.addOrReplaceChild("pua" + i, pua,
					PartPose.offsetAndRotation(Mth.cos(a) * 6, -7, Mth.sin(a) * 6, Mth.sin(a) * 0.5f, 0, -Mth.cos(a) * 0.5f));
		}
		nucleo.addOrReplaceChild("pua_alta", CubeListBuilder.create().texOffs(40, 32).addBox(-1, -9, -1, 2, 9, 2),
				PartPose.offset(0, -7, 0));

		CubeListBuilder cristal = CubeListBuilder.create().texOffs(48, 32).addBox(-1.5f, -5, -1.5f, 3, 10, 3);
		for (int i = 0; i < CRISTALES; i++) {
			root.addOrReplaceChild("cristal" + i, cristal, PartPose.offset(0, CENTRO_Y, 0));
		}
		CubeListBuilder mano = CubeListBuilder.create()
				.texOffs(0, 44).addBox(-3.5f, -1, -4, 7, 2, 8)          // palma
				.texOffs(30, 44).addBox(-3.5f, -0.5f, -8, 1.5f, 1.5f, 4) // dedos
				.texOffs(30, 44).addBox(-1, -0.5f, -9, 1.5f, 1.5f, 5)
				.texOffs(30, 44).addBox(1.5f, -0.5f, -8, 1.5f, 1.5f, 4)
				.texOffs(30, 50).addBox(3.5f, -0.5f, -3, 2, 1.5f, 3);    // pulgar
		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("mano" + i, mano, PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(EclipseRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		boolean furia = s.fase >= 2;

		// núcleo: flota, respira y mira al jugador
		float respira = 1f + Mth.sin(t * (furia ? 0.3f : 0.15f)) * 0.04f;
		nucleo.y = CENTRO_Y + Mth.sin(t * 0.08f) * 1.5f;
		nucleo.xScale = nucleo.yScale = nucleo.zScale = respira;
		nucleo.xRot = s.xRot * Mth.DEG_TO_RAD;
		nucleo.zRot = Mth.sin(t * 0.05f) * 0.08f;

		// anillo de cristales: gira (más rápido en furia) y se inclina
		float giro = t * (furia ? 0.09f : 0.04f);
		float radio = 17f + Mth.sin(t * 0.1f) * (furia ? 3f : 1f);
		for (int i = 0; i < CRISTALES; i++) {
			float a = giro + i * Mth.TWO_PI / CRISTALES;
			ModelPart c = cristales[i];
			c.x = Mth.cos(a) * radio;
			c.z = Mth.sin(a) * radio;
			c.y = nucleo.y + Mth.sin(a * 2 + t * 0.1f) * 3f;
			c.yRot = -a;
			c.zRot = 0.35f;
			c.xRot = t * 0.05f + i;
		}

		// manos: flotan a los lados; en el golpe suben y bajan de golpe
		float alzar = 0f;
		if (s.golpe > 0) {
			float p = (30f - s.golpe) / 30f; // 0 → 1
			alzar = p < 0.6f ? -12f * (p / 0.6f) : -12f + 40f * Math.min(1f, (p - 0.6f) / 0.15f);
		}
		for (int i = 0; i < 4; i++) {
			ModelPart m = manos[i];
			float lado = (i % 2 == 0) ? -1 : 1;
			float delante = (i < 2) ? -1 : 1;
			float fase = i * 1.3f;
			m.x = lado * (22f + Mth.sin(t * 0.07f + fase) * 2f);
			m.z = delante * 9f + Mth.cos(t * 0.06f + fase) * 2f;
			m.y = nucleo.y + 6f + Mth.sin(t * 0.09f + fase) * 3f + alzar;
			m.yRot = lado * 0.3f + (delante > 0 ? Mth.PI : 0);
			m.xRot = s.golpe > 0 ? 0.2f : 0.5f + Mth.sin(t * 0.08f + fase) * 0.15f;
			m.zRot = -lado * 0.25f;
		}

		// muerte: se encoge girando
		if (s.deathTime > 0) {
			float d = Math.min(1f, s.deathTime / 20f);
			float k = 1f - d * 0.9f;
			nucleo.xScale = nucleo.yScale = nucleo.zScale = k;
			nucleo.yRot += d * 6f;
			for (ModelPart c : cristales) {
				c.x *= 1f + d * 2f;
				c.z *= 1f + d * 2f;
			}
		}
	}
}
