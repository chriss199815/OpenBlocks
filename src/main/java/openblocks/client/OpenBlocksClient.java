package openblocks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.OpenBlocks;

/**
 * Client-only entry point; wires renderers, color handlers and input listeners.
 */
@Mod(value = OpenBlocks.MODID, dist = Dist.CLIENT)
public class OpenBlocksClient {

	public OpenBlocksClient(IEventBus modBus) {
		modBus.addListener(OBColorHandlers::registerBlockColors);
		modBus.addListener(OBColorHandlers::registerItemColors);

		NeoForge.EVENT_BUS.addListener(ElevatorInputHandler::onMovementInput);
	}
}
