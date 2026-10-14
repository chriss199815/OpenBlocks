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
		pickaxe.add(OBBlocks.XP_DRAIN.get());
		pickaxe.add(OBBlocks.XP_SHOWER.get());
		pickaxe.add(OBBlocks.XP_BOTTLER.get());
		pickaxe.add(OBBlocks.VACUUM_HOPPER.get());

		tag(BlockTags.MINEABLE_WITH_SHOVEL).add(OBBlocks.GRAVE.get());
		// replaces legacy canEntityDestroy
		tag(BlockTags.WITHER_IMMUNE).add(OBBlocks.GRAVE.get());
		tag(BlockTags.DRAGON_IMMUNE).add(OBBlocks.GRAVE.get());
	}
}
