package openblocks.integration.computercraft;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;

/**
 * Turtle upgrade types; the upgrades themselves are data driven (see {@link OBTurtleUpgradeProvider}).
 */
public class OBTurtleUpgrades {

	public static final DeferredRegister<UpgradeType<? extends ITurtleUpgrade>> UPGRADE_TYPES = DeferredRegister.create(ITurtleUpgrade.typeRegistry(), OpenBlocks.MODID);

	public static final DeferredHolder<UpgradeType<? extends ITurtleUpgrade>, UpgradeType<MagnetTurtleUpgrade>> MAGNET = UPGRADE_TYPES.register("magnet",
			() -> UpgradeType.simpleWithCustomItem(MagnetTurtleUpgrade::new));

	public static final ResourceKey<ITurtleUpgrade> MAGNET_UPGRADE = ITurtleUpgrade.createKey(OpenBlocks.location("magnet"));
}
