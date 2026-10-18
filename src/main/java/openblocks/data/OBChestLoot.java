package openblocks.data;

import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import openblocks.OpenBlocks;
import openblocks.registry.OBItems;

/**
 * Loot tables that {@link OBLootModifierProvider} adds to vanilla chests; legacy {@code loot_tables/inject}.
 */
public class OBChestLoot implements LootTableSubProvider {

	public static final ResourceKey<LootTable> TECHNICOLOR_GLASSES = ResourceKey.create(Registries.LOOT_TABLE, OpenBlocks.location("inject/technicolor_glasses"));

	public OBChestLoot(HolderLookup.Provider registries) {}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		output.accept(TECHNICOLOR_GLASSES, LootTable.lootTable().withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(OBItems.TECHNICOLOR_GLASSES.get()).setWeight(20))
				.add(EmptyLootItem.emptyItem().setWeight(80))));
	}
}
