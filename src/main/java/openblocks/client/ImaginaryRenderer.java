package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.block.ImaginaryBlock;
import openblocks.block.entity.ImaginaryBlockEntity;
import openblocks.item.ImaginationGlassesItem;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code TileEntityImaginaryRenderer}: draws the block model translucent and full bright, in the crayon
 * color, and fades it in or out when the local player starts or stops seeing it. Instead of the legacy vertex rewriting,
 * the quads go through {@code putBulkData} with the color and alpha.
 */
public class ImaginaryRenderer implements BlockEntityRenderer<ImaginaryBlockEntity> {

	private static final Direction[] SIDES_AND_NONE = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null };

	private final BlockRenderDispatcher blockRenderer;
	private final RandomSource random = RandomSource.create();

	public ImaginaryRenderer(BlockEntityRendererProvider.Context context) {
		this.blockRenderer = context.getBlockRenderDispatcher();
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		// tints the dyeable overlay layer of crayon glasses
		event.registerItem(new IClientItemExtensions() {
			@Override
			public int getDefaultDyeColor(ItemStack stack) {
				return 0xFF000000 | ImaginationGlassesItem.getColor(stack);
			}
		}, OBItems.CRAYON_GLASSES.get());
	}

	@Override
	public void render(ImaginaryBlockEntity imaginary, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		final Player player = Minecraft.getInstance().player;
		final boolean visible = player != null && imaginary.is(ImaginaryBlockEntity.Property.VISIBLE, player);

		// time based, legacy changed it by a fixed amount per frame
		final long now = Util.getMillis();
		final float step = imaginary.lastVisibilityUpdate == 0? 0 : (now - imaginary.lastVisibilityUpdate) / 1000f * OBClientConfig.IMAGINARY.fadingSpeed.get().floatValue();
		imaginary.lastVisibilityUpdate = now;
		imaginary.visibility = Mth.clamp(imaginary.visibility + (visible? step : -step), 0, 1);
		if (imaginary.visibility <= 0) return;

		final BlockState state = imaginary.getBlockState();
		if (!(state.getBlock() instanceof ImaginaryBlock)) return;

		final Integer color = imaginary.getColor();
		final float r = color != null? ((color >> 16) & 0xFF) / 255f : 1;
		final float g = color != null? ((color >> 8) & 0xFF) / 255f : 1;
		final float b = color != null? (color & 0xFF) / 255f : 1;

		// not culled, so panels can be seen from below
		final VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
		final PoseStack.Pose pose = poseStack.last();
		final BakedModel model = blockRenderer.getBlockModel(state);
		for (Direction side : SIDES_AND_NONE) {
			random.setSeed(42);
			for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null))
				consumer.putBulkData(pose, quad, r, g, b, imaginary.visibility, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		}
	}
}
