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
import openblocks.block.SprinklerBlock;
import openblocks.block.entity.SprinklerBlockEntity;

/**
 * Replaces the legacy sprinkler armature: the nozzle tilts around its length, following the spray direction.
 */
public class SprinklerRenderer implements BlockEntityRenderer<SprinklerBlockEntity> {

	public static final ModelResourceLocation NOZZLE_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/sprinkler_nozzle"));

	private static final float NOZZLE_Y = 3f / 16;

	public SprinklerRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(NOZZLE_MODEL);
	}

	@Override
	public void render(SprinklerBlockEntity sprinkler, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final BlockState state = sprinkler.getBlockState();

		poseStack.pushPose();
		poseStack.translate(0.5, NOZZLE_Y, 0.5);
		if (state.getValue(SprinklerBlock.AXIS) == Direction.Axis.X) poseStack.mulPose(Axis.YP.rotationDegrees(90));
		poseStack.mulPose(Axis.ZP.rotationDegrees(-SprinklerBlockEntity.MAX_NOZZLE_ANGLE * sprinkler.getSprayDirection(partialTick)));
		poseStack.translate(-0.5, -NOZZLE_Y, -0.5);
		ExtraModels.render(NOZZLE_MODEL, state, poseStack, bufferSource, packedLight, packedOverlay);
		poseStack.popPose();
	}
}
