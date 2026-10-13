package openblocks.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public class OBCapabilities {

	public static void register(RegisterCapabilitiesEvent event) {
		// NeoForge only attaches the bucket wrapper to exact BucketItem instances, not subclasses
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidBucketWrapper(stack), OBItems.XP_BUCKET.get());
	}
}
