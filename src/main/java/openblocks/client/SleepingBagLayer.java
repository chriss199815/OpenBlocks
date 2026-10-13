package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import openblocks.OpenBlocks;
import openblocks.common.SleepingBagHandler;
import openblocks.registry.OBAttachments;

/**
 * Draws the sleeping bag around sleeping players; port of legacy {@code ModelSleepingBag} (formerly an armor model).
 */
public class SleepingBagLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("sleeping_bag"), "main");
	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/sleeping_bag.png");

	/**
	 * The bag reaches 4 pixels below the player's position when lying down.
	 */
	private static final double BAG_DEPTH = 4 / 16.0;

	private final ModelPart model;

	public SleepingBagLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart model) {
		super(parent);
		this.model = model;
	}

	public static LayerDefinition createLayer() {
		final MeshDefinition mesh = new MeshDefinition();
		final PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-9, 0, -3, 18, 26, 7), PartPose.ZERO);
		root.addOrReplaceChild("pillow", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(-8, -9, 0, 8, 18, 1), PartPose.offsetAndRotation(0, 0, 3, 0, 0, (float)(Math.PI / 2)));
		return LayerDefinition.create(mesh, 128, 64);
	}

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(LAYER, SleepingBagLayer::createLayer);
	}

	public static void addLayers(EntityRenderersEvent.AddLayers event) {
		final ModelPart model = event.getEntityModels().bakeLayer(LAYER);
		for (PlayerSkin.Model skin : event.getSkins()) {
			final PlayerRenderer renderer = event.getSkin(skin);
			if (renderer != null) renderer.addLayer(new SleepingBagLayer(renderer, model));
		}
	}

	/**
	 * Without a bed vanilla lays every sleeper in the same direction. This turns the body like a bed whose head end is behind the player,
	 * so they keep looking where they looked when lying down (see {@link #onComputeCameraAngles}).
	 */
	public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
		final Player player = event.getEntity();
		if (!isInBag(player)) return;

		final float yaw = player.getData(OBAttachments.SLEEPING_BAG);
		final double headYaw = Math.toRadians(yaw + 180);
		// like the vanilla bed shift in LivingEntityRenderer, so the head lies where the player stood
		final float shift = player.getEyeHeight(Pose.STANDING) - 0.1f;

		// vanilla places sleepers at bed height, but without a bed gravity still pulls them to the ground, so the height is taken from
		// the actual position: the bag's underside goes onto the floor of the block the player lay down in
		final double groundY = player.getSleepingPos().map(pos -> (double)pos.getY()).orElse(player.getY());
		final double dy = groundY + BAG_DEPTH - Mth.lerp(event.getPartialTick(), player.yo, player.getY());

		final PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(Math.sin(headYaw) * shift, dy, -Math.cos(headYaw) * shift);
		// vanilla already applied the rotation for Direction.UP (0°); a bed facing headYaw would use 90 - (yaw + 180)
		poseStack.mulPose(Axis.YP.rotationDegrees(-90 - yaw));
	}

	public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		if (event.getCamera().isDetached() || !(event.getCamera().getEntity() instanceof Player player) || !isInBag(player)) return;
		event.setYaw(player.getData(OBAttachments.SLEEPING_BAG));
		event.setPitch(0);
	}

	public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
		if (isInBag(event.getEntity())) event.getPoseStack().popPose();
	}

	private static boolean isInBag(Player player) {
		return player.isSleeping() && SleepingBagHandler.isInSleepingBag(player);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (!isInBag(player)) return;
		model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
	}
}
