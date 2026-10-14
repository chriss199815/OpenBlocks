package openblocks.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.item.DevNullItem;
import openblocks.registry.OBItems;

/**
 * Replaces legacy {@code DevNullItemOverride}/{@code DevNullModel}: draws one frame per nesting level, each smaller by
 * {@link #SCALE}, with the contained item inside. In the GUI the frames are the flat item and the contained item keeps its
 * GUI transform; elsewhere the frames are inside-out cubes and everything uses the contained item's transform.
 */
public class DevNullItemRenderer extends BlockEntityWithoutLevelRenderer {

	public static final ModelResourceLocation FRAME_2D = ModelResourceLocation.standalone(OpenBlocks.location("item/dev_null_frame"));
	public static final ModelResourceLocation OVERFLOW_2D = ModelResourceLocation.standalone(OpenBlocks.location("item/dev_null_overflow"));
	public static final ModelResourceLocation FRAME_3D = ModelResourceLocation.standalone(OpenBlocks.location("block/dev_null_3d"));
	public static final ModelResourceLocation OVERFLOW_3D = ModelResourceLocation.standalone(OpenBlocks.location("block/dev_null_3d_overflow"));

	private static final float SCALE = 0.875f;

	private static final RandomSource RANDOM = RandomSource.create();

	public DevNullItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(FRAME_2D);
		event.register(OVERFLOW_2D);
		event.register(FRAME_3D);
		event.register(OVERFLOW_3D);
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private DevNullItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new DevNullItemRenderer();
				return renderer;
			}
		}, OBItems.DEV_NULL.get());
	}

	/**
	 * The size of the contained stack, drawn like the vanilla stack size (legacy baked it into the GUI model).
	 */
	public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
		event.register(OBItems.DEV_NULL.get(), (graphics, font, stack, x, y) -> {
			final int count = DevNullItem.getContents(stack).stack().getCount();
			if (count <= 1) return false;

			final String text = Integer.toString(count);
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 200);
			graphics.drawString(font, text, x + 19 - 2 - font.width(text), y + 6 + 3, 0xFF000000 | OBConfig.DEV_NULL.countColor.get(), true);
			graphics.pose().popPose();
			return false;
		});
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		final Minecraft minecraft = Minecraft.getInstance();
		final ItemRenderer itemRenderer = minecraft.getItemRenderer();

		final DevNullItem.Contents contents = DevNullItem.getContents(stack);
		final boolean overflow = contents.depth() >= DevNullItem.STACK_LIMIT;
		final int frames = overflow? DevNullItem.STACK_LIMIT : Math.max(1, contents.depth());
		final ItemStack inner = contents.stack();
		@Nullable
		final BakedModel innerModel = inner.isEmpty()? null : itemRenderer.getModel(inner, minecraft.level, null, 0);
		final float innerScale = (float)Math.pow(SCALE, frames);

		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);

		if (displayContext == ItemDisplayContext.GUI) {
			renderFrames(itemRenderer, FRAME_2D, OVERFLOW_2D, frames, overflow, true, poseStack, buffers, light, overlay);
			if (innerModel != null) {
				poseStack.scale(innerScale, innerScale, 1);
				// the frames are flat items, the contained item may need block lighting
				final boolean blockLight = innerModel.usesBlockLight() && buffers instanceof MultiBufferSource.BufferSource;
				if (blockLight) {
					((MultiBufferSource.BufferSource)buffers).endBatch();
					Lighting.setupFor3DItems();
				}
				itemRenderer.render(inner, ItemDisplayContext.GUI, false, poseStack, buffers, light, overlay, innerModel);
				if (blockLight) {
					((MultiBufferSource.BufferSource)buffers).endBatch();
					Lighting.setupForFlatItems();
				}
			}
		} else {
			final boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
			final BakedModel transformSource = innerModel != null? innerModel : minecraft.getModelManager().getModel(FRAME_3D);
			final BakedModel transformed = transformSource.applyTransform(displayContext, poseStack, leftHand);
			renderFrames(itemRenderer, FRAME_3D, OVERFLOW_3D, frames, overflow, false, poseStack, buffers, light, overlay);
			if (innerModel != null) {
				poseStack.scale(innerScale, innerScale, innerScale);
				// already transformed, NONE keeps the model as it is
				itemRenderer.render(inner, ItemDisplayContext.NONE, false, poseStack, buffers, light, overlay, transformed);
			}
		}

		poseStack.popPose();
	}

	private static void renderFrames(ItemRenderer itemRenderer, ModelResourceLocation frame, ModelResourceLocation overflowFrame, int frames, boolean overflow, boolean flat,
			PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		final BakedModel frameModel = Minecraft.getInstance().getModelManager().getModel(frame);
		float scale = 1;
		for (int i = 0; i < frames; i++) {
			renderFrame(itemRenderer, frameModel, scale, flat, poseStack, buffers, light, overlay);
			scale *= SCALE;
		}
		if (overflow) renderFrame(itemRenderer, Minecraft.getInstance().getModelManager().getModel(overflowFrame), scale, flat, poseStack, buffers, light, overlay);
	}

	/**
	 * Flat frames only show their front, like legacy, so they don't hide the contained item.
	 */
	private static void renderFrame(ItemRenderer itemRenderer, BakedModel model, float scale, boolean flat, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		final List<BakedQuad> quads = new ArrayList<>();
		for (Direction side : Direction.values()) {
			RANDOM.setSeed(42);
			quads.addAll(model.getQuads(null, side, RANDOM));
		}
		RANDOM.setSeed(42);
		quads.addAll(model.getQuads(null, null, RANDOM));
		if (flat) quads.removeIf(quad -> quad.getDirection() != Direction.SOUTH);

		poseStack.pushPose();
		poseStack.scale(scale, scale, flat? 1 : scale);
		poseStack.translate(-0.5f, -0.5f, -0.5f);
		itemRenderer.renderQuadList(poseStack, buffers.getBuffer(Sheets.translucentItemSheet()), quads, ItemStack.EMPTY, light, overlay);
		poseStack.popPose();
	}
}
