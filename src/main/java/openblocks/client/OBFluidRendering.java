package openblocks.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import openblocks.OpenBlocks;
import openblocks.registry.OBFluids;

/**
 * Fluid textures; replaces the legacy {@code FluidTextureRegisterListener} (block atlas picks up {@code textures/block} on its own).
 */
public class OBFluidRendering {

	private static final ResourceLocation XP_JUICE_STILL = OpenBlocks.location("block/xp_juice_still");
	private static final ResourceLocation XP_JUICE_FLOWING = OpenBlocks.location("block/xp_juice_flowing");

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerFluidType(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return XP_JUICE_STILL;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return XP_JUICE_FLOWING;
			}
		}, OBFluids.XP_JUICE_TYPE.get());
	}
}
