package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import openblocks.block.VillageHighlighterBlock;
import openblocks.block.entity.VillageHighlighterBlockEntity;
import openblocks.block.entity.VillageHighlighterBlockEntity.Poi;
import openblocks.block.entity.VillageHighlighterBlockEntity.VillagerInfo;

/**
 * Replaces legacy {@code TileEntityVillageHighlighterRenderer} (village sphere of points and golem spawn box): outlines
 * of beds (blue), job sites (yellow) and bells (magenta), bright while free and dark while claimed, and around every
 * villager the area where it can summon an iron golem. Each villager has its own color (from its entity id, like legacy
 * colored villages), solid while it could summon a golem now and faint otherwise. The area follows the villager entity
 * on the client and, like the vanilla summoning, snaps to the block the villager stands in.
 */
public class VillageHighlighterRenderer implements BlockEntityRenderer<VillageHighlighterBlockEntity> {

	// like the vanilla golem summoning: 8 blocks to the sides, 6 up and down
	private static final AABB GOLEM_AREA = new AABB(-8, -6, -8, 9, 7, 9);

	private static final float GOLDEN_RATIO = 0.618034f;

	private static final int[] FREE_COLORS = { 0x40A0FF, 0xFFE040, 0xFF40FF };
	private static final int[] CLAIMED_COLORS = { 0x203C80, 0x806010, 0x802080 };

	public VillageHighlighterRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(VillageHighlighterBlockEntity highlighter, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		if (!highlighter.getBlockState().getValue(VillageHighlighterBlock.POWERED)) return;

		final BlockPos origin = highlighter.getBlockPos();
		final VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

		for (Poi poi : highlighter.getPois()) {
			final int color = (poi.free()? FREE_COLORS : CLAIMED_COLORS)[poi.kind().ordinal()];
			renderBox(poseStack, consumer, new AABB(poi.pos().subtract(origin)).inflate(0.01), color, 1);
		}

		for (VillagerInfo villager : highlighter.getVillagers()) {
			if (!(highlighter.getLevel().getEntity(villager.entityId()) instanceof Villager entity)) continue;
			final BlockPos pos = BlockPos.containing(entity.getPosition(partialTick)).subtract(origin);
			final AABB area = GOLEM_AREA.move(pos.getX(), pos.getY(), pos.getZ());
			renderBox(poseStack, consumer, area, villagerColor(villager.entityId()), villager.wantsGolem()? 1 : 0.3f);
		}
	}

	/**
	 * Hues spread by the golden angle, so villagers with neighboring ids get clearly different colors.
	 */
	private static int villagerColor(int entityId) {
		final float hue = (entityId * GOLDEN_RATIO) % 1;
		return Mth.hsvToRgb(hue, 0.8f, 1);
	}

	private static void renderBox(PoseStack poseStack, VertexConsumer consumer, AABB box, int color, float alpha) {
		LevelRenderer.renderLineBox(poseStack, consumer, box, ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, alpha);
	}

	@Override
	public boolean shouldRenderOffScreen(VillageHighlighterBlockEntity highlighter) {
		return true;
	}

	@Override
	public int getViewDistance() {
		// like legacy
		return 256;
	}

	@Override
	public AABB getRenderBoundingBox(VillageHighlighterBlockEntity highlighter) {
		return new AABB(highlighter.getBlockPos()).inflate(VillageHighlighterBlockEntity.RANGE + GOLEM_AREA.getXsize());
	}
}
