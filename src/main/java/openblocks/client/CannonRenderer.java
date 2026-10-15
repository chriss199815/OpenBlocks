package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import openblocks.OpenBlocks;
import openblocks.block.entity.CannonBlockEntity;
import openblocks.common.CannonBallistics;
import openblocks.item.PointerItem;
import openblocks.registry.OBBlocks;

/**
 * Port of legacy {@code TileEntityCannonRenderer}: the cannon model turned towards its target and, while the player
 * holds a pointer, the flight path of the next shot. Also draws the item.
 */
public class CannonRenderer implements BlockEntityRenderer<CannonBlockEntity> {

	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/cannon.png");
	private static final int LINE_STEPS = 200;
	private static final double LINE_DROP = 64;

	private final CannonModel model;

	public CannonRenderer(BlockEntityRendererProvider.Context context) {
		model = new CannonModel(context.bakeLayer(CannonModel.LAYER));
	}

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(CannonModel.LAYER, CannonModel::createLayer);
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private ItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new ItemRenderer();
				return renderer;
			}
		}, OBBlocks.CANNON.get().asItem());
	}

	@Override
	public void render(CannonBlockEntity cannon, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		renderModel(model, poseStack, buffers, light, overlay, cannon.getYaw(), cannon.getPitch(), cannon.getTicksSinceLastFire());

		final LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && PointerItem.isPointer(player.getMainHandItem())) renderTrajectory(cannon.getVelocity(), poseStack, buffers);
	}

	private static void renderModel(CannonModel model, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, double yaw, double pitch, int ticksSinceLastFire) {
		poseStack.pushPose();
		poseStack.translate(0.5, 1.0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees((float)(180 - yaw)));
		poseStack.mulPose(Axis.XP.rotationDegrees(180));
		model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay, pitch, ticksSinceLastFire);
		poseStack.popPose();
	}

	// same physics as the shot item, see CannonBallistics
	private static void renderTrajectory(Vec3 velocity, PoseStack poseStack, MultiBufferSource buffers) {
		final VertexConsumer consumer = buffers.getBuffer(RenderType.lineStrip());
		final PoseStack.Pose pose = poseStack.last();
		final Vec3 origin = CannonBlockEntity.ORIGIN;
		Vec3 pos = origin;
		Vec3 v = velocity;
		for (int i = 0; i < LINE_STEPS && pos.y > origin.y - LINE_DROP; i++) {
			v = v.subtract(0, CannonBallistics.GRAVITY, 0);
			final Vec3 normal = v.normalize();
			consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z).setColor(0, 0, 0, 255).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
			pos = pos.add(v);
			v = v.scale(CannonBallistics.DRAG);
		}
	}

	@Override
	public AABB getRenderBoundingBox(CannonBlockEntity cannon) {
		// the trajectory line reaches far
		return new AABB(cannon.getBlockPos()).inflate(LINE_DROP);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	private static class ItemRenderer extends BlockEntityWithoutLevelRenderer {

		private final CannonModel model;

		ItemRenderer() {
			super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
			model = new CannonModel(Minecraft.getInstance().getEntityModels().bakeLayer(CannonModel.LAYER));
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
			// legacy default aim
			renderModel(model, poseStack, buffers, light, OverlayTexture.NO_OVERLAY, 0, 45, Integer.MAX_VALUE);
		}
	}
}
