package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;

/**
 * Custom statistics, shown in the "General" tab.
 */
public class OBStats {

	public static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, OpenBlocks.MODID);

	public static final DeferredHolder<ResourceLocation, ResourceLocation> BRICKS_DROPPED = STATS.register("bricks_dropped", () -> OpenBlocks.location("bricks_dropped"));
}
