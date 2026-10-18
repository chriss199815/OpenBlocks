package openblocks.client;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code ModelCraneBackpack}: the backpack with its support on the body, and the arm on top of the
 * support, turning with the head. Only body and arm are drawn.
 */
public class CraneBackpackModel extends HumanoidModel<LivingEntity> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("crane_backpack"), "main");

	// top of the support, in body coordinates
	private static final float ARM_Y = -16;
	private static final float ARM_Z = 7;

	private final ModelPart arm;

	public CraneBackpackModel(ModelPart root) {
		super(root);
		arm = root.getChild("crane_arm");
	}

	public static LayerDefinition createLayer() {
		final MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
		final PartDefinition root = mesh.getRoot();
		for (String part : new String[] { "head", "hat", "right_arm", "left_arm", "right_leg", "left_leg" })
			root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4, 0, -2, 8, 12, 8)
				.texOffs(32, 0).addBox(-1, -16, 6, 2, 24, 2), PartPose.ZERO);
		root.addOrReplaceChild("crane_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0, 1, 2, 2, 42), PartPose.offset(0, ARM_Y, ARM_Z));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	protected Iterable<ModelPart> bodyParts() {
		return ImmutableList.of(body, arm);
	}

	/**
	 * The arm stays on top of the support (also when the body leans forward while crouching) and points where the head
	 * looks.
	 */
	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		final float sin = Mth.sin(body.xRot);
		final float cos = Mth.cos(body.xRot);
		arm.x = body.x;
		arm.y = body.y + ARM_Y * cos - ARM_Z * sin;
		arm.z = body.z + ARM_Y * sin + ARM_Z * cos;
		arm.xRot = 0;
		arm.yRot = Mth.PI + head.yRot;
		super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
	}

	/**
	 * The arm alone, pointing along +z; for the first person view.
	 */
	public void renderArm(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
		arm.setPos(0, ARM_Y, ARM_Z);
		arm.setRotation(0, 0, 0);
		arm.render(poseStack, buffer, packedLight, packedOverlay);
	}
}
