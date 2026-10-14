package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class PathGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 2, 1);
	private static final BlockPos POS = GROUND.above();

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void pathHasNoCollision(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.STONE);
		helper.setBlock(POS, OBBlocks.PATH.get());
		helper.assertTrue(helper.getBlockState(POS).getCollisionShape(helper.getLevel(), helper.absolutePos(POS), CollisionContext.empty()).isEmpty(), "Paths should not have collision");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void pathBreaksWithoutGround(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.STONE);
		helper.setBlock(POS, OBBlocks.PATH.get());
		helper.getLevel().destroyBlock(helper.absolutePos(GROUND), false);
		helper.assertBlockNotPresent(OBBlocks.PATH.get(), POS);
		helper.assertItemEntityCountIs(OBBlocks.PATH.get().asItem(), POS, 1, 1);
		helper.succeed();
	}
}
