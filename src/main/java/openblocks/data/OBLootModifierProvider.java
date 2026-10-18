package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import openblocks.OpenBlocks;

/**
 * Global loot modifiers; replace the legacy {@code LootHandler}.
 */
public class OBLootModifierProvider extends GlobalLootModifierProvider {

	public OBLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, OpenBlocks.MODID);
	}

	@Override
	protected void start() {
		// like legacy, in mineshaft and dungeon chests; the config loot.technicolorGlasses is gone, a data pack can
		// override this modifier instead
		add("technicolor_glasses", new AddTableLootModifier(new LootItemCondition[] {
				AnyOfCondition.anyOf(
						LootTableIdCondition.builder(BuiltInLootTables.ABANDONED_MINESHAFT.location()),
						LootTableIdCondition.builder(BuiltInLootTables.SIMPLE_DUNGEON.location())).build()
		}, OBChestLoot.TECHNICOLOR_GLASSES));
	}
}
