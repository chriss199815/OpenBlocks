package openblocks.data;

import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import openblocks.registry.OBBlocks;

public class OBBlockLoot extends BlockLootSubProvider {

	public OBBlockLoot(HolderLookup.Provider registries) {
		super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
	}

	@Override
	protected void generate() {
		OBBlocks.ELEVATORS.values().forEach(block -> dropSelf(block.get()));
		OBBlocks.ROTATING_ELEVATORS.values().forEach(block -> dropSelf(block.get()));
	}

	@Override
	protected Iterable<Block> getKnownBlocks() {
		return OBBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast)::iterator;
	}
}
