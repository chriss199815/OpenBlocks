package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import openblocks.registry.OBBlocks;

public class OBDataMapProvider extends DataMapProvider {

	public OBDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider);
	}

	@Override
	protected void gather() {
		builder(NeoForgeDataMaps.FURNACE_FUELS)
				.add(OBBlocks.SCAFFOLDING.getId(), new FurnaceFuel(100), false);
	}
}
