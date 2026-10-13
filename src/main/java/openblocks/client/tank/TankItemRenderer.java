package openblocks.client.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import openblocks.block.entity.TankBlockEntity;
import openblocks.item.TankBlockItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBItems;

/**
 * Draws the tank item as frame plus fluid level; replaces legacy {@code openmods:textureditem} with its 17 fill-level models.
 */
public class TankItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static final boolean[] ALL_WALLS = { true, true, true, true, true, true };

	public TankItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private TankItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new TankItemRenderer();
				return renderer;
			}
		}, OBItems.TANK.get());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(OBBlocks.TANK.get().defaultBlockState(), poseStack, buffers, light, overlay, ModelData.EMPTY, null);

		final FluidStack fluid = TankBlockItem.getContents(stack);
		if (fluid.isEmpty()) return;

		final float level = Math.max(0.05f, Math.min(1.0f, (float)fluid.getAmount() / TankBlockEntity.getTankCapacity()));
		TankFluidRenderer.render(poseStack, buffers, fluid, new TankFluidRenderer.Shape(level, level, level, level, level, ALL_WALLS), light);
	}
}
