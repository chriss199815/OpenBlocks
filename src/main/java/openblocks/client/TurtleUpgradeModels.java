package openblocks.client;

import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import openblocks.integration.computercraft.OBTurtleUpgrades;

/**
 * Turtle upgrade models (CC: Tweaked, only registered when it is loaded); like legacy, which never got a proper model, the
 * magnet shows its item.
 */
public class TurtleUpgradeModels {

	public static void register(RegisterTurtleModellersEvent event) {
		event.register(OBTurtleUpgrades.MAGNET.get(), TurtleUpgradeModeller.flatItem());
	}
}
