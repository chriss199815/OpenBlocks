package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code ModelSonicGlasses}: glasses with ear pieces and two horns, drawn 1.2 times as large as the
 * head. Only the head is drawn.
 */
public class SonicGlassesModel extends HumanoidModel<LivingEntity> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("sonic_glasses"), "main");

	private static final float SCALE = 1.2f;

	public SonicGlassesModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		final MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
		final PartDefinition root = mesh.getRoot();
		for (String part : new String[] { "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg" })
			root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
				// bar
				.texOffs(0, 16).addBox(-5, -4, -5, 10, 1, 1)
				// ears
				.texOffs(0, 22).addBox(-6, -5, -1, 2, 3, 3)
				.texOffs(0, 22).addBox(4, -5, -1, 2, 3, 3)
				// eyes
				.texOffs(0, 28).addBox(-3, -5, -5, 2, 3, 1)
				.texOffs(0, 28).addBox(1, -5, -5, 2, 3, 1)
				// wood handles
				.texOffs(0, 18).addBox(-5, -4, -4, 1, 1, 3)
				.texOffs(0, 18).addBox(4, -4, -4, 1, 1, 3)
				// up tubes
				.texOffs(10, 18).addBox(-5, -10, 0, 1, 5, 1)
				.texOffs(10, 18).addBox(4, -10, 0, 1, 5, 1)
				// cones
				.texOffs(32, 0).addBox(-6, -11, -3, 3, 3, 3)
				.texOffs(32, 0).addBox(3, -11, -3, 3, 3, 3)
				.texOffs(32, 6).addBox(-7, -12, -7, 5, 5, 4)
				.texOffs(32, 6).addBox(2, -12, -7, 5, 5, 4)
				.texOffs(32, 15).addBox(-8, -13, -9, 7, 7, 2)
				.texOffs(32, 15).addBox(1, -13, -9, 7, 7, 2), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		poseStack.pushPose();
		poseStack.scale(SCALE, SCALE, SCALE);
		head.render(poseStack, buffer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}
}
