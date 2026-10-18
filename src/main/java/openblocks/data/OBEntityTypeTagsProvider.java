package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.entity.Magnet;

public class OBEntityTypeTagsProvider extends EntityTypeTagsProvider {

	public OBEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, OpenBlocks.MODID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		// legacy magnet entity whitelist: items, boats and minecarts (living entities are always pickable)
		tag(Magnet.PICKABLE).add(EntityType.ITEM, EntityType.BOAT, EntityType.CHEST_BOAT,
				EntityType.MINECART, EntityType.CHEST_MINECART, EntityType.FURNACE_MINECART, EntityType.HOPPER_MINECART,
				EntityType.TNT_MINECART, EntityType.SPAWNER_MINECART, EntityType.COMMAND_BLOCK_MINECART);
	}
}
