package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.BlockManipulatorBlock;
import openblocks.block.entity.BlockPlacerBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class BlockManipulatorGameTests {

	private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
	private static final BlockPos TARGET = MACHINE.east();
	private static final BlockPos BEHIND = MACHINE.west();
	private static final BlockPos POWER = MACHINE.above();

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void breakerPutsDropsIntoInventoryBehind(GameTestHelper helper) {
		helper.setBlock(MACHINE, OBBlocks.BLOCK_BREAKER.get().defaultBlockState().setValue(BlockManipulatorBlock.FACING, Direction.EAST));
		helper.setBlock(TARGET, Blocks.STONE);
		helper.setBlock(BEHIND, Blocks.CHEST);
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, TARGET);
			// stone needs a pickaxe to drop anything, so this also checks the tool selection
			final ChestBlockEntity chest = helper.getBlockEntity(BEHIND);
			helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 1, "Expected cobblestone in the chest behind the breaker");
			helper.assertBlockProperty(MACHINE, BlockManipulatorBlock.POWERED, true);
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void poweredBreakerWaitsBetweenActions(GameTestHelper helper) {
		helper.setBlock(MACHINE, OBBlocks.BLOCK_BREAKER.get().defaultBlockState().setValue(BlockManipulatorBlock.FACING, Direction.EAST));
		helper.setBlock(TARGET, Blocks.STONE);
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

		// the first block breaks right away, the next one only after the 10 tick cooldown
		helper.runAfterDelay(2, () -> {
			helper.assertBlockPresent(Blocks.AIR, TARGET);
			helper.setBlock(TARGET, Blocks.STONE);
		});
		helper.runAfterDelay(8, () -> helper.assertBlockPresent(Blocks.STONE, TARGET));
		helper.runAfterDelay(14, () -> {
			helper.assertBlockPresent(Blocks.AIR, TARGET);
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void breakerIgnoresUnbreakableBlocks(GameTestHelper helper) {
		helper.setBlock(MACHINE, OBBlocks.BLOCK_BREAKER.get().defaultBlockState().setValue(BlockManipulatorBlock.FACING, Direction.EAST));
		helper.setBlock(TARGET, Blocks.BEDROCK);
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(5, () -> {
			helper.assertBlockPresent(Blocks.BEDROCK, TARGET);
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void placerPlacesFacingAway(GameTestHelper helper) {
		helper.setBlock(MACHINE, OBBlocks.BLOCK_PLACER.get().defaultBlockState().setValue(BlockManipulatorBlock.FACING, Direction.EAST));
		final BlockPlacerBlockEntity placer = helper.getBlockEntity(MACHINE);
		placer.getInventory().setStackInSlot(2, new ItemStack(Items.PISTON, 2));
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.PISTON, TARGET);
			// like legacy, oriented as if placed by a player looking back at the placer
			helper.assertBlockProperty(TARGET, PistonBaseBlock.FACING, Direction.EAST);
			helper.assertTrue(placer.getInventory().getStackInSlot(2).getCount() == 1, "Expected one piston used");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void poweredPlacerPlacesInsertedItems(GameTestHelper helper) {
		helper.setBlock(MACHINE, OBBlocks.BLOCK_PLACER.get().defaultBlockState().setValue(BlockManipulatorBlock.FACING, Direction.EAST));
		final BlockPlacerBlockEntity placer = helper.getBlockEntity(MACHINE);
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(2, () -> {
			helper.assertBlockPresent(Blocks.AIR, TARGET);
			placer.getInventory().insertItem(0, new ItemStack(Items.OAK_PLANKS), false);
			helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.OAK_PLANKS, TARGET));
		});
	}
}
