package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.world.phys.AABB;
import openblocks.block.entity.AutoEnchantmentTableBlockEntity;

/**
 * Port of legacy {@code TileEntityAutoEnchantmentTableRenderer}: the floating book, drawn by the vanilla renderer from the table's book state.
 */
public class AutoEnchantmentTableRenderer implements BlockEntityRenderer<AutoEnchantmentTableBlockEntity> {

	private final EnchantTableRenderer book;

	public AutoEnchantmentTableRenderer(BlockEntityRendererProvider.Context context) {
		this.book = new EnchantTableRenderer(context);
	}

	@Override
	public void render(AutoEnchantmentTableBlockEntity table, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		book.render(table.getBook(), partialTick, poseStack, bufferSource, packedLight, packedOverlay);
	}

	@Override
	public AABB getRenderBoundingBox(AutoEnchantmentTableBlockEntity table) {
		// the book floats above the block
		return new AABB(table.getBlockPos()).expandTowards(0, 0.5, 0);
	}
}
