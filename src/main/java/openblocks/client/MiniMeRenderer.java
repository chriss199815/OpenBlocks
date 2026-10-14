package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import openblocks.entity.MiniMe;

/**
 * Port of legacy {@code EntityMiniMeRenderer}: a baby-sized player with the skin of its owner (slim or wide arms, like
 * the skin wants); arms raised while carrying someone, like legacy {@code ModelMiniMe}.
 */
public class MiniMeRenderer extends MobRenderer<MiniMe, PlayerModel<MiniMe>> {

	private final PlayerModel<MiniMe> wide;
	private final PlayerModel<MiniMe> slim;

	public MiniMeRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(ModelLayers.PLAYER), false), 0.3f);
		wide = model;
		slim = new Model(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
	}

	@Override
	public void render(MiniMe miniMe, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		model = getSkin(miniMe).model() == PlayerSkin.Model.SLIM? slim : wide;
		super.render(miniMe, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(MiniMe miniMe) {
		return getSkin(miniMe).texture();
	}

	private static PlayerSkin getSkin(MiniMe miniMe) {
		final ResolvableProfile owner = miniMe.getOwner();
		return owner != null? Minecraft.getInstance().getSkinManager().getInsecureSkin(owner.gameProfile()) : DefaultPlayerSkin.get(miniMe.getUUID());
	}

	private static class Model extends PlayerModel<MiniMe> {

		Model(ModelPart root, boolean slim) {
			super(root, slim);
		}

		@Override
		public void setupAnim(MiniMe miniMe, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			super.setupAnim(miniMe, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			if (miniMe.isVehicle()) {
				leftArm.xRot = -3.0f;
				rightArm.xRot = -3.0f;
				leftArm.zRot = 0.3f;
				rightArm.zRot = -0.3f;
				leftSleeve.copyFrom(leftArm);
				rightSleeve.copyFrom(rightArm);
			}
		}
	}
}
