package openblocks.client;

import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import openblocks.registry.OBBlocks;

public class OBColorHandlers {

	private static final int WHITE = 0xFFFFFFFF;

	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		registerElevatorBlockColors(event, OBBlocks.ELEVATORS);
		registerElevatorBlockColors(event, OBBlocks.ROTATING_ELEVATORS);
	}

	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		registerElevatorItemColors(event, OBBlocks.ELEVATORS);
		registerElevatorItemColors(event, OBBlocks.ROTATING_ELEVATORS);
	}

	private static void registerElevatorBlockColors(RegisterColorHandlersEvent.Block event, Map<DyeColor, ? extends DeferredBlock<? extends Block>> blocks) {
		blocks.forEach((color, block) -> {
			final int rgb = color.getTextureDiffuseColor();
			event.register((state, level, pos, tintIndex) -> tintIndex == 0? rgb : WHITE, block.get());
		});
	}

	private static void registerElevatorItemColors(RegisterColorHandlersEvent.Item event, Map<DyeColor, ? extends DeferredBlock<? extends Block>> blocks) {
		blocks.forEach((color, block) -> {
			final int rgb = color.getTextureDiffuseColor();
			event.register((stack, tintIndex) -> tintIndex == 0? rgb : WHITE, block.get());
		});
	}
}
