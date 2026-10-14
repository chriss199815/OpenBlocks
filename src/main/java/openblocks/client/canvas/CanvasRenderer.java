package openblocks.client.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.common.CanvasSide;
import org.joml.Vector3f;

/**
 * Replaces the legacy {@code StencilModelTransformer} and {@code StencilCoverQuadBuilder}: draws the paint over every face of the block
 * that points to a painted side, projected like legacy {@code StencilTextureProjection}, and the stencils lying on the sides.
 */
public class CanvasRenderer implements BlockEntityRenderer<CanvasBlockEntity> {

	// lifts paint off the block faces to avoid z-fighting
	private static final float PAINT_OFFSET = 0.002f;
	// legacy COVER_DELTA
	private static final double COVER_OFFSET = 0.01;

	private static final int VERTEX_SIZE = 8;

	private final RandomSource random = RandomSource.create();

	public CanvasRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(CanvasBlockEntity canvas, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final Level level = canvas.getLevel();
		if (level == null) return;
		final BlockPos pos = canvas.getBlockPos();
		final BlockState painted = canvas.getPaintedBlock();
		final BlockState shown = painted != null? painted : canvas.getBlockState();

		boolean hasPaint = false;
		for (Direction side : Direction.values())
			hasPaint |= canvas.getSide(side).hasPaint();
		if (hasPaint) renderPaint(canvas, shown, level, pos, poseStack, bufferSource);

		final VoxelShape shape = shown.getShape(level, pos);
		final AABB bounds = (shape.isEmpty()? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds()).inflate(COVER_OFFSET);
		for (Direction side : Direction.values()) {
			final CanvasSide paint = canvas.getSide(side);
			if (paint.cover().isEmpty()) continue;
			final VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(CanvasTextures.cover(paint.cover().get())));
			renderCover(consumer, poseStack.last(), bounds, side, LevelRenderer.getLightColor(level, pos.relative(side)));
		}
	}

	private void renderPaint(CanvasBlockEntity canvas, BlockState shown, Level level, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource) {
		final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(shown);
		final ResourceLocation[] textures = new ResourceLocation[6];
		for (Direction side : Direction.values()) {
			final CanvasSide paint = canvas.getSide(side);
			if (paint.hasPaint()) textures[side.get3DDataValue()] = CanvasTextures.paint(paint);
		}

		for (Direction cullFace : new Direction[] { null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
			if (cullFace != null && !Block.shouldRenderFace(shown, level, pos, cullFace, pos.relative(cullFace))) continue;
			random.setSeed(42);
			for (BakedQuad quad : model.getQuads(shown, cullFace, random, ModelData.EMPTY, null)) {
				final Direction face = quad.getDirection();
				final ResourceLocation texture = textures[face.get3DDataValue()];
				if (texture == null) continue;
				final VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
				renderPaintedQuad(consumer, poseStack.last(), quad, face, LevelRenderer.getLightColor(level, pos.relative(face)));
			}
		}
	}

	private static void renderPaintedQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, Direction face, int light) {
		final int[] vertices = quad.getVertices();
		final Vector3f normal = face.step();
		for (int i = 0; i < 4; i++) {
			final float x = Float.intBitsToFloat(vertices[i * VERTEX_SIZE]) + normal.x() * PAINT_OFFSET;
			final float y = Float.intBitsToFloat(vertices[i * VERTEX_SIZE + 1]) + normal.y() * PAINT_OFFSET;
			final float z = Float.intBitsToFloat(vertices[i * VERTEX_SIZE + 2]) + normal.z() * PAINT_OFFSET;
			vertex(consumer, pose, face, x, y, z, light);
		}
	}

	private static void renderCover(VertexConsumer consumer, PoseStack.Pose pose, AABB bounds, Direction side, int light) {
		final float minX = (float)bounds.minX, minY = (float)bounds.minY, minZ = (float)bounds.minZ;
		final float maxX = (float)bounds.maxX, maxY = (float)bounds.maxY, maxZ = (float)bounds.maxZ;
		// counter-clockwise seen from outside
		switch (side) {
			case DOWN -> {
				vertex(consumer, pose, side, minX, minY, minZ, light);
				vertex(consumer, pose, side, maxX, minY, minZ, light);
				vertex(consumer, pose, side, maxX, minY, maxZ, light);
				vertex(consumer, pose, side, minX, minY, maxZ, light);
			}
			case UP -> {
				vertex(consumer, pose, side, minX, maxY, minZ, light);
				vertex(consumer, pose, side, minX, maxY, maxZ, light);
				vertex(consumer, pose, side, maxX, maxY, maxZ, light);
				vertex(consumer, pose, side, maxX, maxY, minZ, light);
			}
			case NORTH -> {
				vertex(consumer, pose, side, maxX, maxY, minZ, light);
				vertex(consumer, pose, side, maxX, minY, minZ, light);
				vertex(consumer, pose, side, minX, minY, minZ, light);
				vertex(consumer, pose, side, minX, maxY, minZ, light);
			}
			case SOUTH -> {
				vertex(consumer, pose, side, minX, maxY, maxZ, light);
				vertex(consumer, pose, side, minX, minY, maxZ, light);
				vertex(consumer, pose, side, maxX, minY, maxZ, light);
				vertex(consumer, pose, side, maxX, maxY, maxZ, light);
			}
			case WEST -> {
				vertex(consumer, pose, side, minX, maxY, minZ, light);
				vertex(consumer, pose, side, minX, minY, minZ, light);
				vertex(consumer, pose, side, minX, minY, maxZ, light);
				vertex(consumer, pose, side, minX, maxY, maxZ, light);
			}
			case EAST -> {
				vertex(consumer, pose, side, maxX, maxY, maxZ, light);
				vertex(consumer, pose, side, maxX, minY, maxZ, light);
				vertex(consumer, pose, side, maxX, minY, minZ, light);
				vertex(consumer, pose, side, maxX, maxY, minZ, light);
			}
		}
	}

	/**
	 * Texture coordinates follow the position on the side, like legacy {@code StencilTextureProjection}, so paint and stencils line up.
	 */
	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Direction side, float x, float y, float z, int light) {
		final float u, v;
		switch (side) {
			case NORTH -> { u = 1 - x; v = 1 - y; }
			case SOUTH -> { u = x; v = 1 - y; }
			case EAST -> { u = 1 - z; v = 1 - y; }
			case WEST -> { u = z; v = 1 - y; }
			case UP -> { u = x; v = z; }
			default -> { u = x; v = 1 - z; }
		}
		consumer.addVertex(pose, x, y, z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, side.getStepX(), side.getStepY(), side.getStepZ());
	}
}
