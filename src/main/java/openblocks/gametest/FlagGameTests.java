package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.FlagBlock;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class FlagGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final BlockPos POS = GROUND.above();

	private static Player playerWith(GameTestHelper helper, DyeColor color, float yaw) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setYRot(yaw);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBBlocks.FLAGS.get(color).get(), 4));
		return player;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void groundFlagPointsWherePlayerLooks(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.STONE);
		final Player player = playerWith(helper, DyeColor.RED, 95);
		helper.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);

		final BlockState state = helper.getBlockState(POS);
		helper.assertTrue(state.is(OBBlocks.FLAGS.get(DyeColor.RED).get()), "Expected a red flag, got " + state);
		helper.assertTrue(state.getValue(FlagBlock.FACING) == Direction.UP && state.getValue(FlagBlock.ROTATION) == 9, "Expected a ground flag at 90°, got " + state);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void clickTurnsGroundFlag(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.STONE);
		helper.setBlock(POS, OBBlocks.FLAGS.get(DyeColor.LIME).get());
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(POS, player);
		helper.assertBlockProperty(POS, FlagBlock.ROTATION, 1);

		player.setShiftKeyDown(true);
		helper.useBlock(POS, player);
		helper.useBlock(POS, player);
		helper.assertBlockProperty(POS, FlagBlock.ROTATION, FlagBlock.ROTATIONS - 1);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void wallFlagFallsWithWall(GameTestHelper helper) {
		final BlockPos wall = new BlockPos(0, 3, 1);
		helper.setBlock(wall, Blocks.STONE);
		final Player player = playerWith(helper, DyeColor.BLUE, 0);
		helper.placeAt(player, player.getMainHandItem(), wall, Direction.EAST);
		helper.assertBlockProperty(wall.east(), FlagBlock.FACING, Direction.EAST);

		helper.setBlock(wall, Blocks.AIR);
		helper.assertBlockNotPresent(OBBlocks.FLAGS.get(DyeColor.BLUE).get(), wall.east());
		helper.assertItemEntityPresent(OBBlocks.FLAGS.get(DyeColor.BLUE).get().asItem(), wall.east(), 1);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void flagsStandOnFencesAndFlags(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.OAK_FENCE);
		final Player player = playerWith(helper, DyeColor.WHITE, 0);
		helper.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
		helper.assertBlockPresent(OBBlocks.FLAGS.get(DyeColor.WHITE).get(), POS);

		// clicking a flag with a flag places another one on top instead of turning it
		helper.useBlock(POS, player, new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(POS)), Direction.UP, helper.absolutePos(POS), false));
		helper.assertBlockProperty(POS, FlagBlock.ROTATION, 0);
		helper.placeAt(player, player.getMainHandItem(), POS, Direction.UP);
		helper.assertBlockPresent(OBBlocks.FLAGS.get(DyeColor.WHITE).get(), POS.above());

		helper.setBlock(GROUND, Blocks.AIR);
		helper.assertBlockNotPresent(OBBlocks.FLAGS.get(DyeColor.WHITE).get(), POS);
		helper.assertBlockNotPresent(OBBlocks.FLAGS.get(DyeColor.WHITE).get(), POS.above());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void flagsCannotHangFromCeilings(GameTestHelper helper) {
		final BlockPos ceiling = new BlockPos(1, 4, 1);
		helper.setBlock(ceiling, Blocks.STONE);
		final Player player = playerWith(helper, DyeColor.WHITE, 0);
		helper.placeAt(player, player.getMainHandItem(), ceiling, Direction.DOWN);
		helper.assertBlockNotPresent(OBBlocks.FLAGS.get(DyeColor.WHITE).get(), ceiling.below());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void usingFlagSwitchesColor(GameTestHelper helper) {
		final Player player = playerWith(helper, DyeColor.WHITE, 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject());
		final ItemStack stack = player.getMainHandItem();
		helper.assertTrue(stack.is(OBBlocks.FLAGS.get(DyeColor.ORANGE).get().asItem()) && stack.getCount() == 4, "Expected 4 orange flags, got " + stack);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBBlocks.FLAGS.get(DyeColor.BLACK).get()));
		player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject());
		helper.assertTrue(player.getMainHandItem().is(OBBlocks.FLAGS.get(DyeColor.WHITE).get().asItem()), "Expected black to wrap around to white, got " + player.getMainHandItem());
		helper.succeed();
	}
}
