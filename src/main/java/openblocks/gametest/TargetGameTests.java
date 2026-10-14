package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.TargetBlock;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class TargetGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final BlockPos POS = GROUND.above();

	private static void placeTarget(GameTestHelper helper, boolean powered) {
		helper.setBlock(GROUND, powered? Blocks.REDSTONE_BLOCK : Blocks.STONE);
		helper.setBlock(POS, OBBlocks.TARGET.get().defaultBlockState().setValue(TargetBlock.FACING, Direction.NORTH));
		helper.getBlockState(POS).handleNeighborChanged(helper.getLevel(), helper.absolutePos(POS), Blocks.AIR, helper.absolutePos(GROUND), false);
	}

	/** Arrow flying south into the bullseye on the north side of the target. */
	private static Arrow shootAtBullseye(GameTestHelper helper) {
		final Arrow arrow = helper.spawn(EntityType.ARROW, new Vec3(1.5, 2.55, 0.1));
		arrow.setNoGravity(true);
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		arrow.setDeltaMovement(0, 0, 0.5);
		return arrow;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void redstoneRaisesTarget(GameTestHelper helper) {
		placeTarget(helper, false);
		helper.assertBlockProperty(POS, TargetBlock.POWERED, false);
		helper.setBlock(GROUND, Blocks.REDSTONE_BLOCK);
		helper.assertBlockProperty(POS, TargetBlock.POWERED, true);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void bullseyeGivesStrongSignal(GameTestHelper helper) {
		placeTarget(helper, true);
		helper.setBlock(POS.east(), Blocks.STONE);
		shootAtBullseye(helper);
		helper.succeedWhen(() -> {
			final int power = helper.getBlockState(POS).getValue(TargetBlock.POWER);
			helper.assertTrue(power >= 13, "Expected a strong signal for a hit near the bullseye, got " + power);
			// the stone next to the target is strongly powered, so it powers the block behind it
			helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(POS.east().east())), "Expected the target to strongly power its neighbours");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void signalEndsAfterTenTicks(GameTestHelper helper) {
		placeTarget(helper, true);
		shootAtBullseye(helper);
		helper.runAfterDelay(4, () -> {
			helper.assertTrue(helper.getBlockState(POS).getValue(TargetBlock.POWER) > 0, "Expected a signal after the hit");
			helper.runAfterDelay(TargetBlock.SIGNAL_TICKS + 1, () -> {
				helper.assertBlockProperty(POS, TargetBlock.POWER, 0);
				helper.succeed();
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void foldedTargetIgnoresArrows(GameTestHelper helper) {
		placeTarget(helper, false);
		final Arrow arrow = helper.spawn(EntityType.ARROW, Vec3.atBottomCenterOf(POS.above(3)));
		arrow.setDeltaMovement(0, -1, 0);
		helper.runAfterDelay(20, () -> {
			helper.assertBlockProperty(POS, TargetBlock.POWER, 0);
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void foldingDropsArrows(GameTestHelper helper) {
		placeTarget(helper, true);
		final Arrow arrow = shootAtBullseye(helper);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(helper.getBlockState(POS).getValue(TargetBlock.POWER) > 0, "Expected the arrow to hit");
			// like legacy, the target powers the block below while its signal lasts, so it only folds afterwards
			helper.setBlock(GROUND, Blocks.STONE);
			helper.succeedWhen(() -> {
				helper.assertBlockProperty(POS, TargetBlock.POWERED, false);
				helper.assertTrue(arrow.isRemoved(), "The arrow should be taken out of the target");
				helper.assertItemEntityCountIs(Items.ARROW, POS, 1, 1);
			});
		});
	}
}
