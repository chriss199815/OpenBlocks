package openblocks.integration.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy {@code MagnetTurtleUpgrade}: a crane magnet on the side of a turtle, controlled by {@link MagnetPeripheral}. Crafted
 * with the crane magnet (legacy used the removed miracle magnet).
 */
public class MagnetTurtleUpgrade extends AbstractTurtleUpgrade {

	public static final String ADJECTIVE = "upgrade.openblocks.magnet.adjective";

	public MagnetTurtleUpgrade(ItemStack item) {
		super(TurtleUpgradeType.PERIPHERAL, ADJECTIVE, item);
	}

	@Override
	public UpgradeType<MagnetTurtleUpgrade> getType() {
		return OBTurtleUpgrades.MAGNET.get();
	}

	@Override
	public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
		return new MagnetPeripheral(turtle, side);
	}

	@Override
	public void update(ITurtleAccess turtle, TurtleSide side) {
		if (!turtle.getLevel().isClientSide && turtle.getPeripheral(side) instanceof MagnetPeripheral magnet) magnet.tick();
	}
}
