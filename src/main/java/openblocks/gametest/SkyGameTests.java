package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.SkyBlock;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class SkyGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void redstoneActivatesSkyBlock(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.SKY.get());
		helper.assertFalse(SkyBlock.isActive(helper.getBlockState(POS)), "Unpowered sky block should be inactive");

		helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> {
			final BlockState state = helper.getBlockState(POS);
			helper.assertTrue(SkyBlock.isActive(state), "Powered sky block should be active");
			// unlike legacy, still targetable while active
			helper.assertFalse(state.getShape(helper.getLevel(), helper.absolutePos(POS)).isEmpty(), "Active sky block should keep its outline");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void invertedSkyBlockTurnsOffWithRedstone(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.SKY_INVERTED.get());
		helper.assertTrue(SkyBlock.isActive(helper.getBlockState(POS)), "Unpowered inverted sky block should be active");

		helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> helper.assertFalse(SkyBlock.isActive(helper.getBlockState(POS)), "Powered inverted sky block should be inactive"));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void skyBlockPlacedNextToPowerIsActive(GameTestHelper helper) {
		helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(POS, OBBlocks.SKY.get());
		helper.succeedWhen(() -> helper.assertBlockProperty(POS, SkyBlock.POWERED, true));
	}
}
