package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import openblocks.OpenBlocks;
import openblocks.block.ProjectorBlock;
import openblocks.block.entity.ProjectorBlockEntity;
import openblocks.common.HeightMapData;

/**
 * Port of legacy {@code TileEntityProjectorRenderer}: the spinner (turning while active, replacing the legacy armature
 * animation), the holographic cone (legacy drew it as part of the block model) and the height map above the projector.
 */
public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {

	public static final ModelResourceLocation SPINNER_INNER_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/projector_spinner_inner"));
	public static final ModelResourceLocation SPINNER_OUTER_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/projector_spinner_outer"));
	public static final ModelResourceLocation CONE_MODEL = ModelResourceLocation.standalone(OpenBlocks.location("block/projector_cone"));

	// legacy turned the inner part a full circle in 4 seconds
	private static final float INNER_DEGREES_PER_TICK = 360f / 80;
	private static final float OUTER_DEGREES_PER_TICK = -INNER_DEGREES_PER_TICK / 2;
	// the map reaches from y -64 to y 320
	private static final double MAP_TOP = 1 + 320.0 / 64;

	public ProjectorRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(SPINNER_INNER_MODEL);
		event.register(SPINNER_OUTER_MODEL);
		event.register(CONE_MODEL);
	}

	@Override
	public void render(ProjectorBlockEntity projector, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final BlockState state = projector.getBlockState();
		final boolean active = state.getValue(ProjectorBlock.ACTIVE);
		final float time = projector.getLevel() != null? projector.getLevel().getGameTime() + partialTick : 0;

		renderSpinning(SPINNER_OUTER_MODEL, active? time * OUTER_DEGREES_PER_TICK : 0, state, poseStack, bufferSource, packedLight, packedOverlay);
		renderSpinning(SPINNER_INNER_MODEL, active? time * INNER_DEGREES_PER_TICK : 0, state, poseStack, bufferSource, packedLight, packedOverlay);
		if (!active) return;

		if (OBClientConfig.PROJECTOR.renderHolographicCone.getAsBoolean()) {
			final int brightness = OBClientConfig.PROJECTOR.brightness.getAsInt();
			ExtraModels.render(CONE_MODEL, state, poseStack, bufferSource, brightness >= 0? LightTexture.pack(brightness, brightness) : packedLight, packedOverlay);
		}

		final Integer mapId = projector.getMapId();
		final HeightMapData data = mapId != null? HeightMapCache.get(mapId) : null;
		if (data == null) return;

		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(90 * projector.getRotation()));
		poseStack.translate(-0.5, 1, -0.5);
		HeightMapRenderer.render(data, poseStack.last(), bufferSource.getBuffer(HeightMapRenderer.RENDER_TYPE));
		poseStack.popPose();
	}

	private static void renderSpinning(ModelResourceLocation model, float degrees, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
		poseStack.translate(-0.5, 0, -0.5);
		ExtraModels.render(model, state, poseStack, bufferSource, packedLight, packedOverlay);
		poseStack.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(ProjectorBlockEntity projector) {
		return new AABB(projector.getBlockPos()).expandTowards(0, MAP_TOP, 0);
	}
}
