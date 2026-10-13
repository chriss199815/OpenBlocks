package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.registry.OBBlocks;

public class OBBlockTagsProvider extends BlockTagsProvider {

	public OBBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, OpenBlocks.MODID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		final var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
		OBBlocks.ELEVATORS.values().forEach(block -> pickaxe.add(block.get()));
		OBBlocks.ROTATING_ELEVATORS.values().forEach(block -> pickaxe.add(block.get()));
		pickaxe.add(OBBlocks.TANK.get());
	}
}
