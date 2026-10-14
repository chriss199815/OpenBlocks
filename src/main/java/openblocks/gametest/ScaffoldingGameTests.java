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
public class ScaffoldingGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void scaffoldingBreaksOnRandomTicks(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(POS, OBBlocks.SCAFFOLDING.get());
		// default despawn rate is 1 in 4, so 200 ticks are practically never all missed
		for (int i = 0; i < 200 && helper.getBlockState(POS).is(OBBlocks.SCAFFOLDING.get()); i++)
			helper.randomTick(POS);

		helper.assertBlockPresent(Blocks.AIR, POS);
		helper.assertItemEntityCountIs(OBBlocks.SCAFFOLDING.get().asItem(), POS, 1, 1);
		helper.succeed();
	}
}
