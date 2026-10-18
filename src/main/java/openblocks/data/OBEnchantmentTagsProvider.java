package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.registry.OBEnchantments;

public class OBEnchantmentTagsProvider extends EnchantmentTagsProvider {

	public OBEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, OpenBlocks.MODID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		// like legacy normal enchantments: enchanting table, villager trades and random loot
		tag(EnchantmentTags.NON_TREASURE).add(OBEnchantments.EXPLOSIVE, OBEnchantments.LAST_STAND, OBEnchantments.FLIM_FLAM);
	}
}
