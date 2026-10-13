package openblocks.gametest;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.RotatingElevatorBlock;
import openblocks.common.ElevatorActionHandler;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.network.ElevatorActionPayload;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class ElevatorGameTests {

	private static final BlockPos LOWER = new BlockPos(1, 1, 1);
	private static final BlockPos UPPER = new BlockPos(1, 5, 1);

	/**
	 * Places a player on top of {@code start}, triggers {@code action} and checks that the player ends up on top of {@code expected}
	 * ({@code null}: player must not move).
	 */
	@SuppressWarnings("removal")
	private static void runElevator(GameTestHelper helper, DyeColor upperColor, BlockPos start, ElevatorActionPayload.Action action, @Nullable BlockPos expected) {
		helper.setBlock(LOWER, OBBlocks.ELEVATORS.get(DyeColor.RED).get());
		helper.setBlock(UPPER, OBBlocks.ELEVATORS.get(upperColor).get());

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			final Vec3 startPos = helper.absoluteVec(Vec3.atBottomCenterOf(start.above()));
			player.teleportTo(startPos.x, startPos.y, startPos.z);

			ElevatorActionHandler.onElevatorAction(player, action);

			final double expectedY = expected != null? helper.absolutePos(expected).getY() + 1.1 : startPos.y;
			helper.assertTrue(Math.abs(player.getY() - expectedY) < 0.01, "Expected player at y=" + expectedY + ", got " + player.getY());
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void jumpMovesUp(GameTestHelper helper) {
		runElevator(helper, DyeColor.RED, LOWER, ElevatorActionPayload.Action.JUMP, UPPER);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sneakMovesDown(GameTestHelper helper) {
		runElevator(helper, DyeColor.RED, UPPER, ElevatorActionPayload.Action.SNEAK, LOWER);
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void rotatingElevatorTurnsPlayer(GameTestHelper helper) {
		helper.setBlock(LOWER, OBBlocks.ELEVATORS.get(DyeColor.RED).get());
		helper.setBlock(UPPER, OBBlocks.ROTATING_ELEVATORS.get(DyeColor.RED).get().defaultBlockState().setValue(RotatingElevatorBlock.FACING, Direction.EAST));

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			final Vec3 startPos = helper.absoluteVec(Vec3.atBottomCenterOf(LOWER.above()));
			player.teleportTo(startPos.x, startPos.y, startPos.z);
			player.setYRot(Direction.NORTH.toYRot());

			ElevatorActionHandler.onElevatorAction(player, ElevatorActionPayload.Action.JUMP);

			helper.assertTrue(player.getDirection() == Direction.EAST, "Expected player facing east, got " + player.getDirection());
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void differentColorDoesNotMove(GameTestHelper helper) {
		runElevator(helper, DyeColor.BLUE, LOWER, ElevatorActionPayload.Action.JUMP, null);
	}
}
