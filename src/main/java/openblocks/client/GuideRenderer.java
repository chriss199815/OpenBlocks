package openblocks.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import openblocks.OpenBlocks;
import openblocks.block.GuideBlock;
import openblocks.block.entity.GuideBlockEntity;
import openblocks.common.GuideButtons;
import org.joml.Matrix4f;

/**
 * Port of legacy {@code TileEntityGuideRenderer} and {@code TileEntityBuilderGuideRenderer}. Legacy drew the markers
 * instanced with its own shader (or display lists as fallback); here all markers of a shape go into one vertex buffer,
 * built when the shape changes and drawn tinted with the guide color, blended additively like legacy. When the shape
 * changes, the old one fades out while the new one fades in (legacy shrank and grew the markers). The builder guide also
 * has three small cubes circling its center.
 */
public class GuideRenderer implements BlockEntityRenderer<GuideBlockEntity> {

	private static final ResourceLocation MARKER_TEXTURE = OpenBlocks.location("textures/block/guide.png");

	// legacy marker model: 0.8 to 15.2 pixels
	private static final float MARKER_MIN = 0.8f / 16;
	private static final float MARKER_MAX = 15.2f / 16;

	private static final float CUBE_RADIUS = 0.4f;
	private static final float CUBE_HALF_SIZE = 0.03f;
	private static final int CUBE_COLOR = 0xFF062320;

	// omega and delta of theta and phi for each cube, from legacy
	private static final float[][] CUBE_MOTION = {
			{ 0.0413f, 0.5423f, 0.0765f, 0.4241f },
			{ 0.0543f, 0.1295f, 0.0914f, 0.6532f },
			{ 0.0624f, 0.6243f, 0.0351f, 0.7635f },
	};

	public GuideRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(GuideBlockEntity guide, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (guide instanceof GuideBlockEntity.Builder && guide.getLevel() != null)
			renderCubes(guide.getLevel().getGameTime() + partialTick, poseStack, bufferSource.getBuffer(RenderType.debugQuads()));

		if (!guide.isEnabled()) return;

		final Meshes meshes = Meshes.of(guide);
		final List<BlockPos> shape = guide.getShape();
		final List<BlockPos> previousShape = guide.getPreviousShape();
		final float progress = guide.getTimeSinceChange(partialTick) / GuideBlockEntity.FADE_TICKS;
		meshes.retain(shape, progress < 1? previousShape : null);

		final int color = guide.getSettings().color();
		final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
		drawShape(meshes.get(shape), modelView, color, progress);
		if (progress < 1 && previousShape != null) drawShape(meshes.get(previousShape), modelView, color, 1 - progress);
	}

