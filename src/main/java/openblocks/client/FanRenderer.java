package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.OpenBlocks;
import openblocks.block.entity.FanBlockEntity;

/**
 * Replaces the legacy fan armatures: frame turned to the fan angle, blades spinning with the redstone signal.
 */
public class FanRenderer implements BlockEntityRenderer<FanBlockEntity> {

	public static final ModelResourceLocation FRAME_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/fan_frame"));
	public static final ModelResourceLocation BLADES_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/fan_blades"));

	// hub of the blades in the frame, in block units
	private static final float HUB_Y = 10.75f / 16;
	private static final float HUB_Z = 7f / 16;

	public FanRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(FRAME_MODEL);
		event.register(BLADES_MODEL);
	}

	@Override
	public void render(FanBlockEntity fan, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final BlockState state = fan.getBlockState();

		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(-fan.getAngle()));
		poseStack.translate(-0.5, 0, -0.5);
		renderModel(FRAME_MODEL, state, poseStack, bufferSource, packedLight, packedOverlay);

		poseStack.translate(0.5, HUB_Y, HUB_Z);
		poseStack.mulPose(Axis.ZP.rotationDegrees(fan.getBladeRotation(partialTick)));
		poseStack.translate(-0.5, -HUB_Y, -HUB_Z);
		renderModel(BLADES_MODEL, state, poseStack, bufferSource, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private static void renderModel(ModelResourceLocation location, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
		final ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
		for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(42), ModelData.EMPTY))
			renderer.renderModel(poseStack.last(), bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(renderType, false)), state, model,
					1, 1, 1, packedLight, packedOverlay, ModelData.EMPTY, renderType);
	}
}
