package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import openblocks.OpenBlocks;
import openblocks.entity.Glyph;
import openblocks.item.GlyphItem;
import openblocks.registry.OBItems;
import org.joml.Matrix4f;

/**
 * Replaces legacy {@code ModelGlyph}, {@code EntityGlyphRenderer} and {@code GlyphPlacementGridRenderer}. The glyph item
 * is the plate from the legacy model with the character drawn on it with the game font (legacy cut it out of the font
 * sheet); a glyph on a wall is the item at half size. While holding a glyph, the pixel grid of the looked at wall and
 * where the glyph would go are shown.
 */
public class GlyphRenderer extends EntityRenderer<Glyph> {

	public static final ModelResourceLocation BASE = ModelResourceLocation.standalone(OpenBlocks.location("item/glyph_base"));

	// legacy: the character cell covers pixels 1 to 15 of the plate, slightly in front of it
	private static final float CELL_SIZE = 14 / 16f;
	private static final float LETTER_Z = 7.45f / 16;
	private static final int FONT_CELL = 8;

	private static final int GRID_COLOR = 0x80808080;
	private static final float GRID_OFFSET = 0.002f;

	private static final RandomSource RANDOM = RandomSource.create();

	private final ItemRenderer itemRenderer;

	public GlyphRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemRenderer = context.getItemRenderer();
	}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		event.register(BASE);
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private ItemRendererImpl renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new ItemRendererImpl();
				return renderer;
			}
		}, OBItems.GLYPH.get());
	}

	@Override
	public void render(Glyph glyph, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180 - glyph.getYRot()));
		poseStack.scale(0.5f, 0.5f, 0.5f);
		itemRenderer.renderStatic(glyph.getStack(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffers, glyph.level(), glyph.getId());
		poseStack.popPose();
		super.render(glyph, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ResourceLocation getTextureLocation(Glyph glyph) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

	private static class ItemRendererImpl extends BlockEntityWithoutLevelRenderer {

		ItemRendererImpl() {
			super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
			final Minecraft minecraft = Minecraft.getInstance();
			final BakedModel base = minecraft.getModelManager().getModel(BASE);
			final List<BakedQuad> quads = new ArrayList<>();
			for (Direction side : Direction.values()) {
				RANDOM.setSeed(42);
				quads.addAll(base.getQuads(null, side, RANDOM));
			}
			RANDOM.setSeed(42);
			quads.addAll(base.getQuads(null, null, RANDOM));
			minecraft.getItemRenderer().renderQuadList(poseStack, buffers.getBuffer(Sheets.translucentItemSheet()), quads, stack, light, overlay);

			final Font font = minecraft.font;
			final String text = String.valueOf(GlyphItem.getChar(stack));
			final float scale = CELL_SIZE / FONT_CELL;
			poseStack.pushPose();
			// on the north side, read from the north: font x goes west, font y goes down
			poseStack.translate(0.5f, 0.5f, LETTER_Z);
			poseStack.scale(-scale, -scale, scale);
			font.drawInBatch(text, -(font.width(text) - 1) / 2f, -FONT_CELL / 2f, 0xFF000000, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
			poseStack.popPose();
		}
	}

	/**
	 * The pixel grid of the face and the outline of the glyph that would be placed (legacy {@code GlyphPlacementGridRenderer}).
	 */
	public static void onRenderHighlight(RenderHighlightEvent.Block event) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !minecraft.player.getMainHandItem().is(OBItems.GLYPH)) return;
		final BlockHitResult hit = event.getTarget();
		final Direction side = hit.getDirection();
		if (side.getAxis().isVertical()) return;

		final BlockPos pos = hit.getBlockPos();
		final Vec3 camera = event.getCamera().getPosition();
		final Direction right = side.getCounterClockWise();
		// the face, seen from the front: origin at its bottom left corner, slightly in front of it
		final Vec3 origin = Vec3.atCenterOf(pos)
				.relative(side, 0.5 + GRID_OFFSET)
				.relative(right, -0.5)
				.add(-camera.x, -0.5 - camera.y, -camera.z);
		final Vec3 rightStep = Vec3.atLowerCornerOf(right.getNormal());
		final Vec3 up = new Vec3(0, 1, 0);

		final PoseStack poseStack = event.getPoseStack();
		final Matrix4f pose = poseStack.last().pose();
		final VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
		final Vec3 normal = Vec3.atLowerCornerOf(side.getNormal());
		for (int i = 0; i <= 16; i++) {
			line(lines, poseStack, origin.add(rightStep.scale(i / 16.0)), origin.add(rightStep.scale(i / 16.0)).add(up), up);
			line(lines, poseStack, origin.add(up.scale(i / 16.0)), origin.add(up.scale(i / 16.0)).add(rightStep), rightStep);
		}

		final Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		final double along = right.getAxis().choose(local.x, local.y, local.z);
		final double u = right.getAxisDirection() == Direction.AxisDirection.POSITIVE? along : 1 - along;
		final int pixelU = Math.min(15, Math.max(0, (int)Math.floor(u * 16)));
		final int pixelV = Math.min(15, Math.max(0, (int)Math.floor(local.y * 16)));
		final double half = Glyph.SIZE_PIXELS / 2;
		final Vec3 corner = origin.add(rightStep.scale((pixelU - half) / 16)).add(up.scale((pixelV - half) / 16)).add(normal.scale(GRID_OFFSET));
		final double size = Glyph.SIZE_PIXELS / 16.0;
		final VertexConsumer quads = event.getMultiBufferSource().getBuffer(RenderType.debugQuads());
		final Vec3 a = corner;
		final Vec3 b = corner.add(rightStep.scale(size));
		final Vec3 c = b.add(up.scale(size));
		final Vec3 d = corner.add(up.scale(size));
		// both windings, so it is visible from any side
		for (Vec3 vertex : new Vec3[] { a, b, c, d, a, d, c, b })
			quads.addVertex(pose, (float)vertex.x, (float)vertex.y, (float)vertex.z).setColor(GRID_COLOR);
	}

	private static void line(VertexConsumer consumer, PoseStack poseStack, Vec3 from, Vec3 to, Vec3 normal) {
		final PoseStack.Pose pose = poseStack.last();
		consumer.addVertex(pose, (float)from.x, (float)from.y, (float)from.z).setColor(GRID_COLOR).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
		consumer.addVertex(pose, (float)to.x, (float)to.y, (float)to.z).setColor(GRID_COLOR).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
	}
}
