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
import openblocks.OpenBlocks;

/**
 * Port of the base of legacy {@code ModelCartographer}: a plate with a turning holder whose two arms carry the eye.
 * Drawn without the usual entity flip, so the arms point up.
 */
public class CartographerModel {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("cartographer"), "main");

	private final ModelPart body;
	private final ModelPart holder;

	public CartographerModel(ModelPart root) {
		body = root.getChild("body");
		holder = body.getChild("holder");
	}

	public static LayerDefinition createLayer() {
		final MeshDefinition mesh = new MeshDefinition();
		final PartDefinition root = mesh.getRoot();
		final PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -1.5f, -2.5f, 5, 2, 5), PartPose.ZERO);
		body.addOrReplaceChild("holder", CubeListBuilder.create()
				.texOffs(0, 7).addBox(-1.5f, 0.5f, -1.5f, 3, 1, 3)
				.texOffs(0, 11).addBox(-0.5f, 0.5f, -2.5f, 1, 4, 1)
				.texOffs(4, 11).addBox(-0.5f, 0.5f, 1.5f, 1, 4, 1), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float holderYaw) {
		holder.yRot = holderYaw;
		body.render(poseStack, consumer, light, overlay);
	}
}
