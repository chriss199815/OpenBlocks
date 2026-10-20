package openblocks.integration.computercraft;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * CC: Tweaked peripherals, replacing the legacy OpenPeripheral adapters ({@code ModuleAdapters}). CC: Tweaked is optional,
 * so its classes are only touched through here after {@link #isLoaded()}.
 */
public class OBComputerCraft {

	public static final String MODID = "computercraft";

	public static boolean isLoaded() {
		return ModList.get().isLoaded(MODID);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		OBPeripherals.register(event);
	}
}
