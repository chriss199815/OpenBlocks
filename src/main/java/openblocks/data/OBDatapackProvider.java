package openblocks.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import openblocks.OpenBlocks;
import openblocks.registry.OBEnchantments;

/**
 * Data driven registry entries: the enchantments.
 */
public class OBDatapackProvider extends DatapackBuiltinEntriesProvider {

	private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
			.add(Registries.ENCHANTMENT, OBEnchantments::bootstrap);

	public OBDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, BUILDER, Set.of(OpenBlocks.MODID));
	}
}
