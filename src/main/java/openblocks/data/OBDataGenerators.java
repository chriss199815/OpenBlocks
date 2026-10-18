package openblocks.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class OBDataGenerators {

	public static void gatherData(GatherDataEvent event) {
		final DataGenerator generator = event.getGenerator();
		final PackOutput output = generator.getPackOutput();
		final ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
		final CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		generator.addProvider(event.includeClient(), new OBStencilTextureProvider(output, existingFileHelper));
		generator.addProvider(event.includeClient(), new OBBlockStateProvider(output, existingFileHelper));

		generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
				List.of(new LootTableProvider.SubProviderEntry(OBBlockLoot::new, LootContextParamSets.BLOCK),
						new LootTableProvider.SubProviderEntry(OBChestLoot::new, LootContextParamSets.CHEST)), lookupProvider));
		generator.addProvider(event.includeServer(), new OBLootModifierProvider(output, lookupProvider));
		generator.addProvider(event.includeServer(), new OBRecipeProvider(output, lookupProvider));
		generator.addProvider(event.includeServer(), new OBAdvancementProvider(output, lookupProvider));
		generator.addProvider(event.includeServer(), new OBBlockTagsProvider(output, lookupProvider, existingFileHelper));
		generator.addProvider(event.includeServer(), new OBFluidTagsProvider(output, lookupProvider, existingFileHelper));
		generator.addProvider(event.includeServer(), new OBEntityTypeTagsProvider(output, lookupProvider, existingFileHelper));
		generator.addProvider(event.includeServer(), new OBStructureTagsProvider(output, lookupProvider, existingFileHelper));
		generator.addProvider(event.includeServer(), new OBDataMapProvider(output, lookupProvider));
		generator.addProvider(event.includeServer(), new OBGameTestStructureProvider(output));
	}
}
