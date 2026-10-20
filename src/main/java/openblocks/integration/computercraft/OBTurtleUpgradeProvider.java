package openblocks.integration.computercraft;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import openblocks.OpenBlocks;
import openblocks.registry.OBItems;

/**
 * The magnet turtle upgrade; CC: Tweaked loads turtle upgrades from data packs.
 */
public class OBTurtleUpgradeProvider extends DatapackBuiltinEntriesProvider {

	private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
			.add(ITurtleUpgrade.REGISTRY, context -> context.register(OBTurtleUpgrades.MAGNET_UPGRADE, new MagnetTurtleUpgrade(new ItemStack(OBItems.CRANE_MAGNET.get()))));

	public OBTurtleUpgradeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, BUILDER, Set.of(OpenBlocks.MODID));
	}

	// OBDatapackProvider already has the default name
	@Override
	public String getName() {
		return "OpenBlocks turtle upgrades";
	}
}
