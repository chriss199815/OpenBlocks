package openblocks.integration.computercraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * CC: Tweaked peripherals and the magnet turtle upgrade, replacing the legacy OpenPeripheral adapters ({@code ModuleAdapters},
 * {@code ModuleTurtles}). CC: Tweaked is optional, so this class doesn't touch its classes; the others are only used after
 * {@link #isLoaded()}.
 */
public class OBComputerCraft {

	public static final String MODID = "computercraft";

	public static boolean isLoaded() {
		return ModList.get().isLoaded(MODID);
	}

	public static void register(IEventBus modBus) {
		OBTurtleUpgrades.UPGRADE_TYPES.register(modBus);
		modBus.addListener(OBPeripherals::register);
	}
}
