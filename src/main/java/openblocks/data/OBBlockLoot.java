package openblocks.data;

import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;

public class OBBlockLoot extends BlockLootSubProvider {

	public OBBlockLoot(HolderLookup.Provider registries) {
		super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
	}

	@Override
	protected void generate() {
		OBBlocks.ELEVATORS.values().forEach(block -> dropSelf(block.get()));
		OBBlocks.ROTATING_ELEVATORS.values().forEach(block -> dropSelf(block.get()));
		dropSelf(OBBlocks.XP_DRAIN.get());
		dropSelf(OBBlocks.XP_SHOWER.get());
		dropSelf(OBBlocks.XP_BOTTLER.get());
		dropSelf(OBBlocks.VACUUM_HOPPER.get());
		dropSelf(OBBlocks.ITEM_DROPPER.get());
		dropSelf(OBBlocks.BLOCK_BREAKER.get());
		dropSelf(OBBlocks.BLOCK_PLACER.get());
		dropSelf(OBBlocks.AUTO_ANVIL.get());
		dropSelf(OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
		dropSelf(OBBlocks.DONATION_STATION.get());
		dropSelf(OBBlocks.DRAWING_TABLE.get());
		dropSelf(OBBlocks.PAINT_MIXER.get());
		dropSelf(OBBlocks.FAN.get());
		OBBlocks.FLAGS.values().forEach(flag -> dropSelf(flag.get()));
		dropSelf(OBBlocks.BEAR_TRAP.get());
		dropSelf(OBBlocks.SPRINKLER.get());
		dropSelf(OBBlocks.LADDER.get());
		// nothing in infinite mode, see RopeLadderBlock.getDrops
		dropSelf(OBBlocks.ROPE_LADDER.get());
		dropSelf(OBBlocks.SPONGE.get());
		dropSelf(OBBlocks.SCAFFOLDING.get());
		dropSelf(OBBlocks.HEAL.get());
		dropSelf(OBBlocks.BIG_BUTTON.get());
		dropSelf(OBBlocks.BIG_BUTTON_WOOD.get());
		dropSelf(OBBlocks.TARGET.get());
		dropSelf(OBBlocks.PATH.get());
		dropSelf(OBBlocks.SKY.get());
		dropSelf(OBBlocks.SKY_INVERTED.get());
		dropSelf(OBBlocks.GOLDEN_EGG.get());
		dropSelf(OBBlocks.CANNON.get());
		dropSelf(OBBlocks.PROJECTOR.get());
		// a canvas that replaced a block drops that block instead, see CanvasBlock.getDrops
		dropSelf(OBBlocks.CANVAS.get());
		dropOther(OBBlocks.CANVAS_GLASS.get(), OBBlocks.CANVAS.get());
		add(OBBlocks.PAINT_CAN.get(), createSingleItemTable(OBBlocks.PAINT_CAN.get())
				.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(OBComponents.PAINT.get())));
		// graves release their items when broken, but never drop themselves
		add(OBBlocks.GRAVE.get(), noDrop());
		add(OBBlocks.TROPHY.get(), createSingleItemTable(OBBlocks.TROPHY.get())
				.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(OBComponents.TROPHY.get()).include(OBComponents.TROPHY_COOLDOWN.get())));
		add(OBBlocks.GUIDE.get(), createSingleItemTable(OBBlocks.GUIDE.get())
				.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(OBComponents.GUIDE.get())));
		add(OBBlocks.BUILDER_GUIDE.get(), createSingleItemTable(OBBlocks.BUILDER_GUIDE.get())
				.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(OBComponents.GUIDE.get())));
		add(OBBlocks.TANK.get(), createSingleItemTable(OBBlocks.TANK.get())
				.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(OBComponents.TANK_CONTENTS.get())));
	}

	@Override
	protected Iterable<Block> getKnownBlocks() {
		return OBBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast)::iterator;
	}
}