	private static void drawShape(@Nullable VertexBuffer buffer, Matrix4f modelView, int color, float alpha) {
		if (buffer == null) return;

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, MARKER_TEXTURE);
		RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, alpha);

		buffer.bind();
		buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
		VertexBuffer.unbind();

		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	@Nullable
	private static VertexBuffer buildMesh(List<BlockPos> shape) {
		final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		for (BlockPos pos : shape)
			addCube(builder, pos.getX() + MARKER_MIN, pos.getY() + MARKER_MIN, pos.getZ() + MARKER_MIN,
					pos.getX() + MARKER_MAX, pos.getY() + MARKER_MAX, pos.getZ() + MARKER_MAX);

		final MeshData mesh = builder.build();
		if (mesh == null) return null;
		final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		buffer.bind();
		buffer.upload(mesh);
		VertexBuffer.unbind();
		return buffer;
	}

	// counter-clockwise from outside, texture over each face like the legacy marker model
	private static void addCube(BufferBuilder builder, float x0, float y0, float z0, float x1, float y1, float z1) {
		final float u0 = MARKER_MIN;
		final float u1 = MARKER_MAX;
		// down
		builder.addVertex(x0, y0, z1).setUv(u0, u1);
		builder.addVertex(x0, y0, z0).setUv(u0, u0);
		builder.addVertex(x1, y0, z0).setUv(u1, u0);
		builder.addVertex(x1, y0, z1).setUv(u1, u1);
		// up
		builder.addVertex(x0, y1, z0).setUv(u0, u0);
		builder.addVertex(x0, y1, z1).setUv(u0, u1);
		builder.addVertex(x1, y1, z1).setUv(u1, u1);
		builder.addVertex(x1, y1, z0).setUv(u1, u0);
		// north
		builder.addVertex(x1, y1, z0).setUv(u0, u0);
		builder.addVertex(x1, y0, z0).setUv(u0, u1);
		builder.addVertex(x0, y0, z0).setUv(u1, u1);
		builder.addVertex(x0, y1, z0).setUv(u1, u0);
		// south
		builder.addVertex(x0, y1, z1).setUv(u0, u0);
		builder.addVertex(x0, y0, z1).setUv(u0, u1);
		builder.addVertex(x1, y0, z1).setUv(u1, u1);
		builder.addVertex(x1, y1, z1).setUv(u1, u0);
		// west
		builder.addVertex(x0, y1, z0).setUv(u0, u0);
		builder.addVertex(x0, y0, z0).setUv(u0, u1);
		builder.addVertex(x0, y0, z1).setUv(u1, u1);
		builder.addVertex(x0, y1, z1).setUv(u1, u0);
		// east
		builder.addVertex(x1, y1, z1).setUv(u0, u0);
		builder.addVertex(x1, y0, z1).setUv(u0, u1);
		builder.addVertex(x1, y0, z0).setUv(u1, u1);
		builder.addVertex(x1, y1, z0).setUv(u1, u0);
	}

	private static void renderCubes(float time, PoseStack poseStack, VertexConsumer consumer) {
		for (float[] motion : CUBE_MOTION) {
			final float theta = 360 * Mth.sin(motion[0] * time + motion[1]);
			final float phi = 360 * Mth.sin(motion[2] * time + motion[3]);
			poseStack.pushPose();
			poseStack.translate(0.5, 0.5, 0.5);
			poseStack.mulPose(Axis.YP.rotationDegrees(theta));
			poseStack.mulPose(Axis.ZP.rotationDegrees(phi));
			poseStack.translate(CUBE_RADIUS, 0, 0);
			addSolidCube(poseStack.last(), consumer, -CUBE_HALF_SIZE, CUBE_HALF_SIZE);
			poseStack.popPose();
		}
	}

	private static void addSolidCube(PoseStack.Pose pose, VertexConsumer consumer, float min, float max) {
		final float[][] faces = {
				{ min, min, max, min, min, min, max, min, min, max, min, max },
				{ min, max, min, min, max, max, max, max, max, max, max, min },
				{ max, max, min, max, min, min, min, min, min, min, max, min },
				{ min, max, max, min, min, max, max, min, max, max, max, max },
				{ min, max, min, min, min, min, min, min, max, min, max, max },
				{ max, max, max, max, min, max, max, min, min, max, max, min },
		};
		for (float[] face : faces)
			for (int i = 0; i < 12; i += 3)
				consumer.addVertex(pose, face[i], face[i + 1], face[i + 2]).setColor(CUBE_COLOR);
	}

	@Override
	public boolean shouldRenderOffScreen(GuideBlockEntity guide) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return OBClientConfig.GUIDE.renderDistance.getAsInt();
	}

	@Override
	public AABB getRenderBoundingBox(GuideBlockEntity guide) {
		return guide.getRenderBox();
	}

	/**
	 * Legacy {@code BlockGuide.onSelected}: outlines only the touch button under the cursor.
	 */
	public static void onRenderHighlight(RenderHighlightEvent.Block event) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) return;

		final BlockPos pos = event.getTarget().getBlockPos();
		final BlockState state = minecraft.level.getBlockState(pos);
		if (!(state.getBlock() instanceof GuideBlock guide)) return;

		final GuideButtons.Button button = guide.findButton(state, pos, event.getTarget().getLocation(), minecraft.player);
		if (button == null) return;

		event.setCanceled(true);
		final Vec3 camera = event.getCamera().getPosition();
		final AABB box = state.getValue(GuideBlock.ORIENTATION).localToBlock(button.box()).move(pos).move(camera.scale(-1));
		LevelRenderer.renderLineBox(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), box, 0, 0, 0, 0.4f);
	}

	/**
	 * Vertex buffers of a guide, one per shape that is drawn (two while fading).
	 */
	private static class Meshes implements AutoCloseable {

		private final Map<List<BlockPos>, VertexBuffer> buffers = new IdentityHashMap<>();

		static Meshes of(GuideBlockEntity guide) {
			if (guide.getRenderData() instanceof Meshes meshes) return meshes;
			final Meshes meshes = new Meshes();
			guide.setRenderData(meshes);
			return meshes;
		}

		void retain(List<BlockPos> shape, @Nullable List<BlockPos> previousShape) {
			buffers.entrySet().removeIf(entry -> {
				if (entry.getKey() == shape || entry.getKey() == previousShape) return false;
				if (entry.getValue() != null) entry.getValue().close();
				return true;
			});
		}

		@Nullable
		VertexBuffer get(List<BlockPos> shape) {
			return buffers.computeIfAbsent(shape, GuideRenderer::buildMesh);
		}

		@Override
		public void close() {
			buffers.values().forEach(VertexBuffer::close);
			buffers.clear();
		}
	}
}
