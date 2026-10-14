package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.JadedLadderBlock;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class LadderGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void jadedLadderClimbableWhenOpen(GameTestHelper helper) {
		final BlockPos pos = GROUND.above();
		helper.setBlock(GROUND, Blocks.STONE);
		helper.setBlock(pos, OBBlocks.LADDER.get());
		final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, pos);
		helper.assertFalse(pig.onClimbable(), "A closed jaded ladder must not be climbable");

		helper.useBlock(pos);
		helper.assertBlockProperty(pos, JadedLadderBlock.OPEN, true);
		// the block the pig is in is cached until its next tick
		helper.succeedWhen(() -> helper.assertTrue(pig.onClimbable(), "An open jaded ladder should be climbable"));
	}
}
