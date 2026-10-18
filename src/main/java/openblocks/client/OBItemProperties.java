package openblocks.client;

import java.util.List;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import openblocks.OpenBlocks;
import openblocks.item.ImaginaryItem;
import openblocks.item.SlimalyzerItem;
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
			ItemProperties.register(OBItems.SLIMALYZER.get(), OpenBlocks.location("active"),
					(stack, level, entity, seed) -> SlimalyzerItem.isActive(stack)? 1 : 0);
			// legacy "mode": the overlay shows the placement mode
			for (Item item : List.of(OBItems.PENCIL.get(), OBItems.CRAYON.get()))
				ItemProperties.register(item, OpenBlocks.location("mode"), (stack, level, entity, seed) -> ImaginaryItem.getMode(stack).ordinal());
		});
	}
}
