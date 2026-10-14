package openblocks.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import openblocks.OpenBlocks;
import openblocks.entity.Luggage;

/**
 * Port of legacy {@code ModelLuggage}: chest body with a lid that flaps while walking, on 7x3 tiny legs.
 */
public class LuggageModel extends HierarchicalModel<Luggage> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("luggage"), "main");

	private final ModelPart root;
	private final ModelPart lid;
	private final ModelPart body;
	private final ModelPart[][] legs = new ModelPart[7][3];

	public LuggageModel(ModelPart root) {
		this.root = root;
		this.lid = root.getChild("lid");
		this.body = root.getChild("body");
		for (int x = -3; x <= 3; x++)
			for (int z = -1; z <= 1; z++)
				legs[x + 3][z + 1] = root.getChild(legName(x, z));
	}

	private static String legName(int x, int z) {
		return "leg_" + (x + 3) + "_" + (z + 1);
	}

	public static LayerDefinition createLayer() {
		final MeshDefinition mesh = new MeshDefinition();
		final PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 23).addBox(-8, -2, -8, 16, 2, 8), PartPose.offset(0, 13, 4));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -4, 16, 7, 8), PartPose.offset(0, 13, 0));
		for (int x = -3; x <= 3; x++)
			for (int z = -1; z <= 1; z++)
				root.addOrReplaceChild(legName(x, z), CubeListBuilder.create().texOffs(0, 41).addBox(-0.5f, 0, -0.5f, 1, 4, 1), PartPose.offset(x * 2, 20, z * 2));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(Luggage luggage, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// a waiting luggage sits on the ground with its legs tucked in
		final boolean sitting = luggage.isInSittingPose();
		lid.y = body.y = sitting? 17 : 13;
		lid.xRot = Math.min(0, Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount);
		for (int x = -3; x <= 3; x++)
			for (int z = -1; z <= 1; z++) {
				final ModelPart leg = legs[x + 3][z + 1];
				leg.visible = !sitting;
				leg.xRot = Mth.cos(limbSwing + (x * z) * 0.6662f) * 1.4f * limbSwingAmount;
			}
	}
}
