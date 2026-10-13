package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.XpShowerBlock;
import openblocks.block.entity.TankBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.NoFlyXpOrb;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class XpShowerGameTests {

	private static final BlockPos TANK = new BlockPos(1, 3, 0);
	private static final BlockPos SHOWER = TANK.south();
	private static final BlockPos DRAIN_TANK = new BlockPos(1, 1, 1);
	private static final BlockPos DRAIN = DRAIN_TANK.above();

	private static TankBlockEntity placeTankWithShower(GameTestHelper helper, int xp) {
		helper.setBlock(TANK, OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(TANK);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(xp), IFluidHandler.FluidAction.EXECUTE);
		helper.setBlock(SHOWER, OBBlocks.XP_SHOWER.get().defaultBlockState().setValue(XpShowerBlock.FACING, Direction.SOUTH));
		return tank;
	}

	private static List<NoFlyXpOrb> orbs(GameTestHelper helper) {
		return helper.getLevel().getEntitiesOfClass(NoFlyXpOrb.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void unpoweredShowerDoesNothing(GameTestHelper helper) {
		final TankBlockEntity tank = placeTankWithShower(helper, 50);

		helper.runAfterDelay(20, () -> {
			helper.assertTrue(orbs(helper).isEmpty(), "Unpowered shower must not spawn orbs");
			helper.assertTrue(tank.getFluid().getAmount() == FluidXpUtils.xpToFluid(50), "Tank must be untouched, got " + tank.getFluid().getAmount());
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void poweredShowerFeedsDrain(GameTestHelper helper) {
		helper.setBlock(DRAIN_TANK, OBBlocks.TANK.get());
		helper.setBlock(DRAIN, OBBlocks.XP_DRAIN.get());
		final TankBlockEntity target = helper.getBlockEntity(DRAIN_TANK);

		final TankBlockEntity source = placeTankWithShower(helper, 50);
		helper.setBlock(SHOWER.above(), Blocks.REDSTONE_BLOCK);

		// orbs fall straight onto the drain below the shower, all XP ends up in the second tank
		helper.succeedWhen(() -> {
			helper.assertTrue(source.getFluid().isEmpty(), "Expected source tank drained, has " + source.getFluid().getAmount());
			helper.assertTrue(orbs(helper).isEmpty(), "Expected all orbs consumed");
			helper.assertTrue(target.getFluid().getAmount() == FluidXpUtils.xpToFluid(50), "Expected 50 XP in target tank, got " + target.getFluid().getAmount());
		});
	}
}
