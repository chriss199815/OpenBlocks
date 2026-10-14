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
		pickaxe.add(OBBlocks.ITEM_DROPPER.get());
		pickaxe.add(OBBlocks.BLOCK_BREAKER.get());
		pickaxe.add(OBBlocks.BLOCK_PLACER.get());
		pickaxe.add(OBBlocks.AUTO_ANVIL.get());
		pickaxe.add(OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
		pickaxe.add(OBBlocks.DONATION_STATION.get());
		pickaxe.add(OBBlocks.PAINT_MIXER.get());
		pickaxe.add(OBBlocks.FAN.get());
		pickaxe.add(OBBlocks.BEAR_TRAP.get());
		pickaxe.add(OBBlocks.SPRINKLER.get());
		pickaxe.add(OBBlocks.HEAL.get());

		tag(BlockTags.MINEABLE_WITH_AXE).add(OBBlocks.DRAWING_TABLE.get(), OBBlocks.LADDER.get(), OBBlocks.SPONGE.get());
		tag(BlockTags.TRAPDOORS).add(OBBlocks.LADDER.get());
		tag(BlockTags.CLIMBABLE).add(OBBlocks.ROPE_LADDER.get());
		tag(BlockTags.MINEABLE_WITH_SHOVEL).add(OBBlocks.GRAVE.get());
		tag(BlockTags.STONE_BUTTONS).add(OBBlocks.BIG_BUTTON.get());
		tag(BlockTags.WOODEN_BUTTONS).add(OBBlocks.BIG_BUTTON_WOOD.get());
		// replaces legacy canEntityDestroy
		tag(BlockTags.WITHER_IMMUNE).add(OBBlocks.GRAVE.get());
		tag(BlockTags.DRAGON_IMMUNE).add(OBBlocks.GRAVE.get());
	}
}
