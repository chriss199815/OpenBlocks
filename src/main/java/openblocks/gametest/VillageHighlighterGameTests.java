package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.VillageHighlighterBlock;
import openblocks.block.entity.VillageHighlighterBlockEntity;
import openblocks.block.entity.VillageHighlighterBlockEntity.PoiKind;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class VillageHighlighterGameTests {

	// facing north, so the signal leaves to the south
	private static final BlockPos HIGHLIGHTER = new BlockPos(1, 2, 1);
	private static final BlockPos FRONT = HIGHLIGHTER.north();
	private static final BlockPos BACK = HIGHLIGHTER.south();

	private static void placeHighlighter(GameTestHelper helper) {
		helper.setBlock(HIGHLIGHTER, OBBlocks.VILLAGE_HIGHLIGHTER.get().defaultBlockState().setValue(VillageHighlighterBlock.FACING, Direction.NORTH));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void villageHighlighterIgnoresPowerFromTheBack(GameTestHelper helper) {
		placeHighlighter(helper);
		helper.setBlock(FRONT, Blocks.REDSTONE_BLOCK);
		helper.assertBlockProperty(HIGHLIGHTER, VillageHighlighterBlock.POWERED, true);
		helper.setBlock(FRONT, Blocks.AIR);
		helper.assertBlockProperty(HIGHLIGHTER, VillageHighlighterBlock.POWERED, false);
		helper.setBlock(BACK, Blocks.REDSTONE_BLOCK);
		helper.assertBlockProperty(HIGHLIGHTER, VillageHighlighterBlock.POWERED, false);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 100)
	public static void villageHighlighterSignalsWhenVillagersCanBreed(GameTestHelper helper) {
		placeHighlighter(helper);
		helper.setBlock(BACK, Blocks.REDSTONE_LAMP);
		helper.setBlock(FRONT, Blocks.REDSTONE_BLOCK);
		// a free bed, the head is the point of interest
		helper.setBlock(new BlockPos(0, 1, 1), Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
		helper.setBlock(new BlockPos(0, 1, 0), Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));

		helper.startSequence()
				.thenIdle(20)
				.thenExecute(() -> {
					final VillageHighlighterBlockEntity highlighter = helper.getBlockEntity(HIGHLIGHTER);
					helper.assertTrue(highlighter.getPois().stream().anyMatch(poi -> poi.kind() == PoiKind.BED && poi.free()), "The highlighter should show the free bed");
					helper.assertBlockProperty(BACK, RedstoneLampBlock.LIT, false);
					// without AI, so they do not claim the bed
					helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 0));
					helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
				})
				.thenWaitUntil(() -> helper.assertBlockProperty(BACK, RedstoneLampBlock.LIT, true))
				.thenExecute(() -> {
					final VillageHighlighterBlockEntity highlighter = helper.getBlockEntity(HIGHLIGHTER);
					helper.assertTrue(highlighter.getVillagers().size() >= 2, "The highlighter should show the villagers");
					helper.assertTrue(highlighter.getLevel().getSignal(helper.absolutePos(HIGHLIGHTER), Direction.SOUTH) == 0, "No signal should leave through the front");
				})
				.thenSucceed();
	}
}
