package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.OpenBlocks;
import openblocks.common.HangGliderHandler;

/**
 * Port of legacy {@code EntityHangGliderRenderer} and {@code GliderPlayerRenderHandler}: draws the glider above players that carry one,
 * tilts deployed players forward and hides the glider item in their hand.
 */
public class HangGliderRenderer {

	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/hang_glider.png");
	private static final ModelResourceLocation ITEM_MODEL = ModelResourceLocation.inventory(OpenBlocks.location("hang_glider"));

	private static final float QUAD_HALF_SIZE = 2.4f;
	private static final float ATTACH_HEIGHT = 1.2f;
	private static final float BODY_TILT = 75;

	/**
	 * Tilts the body around its sideways axis before the player model is drawn; undone in {@link #onRenderPlayerPost}.
	 */
	public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
		final Player player = event.getEntity();
		if (!HangGliderHandler.isDeployed(player)) return;

		// legacy also stopped the legs from swinging
		player.walkAnimation.setSpeed(0);

		final float bodyYaw = 180 - Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
		final PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
		poseStack.mulPose(Axis.XN.rotationDegrees(BODY_TILT));
		poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
	}

	public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
		final Player player = event.getEntity();
		if (!HangGliderHandler.hasGlider(player)) return;

		final PoseStack poseStack = event.getPoseStack();
		final boolean deployed = HangGliderHandler.isDeployed(player);
		if (deployed) poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, ATTACH_HEIGHT, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot)));
		if (deployed) {
			// closer to the back and forward when flying
			poseStack.translate(0, -0.5, -1.0);
		} else {
			// folded on the back
			poseStack.translate(0, 0.2, 0.3);
			poseStack.mulPose(Axis.XP.rotationDegrees(90));
			poseStack.scale(0.4f, 1, 0.4f);
		}
		drawGlider(poseStack, event.getMultiBufferSource(), event.getPackedLight());
		poseStack.popPose();
	}

	/**
	 * In first person the player model isn't drawn, but the deployed glider is still shown above the head.
	 */
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

		final Minecraft minecraft = Minecraft.getInstance();
		final Player player = minecraft.player;
		if (player == null || !minecraft.options.getCameraType().isFirstPerson() || !HangGliderHandler.isDeployed(player)) return;

		final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		final Vec3 camera = event.getCamera().getPosition();
		final Vec3 pos = player.getPosition(partialTick);

		final PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(pos.x - camera.x, pos.y - camera.y + ATTACH_HEIGHT + 0.7, pos.z - camera.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)));

		final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		drawGlider(poseStack, buffers, dispatcher.getPackedLightCoords(player, partialTick));
		buffers.endBatch(RenderType.entityCutoutNoCull(TEXTURE));
		poseStack.popPose();
	}

	private static void drawGlider(PoseStack poseStack, MultiBufferSource buffers, int light) {
		final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		final PoseStack.Pose pose = poseStack.last();
		vertex(consumer, pose, QUAD_HALF_SIZE, QUAD_HALF_SIZE, 1, 1, light);
		vertex(consumer, pose, -QUAD_HALF_SIZE, QUAD_HALF_SIZE, 0, 1, light);
		vertex(consumer, pose, -QUAD_HALF_SIZE, -QUAD_HALF_SIZE, 0, 0, light);
		vertex(consumer, pose, QUAD_HALF_SIZE, -QUAD_HALF_SIZE, 1, 0, light);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v, int light) {
		consumer.addVertex(pose, x, 0, z)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0, 1, 0);
	}

	/**
	 * Replaces legacy {@code openmods:perspective-aware}: while the glider is on the back, the item isn't drawn in the hands.
	 */
	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		final Map<ModelResourceLocation, BakedModel> models = event.getModels();
		models.computeIfPresent(ITEM_MODEL, (location, model) -> new ItemModel(model));
	}

	private static class ItemModel extends BakedModelWrapper<BakedModel> {

		private final BakedModel hiddenInHand;

		private final ItemOverrides overrides = new ItemOverrides() {
			@Nullable
			@Override
			public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
				if (entity instanceof AbstractClientPlayer player && HangGliderHandler.isDeployedStack(player, stack)) return hiddenInHand;
				return originalModel.getOverrides().resolve(model, stack, level, entity, seed);
			}
		};

		ItemModel(BakedModel original) {
			super(original);
			this.hiddenInHand = new HiddenInHandModel(original);
		}

		@Override
		public ItemOverrides getOverrides() {
			return overrides;
		}
	}

	private static class HiddenInHandModel extends BakedModelWrapper<BakedModel> {

		private final BakedModel empty;

		HiddenInHandModel(BakedModel original) {
			super(original);
			this.empty = new BakedModelWrapper<>(original) {
				@Override
				public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
					return List.of();
				}

				@Override
				public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
					return List.of();
				}
			};
		}

		@Override
		public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
			super.applyTransform(context, poseStack, applyLeftHandTransform);
			final boolean inHand = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
			// GUI, ground and frames still show the item
			return inHand? empty : originalModel;
		}
	}
}
