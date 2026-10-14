package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.BearTrapBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class BearTrapGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	private static BearTrapBlockEntity placeTrap(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(POS, OBBlocks.BEAR_TRAP.get());
		return helper.getBlockEntity(POS);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void openedTrapHoldsMob(GameTestHelper helper) {
		final BearTrapBlockEntity trap = placeTrap(helper);
		helper.assertTrue(trap.isShut(), "New traps should be shut");
		helper.useBlock(POS);
		helper.assertFalse(trap.isShut(), "Clicking should open the trap");

		helper.runAfterDelay(BearTrapBlockEntity.OPENING_TICKS + 1, () -> {
			final Pig pig = helper.spawn(EntityType.PIG, POS);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(trap.isShut() && pig.getUUID().equals(trap.getTrappedId()), "The pig should be trapped");
				helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(POS)).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(POS)) == 1,
						"Expected comparator output 1");

				pig.setDeltaMovement(1, 0, 1);
				helper.runAfterDelay(20, () -> {
					helper.assertTrue(pig.position().distanceTo(Vec3.atBottomCenterOf(helper.absolutePos(POS))) < 0.01, "The pig escaped to " + pig.position());

					helper.useBlock(POS);
					helper.assertTrue(!trap.isShut() && trap.getTrappedId() == null, "Clicking should release the pig");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void trapReopensWhenMobDies(GameTestHelper helper) {
		final BearTrapBlockEntity trap = placeTrap(helper);
		helper.useBlock(POS);
		helper.runAfterDelay(BearTrapBlockEntity.OPENING_TICKS + 1, () -> {
			final Pig pig = helper.spawn(EntityType.PIG, POS);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(trap.isShut(), "The pig should be trapped");
				pig.kill();
				helper.succeedWhen(() -> helper.assertFalse(trap.isShut(), "The trap should open once the pig is gone"));
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void poweredTrapStaysOpen(GameTestHelper helper) {
		final BearTrapBlockEntity trap = placeTrap(helper);
		helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(trap.isLocked() && !trap.isShut(), "Redstone should open and lock the trap");

		helper.runAfterDelay(BearTrapBlockEntity.OPENING_TICKS + 1, () -> {
			helper.spawn(EntityType.PIG, POS);
			helper.runAfterDelay(5, () -> {
				helper.assertFalse(trap.isShut(), "A locked trap must not catch mobs");
				helper.succeed();
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void trapCatchesOnlyAfterOpening(GameTestHelper helper) {
		final BearTrapBlockEntity trap = placeTrap(helper);
		helper.useBlock(POS);
		helper.spawn(EntityType.PIG, POS);
		helper.runAfterDelay(5, () -> {
			helper.assertFalse(trap.isShut(), "The trap caught a mob while still opening");
			helper.succeed();
		});
	}
}
