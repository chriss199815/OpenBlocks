package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code ModelCannon}: barrel on a base, with three spokes per side for the wheels; on firing the barrel
 * recoils and the wheels turn.
 */
public class CannonModel {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("cannon"), "main");

	private static final double[] RECOIL = { 0, 0.5, 1, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0 };
	private static final float DEG_30 = 30 * Mth.DEG_TO_RAD;

	private final ModelPart body;
	private final ModelPart shooter;
	private final ModelPart base;
	private final ModelPart wheel;

	public CannonModel(ModelPart root) {
		body = root.getChild("body");
		shooter = root.getChild("shooter");
		base = root.getChild("base");
		wheel = root.getChild("wheel");
	}

	public static LayerDefinition createLayer() {
		final MeshDefinition mesh = new MeshDefinition();
		final PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -5, -3, 6, 6, 6), PartPose.offset(0, 11, 3));
		root.addOrReplaceChild("shooter", CubeListBuilder.create().texOffs(34, 0).addBox(-2, -4, 2, 4, 4, 6), PartPose.offset(0, 11, 3));
		root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(14, 19).addBox(-6, 0, -6, 12, 1, 12), PartPose.offset(0, 15, 0));
		root.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(0, 20).addBox(3, -3, -3, 1, 6, 6), PartPose.offset(0, 11, 3));
		return LayerDefinition.create(mesh, 64, 32);
	}

	public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, double pitch, int ticksSinceLastFire) {
		final float ease = (float)RECOIL[Math.min(12, ticksSinceLastFire)];
		final float shooterAnim = -3.0f * ease;
		final float pitchRad = (float)Math.toRadians(pitch);
		final float cannonOffset = ease * 4;

		shooter.xRot = pitchRad;
		shooter.y = 11 - shooterAnim * Mth.sin(pitchRad);
		shooter.z = 3 + shooterAnim * Mth.cos(pitchRad) - cannonOffset;
		shooter.xRot += ease / 2;
		body.z = 3 - cannonOffset;
		body.xRot = shooter.xRot;

		body.render(poseStack, consumer, light, overlay);
		shooter.render(poseStack, consumer, light, overlay);
		base.render(poseStack, consumer, light, overlay);

		wheel.z = -cannonOffset;
		renderWheel(poseStack, consumer, light, overlay, 0, ease);
		renderWheel(poseStack, consumer, light, overlay, Mth.PI, -ease);
	}

	// three spokes, 30 degrees apart
	private void renderWheel(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float zRot, float startAngle) {
		wheel.zRot = zRot;
		wheel.xRot = startAngle;
		wheel.x = 0;
		for (int i = 0; i < 3; i++) {
			wheel.render(poseStack, consumer, light, overlay);
			wheel.xRot += DEG_30;
			wheel.x -= 0.01f;
		}
	}
}
