package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.JadedLadderBlock;
import openblocks.block.RopeLadderBlock;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class LadderGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final int TOP = 5;

	/**
	 * Stone pillar on the south edge and a ladder of 10 clicked onto it at {@link #TOP}, so it rolls down to the ground.
	 */
	private static Player placeRopeLadder(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.STONE);
		for (int y = 2; y <= TOP; y++)
			helper.setBlock(new BlockPos(1, y, 2), Blocks.STONE);

		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBBlocks.ROPE_LADDER.get(), 10));
		helper.placeAt(player, player.getMainHandItem(), new BlockPos(1, TOP, 2), Direction.NORTH);
		return player;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ropeLadderRollsDown(GameTestHelper helper) {
		final Player player = placeRopeLadder(helper);
		for (int y = 2; y <= TOP; y++) {
			final BlockState state = helper.getBlockState(new BlockPos(1, y, 1));
			helper.assertTrue(state.is(OBBlocks.ROPE_LADDER.get()) && state.getValue(RopeLadderBlock.FACING) == Direction.NORTH, "Expected a rope ladder at y=" + y + ", got " + state);
		}
		helper.assertTrue(player.getMainHandItem().getCount() == 6, "Expected one item per ladder block, 6 left, got " + player.getMainHandItem().getCount());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ropeLadderRollsUpWhenBroken(GameTestHelper helper) {
		placeRopeLadder(helper);
		helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(1, TOP, 1)), true);
		for (int y = 2; y <= TOP; y++)
			helper.assertBlockNotPresent(OBBlocks.ROPE_LADDER.get(), new BlockPos(1, y, 1));
		helper.assertItemEntityCountIs(OBBlocks.ROPE_LADDER.get().asItem(), new BlockPos(1, 3, 1), 3, 4);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ropeLadderHangsFromLadderAbove(GameTestHelper helper) {
		placeRopeLadder(helper);
		// the lower ladders lose their wall, but still hang from the top one
		for (int y = 2; y < TOP; y++)
			helper.setBlock(new BlockPos(1, y, 2), Blocks.AIR);
		for (int y = 2; y <= TOP; y++)
			helper.assertBlockPresent(OBBlocks.ROPE_LADDER.get(), new BlockPos(1, y, 1));

		helper.setBlock(new BlockPos(1, TOP, 2), Blocks.AIR);
		for (int y = 2; y <= TOP; y++)
			helper.assertBlockNotPresent(OBBlocks.ROPE_LADDER.get(), new BlockPos(1, y, 1));
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ropeLadderOnlyBlocksMobsFacingIt(GameTestHelper helper) {
		final BlockPos pos = GROUND.above();
		helper.setBlock(pos.south(), Blocks.STONE);
		helper.setBlock(pos, OBBlocks.ROPE_LADDER.get().defaultBlockState().setValue(RopeLadderBlock.FACING, Direction.NORTH));
		final BlockState state = helper.getBlockState(pos);
		final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, GROUND.above(2));

		pig.setYRot(Direction.SOUTH.toYRot());
		helper.assertFalse(state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.of(pig)).isEmpty(), "A pig facing the ladder should collide with it");
		pig.setYRot(Direction.EAST.toYRot());
		helper.assertTrue(state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.of(pig)).isEmpty(), "A pig walking along the ladder should pass through");
		helper.succeed();
	}

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
