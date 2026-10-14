package openblocks.client;

import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import openblocks.block.entity.PaintCanBlockEntity;
import openblocks.item.Paint;
import openblocks.item.PaintCanItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

public class OBColorHandlers {

	private static final int WHITE = 0xFFFFFFFF;

	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		registerDyedBlockColors(event, OBBlocks.ELEVATORS);
		registerDyedBlockColors(event, OBBlocks.ROTATING_ELEVATORS);
		registerDyedBlockColors(event, OBBlocks.FLAGS);
		event.register((state, level, pos, tintIndex) -> tintIndex == 1 && level != null && pos != null && level.getBlockEntity(pos) instanceof PaintCanBlockEntity can?
				0xFF000000 | can.getColor() : WHITE, OBBlocks.PAINT_CAN.get());
	}

	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		registerDyedItemColors(event, OBBlocks.ELEVATORS);
		registerDyedItemColors(event, OBBlocks.ROTATING_ELEVATORS);
		registerDyedItemColors(event, OBBlocks.FLAGS);
		event.register((stack, tintIndex) -> tintIndex == 1? 0xFF000000 | PaintCanItem.getColor(stack) : WHITE, OBItems.PAINT_CAN.get());
		event.register((stack, tintIndex) -> {
			final Paint paint = stack.get(OBComponents.PAINT);
			return tintIndex == 1 && paint != null? 0xFF000000 | paint.color() : WHITE;
		}, OBItems.PAINT_BRUSH.get());
	}

	private static void registerDyedBlockColors(RegisterColorHandlersEvent.Block event, Map<DyeColor, ? extends DeferredBlock<? extends Block>> blocks) {
		blocks.forEach((color, block) -> {
			final int rgb = color.getTextureDiffuseColor();
			event.register((state, level, pos, tintIndex) -> tintIndex == 0? rgb : WHITE, block.get());
		});
	}

	private static void registerDyedItemColors(RegisterColorHandlersEvent.Item event, Map<DyeColor, ? extends DeferredBlock<? extends Block>> blocks) {
		blocks.forEach((color, block) -> {
			final int rgb = color.getTextureDiffuseColor();
			event.register((stack, tintIndex) -> tintIndex == 0? rgb : WHITE, block.get());
		});
	}
}
