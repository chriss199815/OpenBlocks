package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.entity.MountedBlock;

/**
 * Draws the carried block like an item would (so chests and other blocks drawn by their block entity show up too).
 */
public class MountedBlockRenderer extends EntityRenderer<MountedBlock> {

	private final BlockRenderDispatcher blockRenderer;

	public MountedBlockRenderer(EntityRendererProvider.Context context) {
		super(context);
		blockRenderer = context.getBlockRenderDispatcher();
		shadowRadius = 0.5f;
	}

	@Override
	public void render(MountedBlock block, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		final BlockState state = block.getBlockState();
		if (state.getRenderShape() != RenderShape.INVISIBLE) {
			poseStack.pushPose();
			poseStack.translate(-0.5, 0, -0.5);
			blockRenderer.renderSingleBlock(state, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
			poseStack.popPose();
		}
		super.render(block, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ResourceLocation getTextureLocation(MountedBlock block) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
