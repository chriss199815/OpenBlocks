package openblocks.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import openblocks.OpenBlocks;
import openblocks.registry.OBItems;

/**
 * Item properties for model overrides of simple items; items with their own renderer register theirs there.
 */
public class OBItemProperties {

	public static void register(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			// legacy "speed": the model switches to the moving pedometer above 0.1 blocks per tick
			ItemProperties.register(OBItems.PEDOMETER.get(), OpenBlocks.location("speed"),
					(stack, level, entity, seed) -> entity != null? (float)entity.getDeltaMovement().length() : 0);
		});
	}
}
