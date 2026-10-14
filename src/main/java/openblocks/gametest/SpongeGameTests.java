package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class SpongeGameTests {

	private static final BlockPos POS = new BlockPos(1, 3, 1);

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spongeRemovesWater(GameTestHelper helper) {
		final BlockPos near = new BlockPos(0, 2, 0);
		final BlockPos far = new BlockPos(2, 6, 2);
		helper.setBlock(near.below(), Blocks.STONE);
		helper.setBlock(near, Blocks.WATER);
		helper.setBlock(far, Blocks.WATER);
		helper.setBlock(POS, OBBlocks.SPONGE.get());

		helper.assertBlockPresent(Blocks.AIR, near);
		helper.assertBlockPresent(Blocks.AIR, far);
		helper.assertBlockPresent(OBBlocks.SPONGE.get(), POS);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spongeRemovesWaterFlowingNextToIt(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.SPONGE.get());
		helper.setBlock(POS.east(), Blocks.WATER);
		helper.assertBlockPresent(Blocks.AIR, POS.east());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spongeBurnsOnLava(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(new BlockPos(0, 2, 0), Blocks.STONE);
		helper.setBlock(new BlockPos(0, 3, 0), Blocks.LAVA);
		helper.setBlock(POS, OBBlocks.SPONGE.get());

		helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 3, 0));
		// the fire is set by a block event at the end of the tick
		helper.succeedWhen(() -> helper.assertBlockNotPresent(OBBlocks.SPONGE.get(), POS));
	}
}
