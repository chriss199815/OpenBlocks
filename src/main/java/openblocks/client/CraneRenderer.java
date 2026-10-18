package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import openblocks.OpenBlocks;
import openblocks.common.CraneState;
import openblocks.entity.Magnet;
import openblocks.item.CraneBackpackItem;
import openblocks.registry.OBItems;

/**
 * Client side of the crane backpack (legacy {@code ModelCraneBackpack} event handlers): the armor model, the arm in first
 * person view and the dashed yellow and black line from the arm to the magnet. Legacy drew it with line stipples, here
 * it is made of short segments.
 */
public class CraneRenderer {

	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/crane.png");

	private static final double DASH_LENGTH = 0.1;
	private static final int YELLOW = 0xFFFFFF00;
	private static final int BLACK = 0xFF000000;

	private static CraneBackpackModel model;

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(CraneBackpackModel.LAYER, CraneBackpackModel::createLayer);
	}

	public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
		model = new CraneBackpackModel(event.getEntityModels().bakeLayer(CraneBackpackModel.LAYER));
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			@Override
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
				return model;
			}
		}, OBItems.CRANE_BACKPACK.get());
	}

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) return;

		final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		final Vec3 camera = event.getCamera().getPosition();
		final PoseStack poseStack = event.getPoseStack();
		final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

		for (Player player : minecraft.level.players()) {
			if (!CraneBackpackItem.isWearing(player)) continue;
			final boolean firstPerson = player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
			if (firstPerson) renderFirstPersonArm(player, partialTick, camera, poseStack, buffers);

			final Magnet magnet = CraneState.of(player).getMagnet();
			if (magnet == null) continue;
			final Vec3 armEnd = firstPerson? getFirstPersonArmEnd(player, partialTick) : getArmEnd(player, partialTick);
			final Vec3 magnetTop = magnet.getPosition(partialTick).add(0, magnet.getBbHeight() - (firstPerson? 0.05 : 0.1), 0);
			renderLine(poseStack, buffers.getBuffer(RenderType.lines()), armEnd.subtract(camera), magnetTop.subtract(camera));
		}
		buffers.endBatch(RenderType.entityCutoutNoCull(TEXTURE));
		buffers.endBatch(RenderType.lines());
	}

	private static Vec3 forward(float yawDegrees) {
		final float yaw = yawDegrees * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	// legacy values, roughly the end of the arm of the model
	private static Vec3 getArmEnd(Player player, float partialTick) {
		final Vec3 head = forward(Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot));
		Vec3 result = player.getPosition(partialTick).add(0, player.getEyeHeight(), 0);
		if (player.isCrouching()) return result.add(0, 0.70, 0).add(head.scale(2));
		final Vec3 body = forward(Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot));
		return result.add(0, 0.65, 0).add(body.scale(-0.45)).add(head.scale(2.4));
	}

	private static Vec3 getFirstPersonArmEnd(Player player, float partialTick) {
		return player.getPosition(partialTick).add(0, player.getEyeHeight() + 0.6, 0).add(forward(player.getViewYRot(partialTick)).scale(1.9));
	}

	private static void renderFirstPersonArm(Player player, float partialTick, Vec3 camera, PoseStack poseStack, MultiBufferSource buffers) {
		if (model == null) return;
		final Vec3 pos = player.getPosition(partialTick);
		poseStack.pushPose();
		poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(-player.getViewYRot(partialTick)));
		// legacy values, roughly where the arm is in third person view
		poseStack.translate(0, player.getEyeHeight() + 1.6, -1);
		final int light = Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(player, partialTick);
		model.renderArm(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	private static void renderLine(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to) {
		final Vec3 delta = to.subtract(from);
		final double length = delta.length();
		if (length < 1e-3) return;
		final Vec3 normal = delta.scale(1 / length);
		final PoseStack.Pose pose = poseStack.last();
		final int dashes = Math.max(1, (int)Math.ceil(length / DASH_LENGTH));
		for (int i = 0; i < dashes; i++) {
			final Vec3 start = from.add(delta.scale((double)i / dashes));
			final Vec3 end = from.add(delta.scale((double)(i + 1) / dashes));
			final int color = i % 2 == 0? YELLOW : BLACK;
			consumer.addVertex(pose, (float)start.x, (float)start.y, (float)start.z).setColor(color).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
			consumer.addVertex(pose, (float)end.x, (float)end.y, (float)end.z).setColor(color).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
		}
	}
}
