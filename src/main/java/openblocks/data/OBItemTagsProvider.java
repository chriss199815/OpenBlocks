package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.registry.OBEnchantments;

public class OBItemTagsProvider extends ItemTagsProvider {

	public OBItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
			CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, blockTags, OpenBlocks.MODID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		tag(OBEnchantments.FLIM_FLAM_PRIMARY).addTag(ItemTags.ARMOR_ENCHANTABLE).addTag(ItemTags.SWORD_ENCHANTABLE);
	}
}
