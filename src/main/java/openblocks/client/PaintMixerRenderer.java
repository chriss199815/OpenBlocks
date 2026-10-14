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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.OpenBlocks;
import openblocks.block.PaintMixerBlock;
import openblocks.block.entity.PaintMixerBlockEntity;

/**
 * Replaces the legacy paint mixer armature: draws the can in the mixer, tinted with the paint color, which fades to the new color
 * and shakes while mixing.
 */
public class PaintMixerRenderer implements BlockEntityRenderer<PaintMixerBlockEntity> {

	public static final ModelResourceLocation CAN_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/paint_mixer_can"));

	// legacy armature: four seconds per swing
	private static final float CYCLE_TICKS = 80;

	public PaintMixerRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(CAN_MODEL);
	}

	@Override
	public void render(PaintMixerBlockEntity mixer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!mixer.hasPaint()) return;

		final BlockState state = mixer.getBlockState();
		final BakedModel model = Minecraft.getInstance().getModelManager().getModel(CAN_MODEL);
		final int color = mixer.getDisplayColor(partialTick);

		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(-state.getValue(PaintMixerBlock.FACING).toYRot() + 180));
		if (mixer.isWorking()) {
			final float time = (mixer.getLevel().getGameTime() + partialTick) / CYCLE_TICKS * Mth.TWO_PI;
			poseStack.translate(0, Math.abs(Mth.sin(time * 6)) * 0.02f, 0);
			poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time) * 10));
		}
		poseStack.translate(-0.5, 0, -0.5);

		final ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
		for (RenderType renderType : model.getRenderTypes(state, mixer.getLevel().random, ModelData.EMPTY))
			renderer.renderModel(poseStack.last(), bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(renderType, false)), state, model,
					((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f,
					packedLight, packedOverlay, ModelData.EMPTY, renderType);
		poseStack.popPose();
	}
}
