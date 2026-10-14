package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.block.entity.GoldenEggBlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Port of legacy {@code TileEntityGoldenEggRenderer}: the spinning egg, and while floating a glowing, growing phantom egg
 * with light rays like the dying ender dragon (legacy drew the same rays with GL calls).
 */
public class GoldenEggRenderer implements BlockEntityRenderer<GoldenEggBlockEntity> {

	private static final float PHANTOM_SCALE = 1.5f;
	private static final float HALF_SQRT_3 = (float)(Math.sqrt(3) / 2);
	private static final Vector3f STAR_AXIS = new Vector3f(0.5f, 1, 0.5f).normalize();
	private static final RandomSource RANDOM = RandomSource.create();

	public GoldenEggRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(GoldenEggBlockEntity egg, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		final BlockState state = egg.getBlockState();
		final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);

		final float rotation = egg.getRotation(partialTick);
		final float progress = egg.getProgress(partialTick);

		poseStack.pushPose();
		poseStack.translate(0.5, egg.getOffset(partialTick), 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

		poseStack.pushPose();
		poseStack.translate(-0.5, 0, -0.5);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(Sheets.solidBlockSheet()), state, model,
				1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.solidBlockSheet());
		poseStack.popPose();

		if (egg.getStage().specialEffects) {
			// like legacy, the rays are drawn inside the transformation of the phantom
			renderPhantom(state, model, progress, poseStack, buffers);
			renderStar(rotation, progress, poseStack, buffers);
		}

		poseStack.popPose();
	}

	private static void renderPhantom(BlockState state, BakedModel model, float progress, PoseStack poseStack, MultiBufferSource buffers) {
		final float scale = PHANTOM_SCALE * (0.2f + progress * 0.8f);
		poseStack.translate(0, -0.1f * progress, 0);
		poseStack.scale(scale, scale, scale);

		// additive, so the alpha of legacy becomes the brightness
		final float brightness = Mth.clamp(0.2f + 0.8f * progress, 0, 1);
		final VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS));
		poseStack.pushPose();
		poseStack.translate(-0.5, 0, -0.5);
		RANDOM.setSeed(42);
		for (Direction side : Direction.values())
			for (BakedQuad quad : model.getQuads(state, side, RANDOM, ModelData.EMPTY, null))
				consumer.putBulkData(poseStack.last(), quad, brightness, brightness, brightness, 1.0f, 0xF000F0, OverlayTexture.NO_OVERLAY);
		for (BakedQuad quad : model.getQuads(state, null, RANDOM, ModelData.EMPTY, null))
			consumer.putBulkData(poseStack.last(), quad, brightness, brightness, brightness, 1.0f, 0xF000F0, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	private static void renderStar(float rotation, float progress, PoseStack poseStack, MultiBufferSource buffers) {
		poseStack.translate(0, 0.5f, 0);
		// opposite direction at 20% speed
		poseStack.mulPose(new Quaternionf().rotationAxis((rotation * -0.2f % 360) * Mth.DEG_TO_RAD, STAR_AXIS));

		renderRays(progress, poseStack, buffers.getBuffer(RenderType.dragonRays()));
		renderRays(progress, poseStack, buffers.getBuffer(RenderType.dragonRaysDepth()));
	}

	// like EnderDragonRenderer.renderRays, with the shorter rays and lower opacity of legacy
	private static void renderRays(float progress, PoseStack poseStack, VertexConsumer consumer) {
		poseStack.pushPose();
		final float fade = progress > 0.8f? Math.min((progress - 0.8f) / 0.2f, 1.0f) : 0.0f;
		final int center = FastColor.ARGB32.colorFromFloat(0.75f * (1.0f - fade), 1.0f, 1.0f, 1.0f);
		final int edge = 0x00FF00FF;
		final RandomSource random = RandomSource.create(432L);
		final Vector3f origin = new Vector3f();
		final Vector3f a = new Vector3f();
		final Vector3f b = new Vector3f();
		final Vector3f c = new Vector3f();
		final Quaternionf rotation = new Quaternionf();
		final int count = Mth.floor((progress + progress * progress) / 2.0f * 60.0f);

		for (int i = 0; i < count; i++) {
			rotation.rotationXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI)
					.rotateXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI + progress * Mth.HALF_PI);
			poseStack.mulPose(rotation);
			final float length = random.nextFloat() * 10.0f + 5.0f + fade * 10.0f;
			final float width = random.nextFloat() * 2.0f + 1.0f + fade * 2.0f;
			a.set(-HALF_SQRT_3 * width, length, -0.5f * width);
			b.set(HALF_SQRT_3 * width, length, -0.5f * width);
			c.set(0.0f, length, width);
			final PoseStack.Pose pose = poseStack.last();
			consumer.addVertex(pose, origin).setColor(center);
			consumer.addVertex(pose, a).setColor(edge);
			consumer.addVertex(pose, b).setColor(edge);
			consumer.addVertex(pose, origin).setColor(center);
			consumer.addVertex(pose, b).setColor(edge);
			consumer.addVertex(pose, c).setColor(edge);
			consumer.addVertex(pose, origin).setColor(center);
			consumer.addVertex(pose, c).setColor(edge);
			consumer.addVertex(pose, a).setColor(edge);
		}

		poseStack.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(GoldenEggBlockEntity egg) {
		// the egg floats up and the rays reach far
		return new AABB(egg.getBlockPos()).inflate(32);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
