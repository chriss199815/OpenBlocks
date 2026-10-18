package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import openblocks.OpenBlocks;
import openblocks.entity.Magnet;

/**
 * Port of legacy {@code EntityMagnetRenderer}: three stacked plates at the top of the magnet's bounding box (which also
 * holds the carried entity). Drawn without the usual entity flip and at twice the model scale, like legacy.
 */
public class MagnetRenderer extends EntityRenderer<Magnet> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.location("magnet"), "main");
	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/magnet.png");

	private final ModelPart model;

	public MagnetRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = context.bakeLayer(LAYER);
	}

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(LAYER, MagnetRenderer::createLayer);
	}

	private static LayerDefinition createLayer() {
		final MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("magnet", CubeListBuilder.create().mirror()
				.texOffs(0, 0).addBox(-3, 0, -3, 6, 1, 6)
				.texOffs(0, 7).addBox(-2, 1, -2, 4, 1, 4)
				.texOffs(0, 12).addBox(-1, 2, -1, 2, 1, 2), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void render(Magnet magnet, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		poseStack.pushPose();
		poseStack.translate(0, magnet.getBbHeight() - 0.4, 0);
		poseStack.scale(2, 2, 2);
		model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
		super.render(magnet, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(Magnet magnet) {
		return TEXTURE;
	}
}
