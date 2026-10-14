package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import openblocks.OpenBlocks;
import openblocks.block.BearTrapBlock;
import openblocks.block.entity.BearTrapBlockEntity;

/**
 * Replaces the legacy bear trap armature: the jaws rise around the hinge when the trap shuts and fall open in
 * {@link BearTrapBlockEntity#OPENING_TICKS}.
 */
public class BearTrapRenderer implements BlockEntityRenderer<BearTrapBlockEntity> {

	public static final ModelResourceLocation BASE_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/bear_trap_base"));
	public static final ModelResourceLocation JAW_WEST_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/bear_trap_jaw_west"));
	public static final ModelResourceLocation JAW_EAST_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/bear_trap_jaw_east"));

	// legacy armature: the jaws stop short of upright
	private static final float SHUT_ANGLE = 75;
	private static final float HINGE_Y = 0.5f / 16;

	public BearTrapRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(BASE_MODEL);
		event.register(JAW_WEST_MODEL);
		event.register(JAW_EAST_MODEL);
	}

	@Override
	public void render(BearTrapBlockEntity trap, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final BlockState state = trap.getBlockState();
		final float angle = SHUT_ANGLE * trap.getClosedFraction(partialTick);

		poseStack.pushPose();
		if (state.getValue(BearTrapBlock.AXIS) == Direction.Axis.X) {
			poseStack.translate(0.5, 0, 0.5);
			poseStack.mulPose(Axis.YP.rotationDegrees(90));
			poseStack.translate(-0.5, 0, -0.5);
		}
		ExtraModels.render(BASE_MODEL, state, poseStack, bufferSource, packedLight, packedOverlay);
		renderJaw(JAW_WEST_MODEL, -angle, state, poseStack, bufferSource, packedLight, packedOverlay);
		renderJaw(JAW_EAST_MODEL, angle, state, poseStack, bufferSource, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private static void renderJaw(ModelResourceLocation location, float angle, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		poseStack.pushPose();
		poseStack.translate(0.5, HINGE_Y, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
		poseStack.translate(-0.5, -HINGE_Y, 0);
		ExtraModels.render(location, state, poseStack, bufferSource, packedLight, packedOverlay);
		poseStack.popPose();
	}
}
