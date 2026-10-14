package openblocks.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.OpenBlocks;
import openblocks.block.SkyBlock;
import openblocks.block.entity.SkyBlockEntity;
import org.joml.Matrix4f;

/**
 * Port of legacy {@code TileEntitySkyRenderer} and {@code SkyBlockRenderer}. Instead of a pre-world render hook and a
 * stencil bit, the sky is copied right after it was drawn ({@link RenderLevelStageEvent.Stage#AFTER_SKY}), and active
 * sky blocks draw their faces with a shader that samples that copy at the same screen position. The copy is only made
 * while sky blocks were drawn in the previous frame, like legacy.
 */
public class SkyBlockRenderer implements BlockEntityRenderer<SkyBlockEntity> {

	private static ShaderInstance shader;

	private static RenderTarget skyTarget;

	private static boolean skyCaptured;

	private static int lastRenderUsers;

	private static final RenderType SKY = RenderType.create(OpenBlocks.MODID + ":sky_block", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
					.setTextureState(new RenderStateShard.EmptyTextureStateShard(() -> RenderSystem.setShaderTexture(0, skyTarget.getColorTextureId()), () -> {}))
					.createCompositeState(false));

	public SkyBlockRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(), OpenBlocks.location("rendertype_sky_block"), DefaultVertexFormat.POSITION),
					loaded -> shader = loaded);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

		// there may be a single frame with users but without sky, like legacy
		skyCaptured = lastRenderUsers > 0;
		lastRenderUsers = 0;
		if (!skyCaptured) return;

		final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (skyTarget == null) {
			skyTarget = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
		} else if (skyTarget.width != main.width || skyTarget.height != main.height) {
			skyTarget.resize(main.width, main.height, Minecraft.ON_OSX);
		}

		// like RenderTarget.copyDepthFrom, but for the color
		GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, main.frameBufferId);
		GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, skyTarget.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, skyTarget.width, skyTarget.height, GlConst.GL_COLOR_BUFFER_BIT, GlConst.GL_NEAREST);
		main.bindWrite(false);
	}

	@Override
	public void render(SkyBlockEntity sky, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		final BlockState state = sky.getBlockState();
		if (!SkyBlock.isActive(state)) return;

		if (!OBClientConfig.SKY_BLOCK.renderingEnabled.getAsBoolean() || shader == null) {
			renderModel(state, poseStack, buffers, packedLight);
			return;
		}

		lastRenderUsers++;
		if (!skyCaptured || skyTarget == null) {
			renderModel(state, poseStack, buffers, packedLight);
			return;
		}

		final Level level = sky.getLevel();
		final BlockPos pos = sky.getBlockPos();
		final Matrix4f pose = poseStack.last().pose();
		final VertexConsumer consumer = buffers.getBuffer(SKY);
		renderFace(level, pos, state, pose, consumer, 0, 1, 0, 1, 1, 1, 1, 1, Direction.SOUTH);
		renderFace(level, pos, state, pose, consumer, 0, 1, 1, 0, 0, 0, 0, 0, Direction.NORTH);
		renderFace(level, pos, state, pose, consumer, 1, 1, 1, 0, 0, 1, 1, 0, Direction.EAST);
		renderFace(level, pos, state, pose, consumer, 0, 0, 0, 1, 0, 1, 1, 0, Direction.WEST);
		renderFace(level, pos, state, pose, consumer, 0, 1, 0, 0, 0, 0, 1, 1, Direction.DOWN);
		renderFace(level, pos, state, pose, consumer, 0, 1, 1, 1, 1, 1, 0, 0, Direction.UP);
	}

	// same vertex order as the end portal
	private static void renderFace(Level level, BlockPos pos, BlockState state, Matrix4f pose, VertexConsumer consumer,
			float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3, Direction direction) {
		if (level != null && !Block.shouldRenderFace(state, level, pos, direction, pos.relative(direction))) return;
		consumer.addVertex(pose, x0, y0, z0);
		consumer.addVertex(pose, x1, y0, z1);
		consumer.addVertex(pose, x1, y1, z2);
		consumer.addVertex(pose, x0, y1, z3);
	}

	// the inactive look, all states share one model
	private static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		final var blockRenderer = Minecraft.getInstance().getBlockRenderer();
		blockRenderer.getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(Sheets.solidBlockSheet()), state, blockRenderer.getBlockModel(state),
				1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.solidBlockSheet());
	}

	@Override
	public int getViewDistance() {
		return 256;
	}
}
