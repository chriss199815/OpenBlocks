package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.item.GoldenEyeItem;

public class OBStructureTagsProvider extends TagsProvider<Structure> {

	public OBStructureTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
		super(output, Registries.STRUCTURE, lookupProvider, OpenBlocks.MODID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		// the structures legacy knew (OpenModsLib StructureRegistry): "Temple" covered pyramids, swamp huts and igloos
		tag(GoldenEyeItem.LOCATED)
				.add(BuiltinStructures.STRONGHOLD, BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.OCEAN_MONUMENT,
						BuiltinStructures.DESERT_PYRAMID, BuiltinStructures.JUNGLE_TEMPLE, BuiltinStructures.SWAMP_HUT, BuiltinStructures.IGLOO,
						BuiltinStructures.FORTRESS, BuiltinStructures.END_CITY)
				.addTag(StructureTags.VILLAGE)
				.addTag(StructureTags.MINESHAFT);
	}
}
