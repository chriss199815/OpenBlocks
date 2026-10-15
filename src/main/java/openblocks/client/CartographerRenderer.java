package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import openblocks.OpenBlocks;
import openblocks.entity.Cartographer;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code EntityCartographerRenderer}: the base with an ender eye looking around. While the player looks at
 * it, a flickering holographic cone towards the player shows the center of a new map or, while mapping, the progress
 * (legacy {@code EntitySelectionHandler}).
 */
public class CartographerRenderer extends EntityRenderer<Cartographer> {

	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/cartographer.png");
	private static final ItemStack EYE = new ItemStack(Items.ENDER_EYE);
	private static final float EYE_SCALE = 0.25f;
	private static final float EYE_HEIGHT = 0.25f;

	private static final float Z_FIGHTER = 0.0001f;
	private static final float CONE_START = 0.075f;
	private static final float CONE_END = 0.3f;
	private static final float BASE_SIZE = 0.125f;
	private static final float MAP_SIZE = 0.08f;
	private static final float INTERSECTION_DIST = 2.5f / 16;
	private static final float INTERSECTION_SIZE = BASE_SIZE * (INTERSECTION_DIST - CONE_START) / (CONE_END - CONE_START);
	private static final int CONE_COLOR = 0x2000FFFF;
	private static final int DONE_COLOR = 0xFF00FF19;
	private static final int TODO_COLOR = 0xFF00600B;
	private static final float FLICKER_CHANCE = 0.1f;

	private final RandomSource random = RandomSource.create();
	private final CartographerModel model;
	private final ItemRenderer itemRenderer;
	private final Font font;

	public CartographerRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new CartographerModel(context.bakeLayer(CartographerModel.LAYER));
		itemRenderer = context.getItemRenderer();
		font = context.getFont();
	}

	/**
	 * Outlines cartographers while the player holds a map, so it is clear where to put it.
	 */
	public static boolean isHighlighted(Cartographer cartographer) {
		final Player player = Minecraft.getInstance().player;
		if (player == null) return false;
		for (InteractionHand hand : InteractionHand.values()) {
			final ItemStack stack = player.getItemInHand(hand);
			if (stack.is(OBItems.EMPTY_MAP) || stack.is(OBItems.HEIGHT_MAP)) return true;
		}
		return false;
	}

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(CartographerModel.LAYER, CartographerModel::createLayer);
	}

	@Override
	public void render(Cartographer cartographer, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		final float eyeYaw = Mth.lerp(partialTick, cartographer.prevEyeYaw, cartographer.eyeYaw);
		final float eyePitch = Mth.lerp(partialTick, cartographer.prevEyePitch, cartographer.eyePitch);

		model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, eyeYaw);

		poseStack.pushPose();
		poseStack.translate(0, EYE_HEIGHT, 0);
		poseStack.mulPose(Axis.YP.rotation(eyeYaw + Mth.HALF_PI));
		poseStack.mulPose(Axis.XP.rotation(eyePitch));
		poseStack.scale(EYE_SCALE, EYE_SCALE, EYE_SCALE);
		itemRenderer.renderStatic(EYE, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffers, cartographer.level(), cartographer.getId());
		poseStack.popPose();

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() == cartographer && minecraft.player != null && random.nextFloat() >= FLICKER_CHANCE)
			renderSelection(cartographer, poseStack, buffers);

		super.render(cartographer, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private void renderSelection(Cartographer cartographer, PoseStack poseStack, MultiBufferSource buffers) {
		poseStack.pushPose();
		// the cone points at the player
		poseStack.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().player.getDirection().toYRot()));
		poseStack.translate(0, -0.03, 0);

		final PoseStack.Pose pose = poseStack.last();
		final VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
		quad(quads, pose, -INTERSECTION_SIZE, -INTERSECTION_SIZE, INTERSECTION_SIZE, INTERSECTION_SIZE, -INTERSECTION_DIST - Z_FIGHTER, 0xFFFFFFFF);
		coneSide(quads, pose, -BASE_SIZE, -BASE_SIZE, +BASE_SIZE, -BASE_SIZE);
		coneSide(quads, pose, +BASE_SIZE, -BASE_SIZE, +BASE_SIZE, +BASE_SIZE);
		coneSide(quads, pose, +BASE_SIZE, +BASE_SIZE, -BASE_SIZE, +BASE_SIZE);
		coneSide(quads, pose, -BASE_SIZE, +BASE_SIZE, -BASE_SIZE, -BASE_SIZE);

		final Cartographer.Progress progress = cartographer.getProgress();
		if (cartographer.isMapping() && progress.chunksPerSide() > 0) renderProgress(quads, pose, progress);
		else renderCenter(cartographer, poseStack, buffers);
		poseStack.popPose();
	}

	private static void coneSide(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1) {
		consumer.addVertex(pose, 0, 0, -CONE_START).setColor(CONE_COLOR);
		consumer.addVertex(pose, 0, 0, -CONE_START).setColor(CONE_COLOR);
		consumer.addVertex(pose, x0, y0, -CONE_END).setColor(CONE_COLOR);
		consumer.addVertex(pose, x1, y1, -CONE_END).setColor(CONE_COLOR);
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z, int color) {
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
	}

	// cells from the viewer's top left, like legacy
	private static void renderProgress(VertexConsumer consumer, PoseStack.Pose pose, Cartographer.Progress progress) {
		final int side = progress.chunksPerSide();
		final float cell = 2 * MAP_SIZE / side;
		for (int row = 0; row < side; row++)
			for (int column = 0; column < side; column++) {
				final float x = MAP_SIZE - column * cell;
				final float y = MAP_SIZE - row * cell;
				quad(consumer, pose, x - cell, y - cell, x, y, -CONE_END - Z_FIGHTER, progress.finished().get(row * side + column)? DONE_COLOR : TODO_COLOR);
			}
	}

	// the panel from the texture, with the block the new map would be centered on
	private void renderCenter(Cartographer cartographer, PoseStack poseStack, MultiBufferSource buffers) {
		final PoseStack.Pose pose = poseStack.last();
		final VertexConsumer panel = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		panelVertex(panel, pose, -BASE_SIZE, -BASE_SIZE, 0.0f, 1.0f);
		panelVertex(panel, pose, +BASE_SIZE, -BASE_SIZE, 0.5f, 1.0f);
		panelVertex(panel, pose, +BASE_SIZE, +BASE_SIZE, 0.5f, 0.5f);
		panelVertex(panel, pose, -BASE_SIZE, +BASE_SIZE, 0.0f, 0.5f);

		final String coords = String.format("%d,%d", cartographer.getNewMapCenterX(), cartographer.getNewMapCenterZ());
		final float available = 2 * BASE_SIZE * 12 / 16;
		final float scale = Math.min(available / font.width(coords), BASE_SIZE / 2 / font.lineHeight);
		poseStack.pushPose();
		poseStack.translate(0, BASE_SIZE * 3 / 4, -CONE_END - Z_FIGHTER);
		// seen from the player, who looks along +z here
		poseStack.scale(-scale, -scale, 1);
		font.drawInBatch(coords, -font.width(coords) / 2.0f, -font.lineHeight / 2.0f, 0xFF000000, false, poseStack.last().pose(), buffers,
				Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
		poseStack.popPose();
	}

	private static void panelVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
		consumer.addVertex(pose, x, y, -CONE_END).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
	}

	@Override
	public ResourceLocation getTextureLocation(Cartographer cartographer) {
		return TEXTURE;
	}
}
