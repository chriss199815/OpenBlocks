package openblocks.common;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.OBConfig;
import openblocks.api.ElevatorCheckEvent;
import openblocks.api.IElevatorBlock;
import openblocks.network.ElevatorActionPayload;
import openblocks.registry.OBSounds;
import openblocks.util.XpUtils;

public class ElevatorActionHandler {

	private record SearchResult(BlockPos pos, @Nullable Direction rotation) {}

	private static boolean canTeleportPlayer(Level level, BlockPos pos) {
		if (level.isEmptyBlock(pos)) return true;

		if (!OBConfig.ELEVATOR.irregularBlocksArePassable.get()) return false;
		final VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
		return shape.isEmpty() || shape.bounds().getSize() < 0.7;
	}

	private static boolean canTeleportPlayer(ServerPlayer player, Level level, BlockPos pos) {
		final AABB aabb = player.getBoundingBox();
		final int blockHeight = Math.max(1, Mth.ceil(aabb.getYsize()));

		for (int dy = 0; dy < blockHeight; dy++)
			if (!canTeleportPlayer(level, pos.above(dy))) return false;

		return true;
	}

	private static ElevatorCheckEvent checkIsElevator(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
		final ElevatorCheckEvent evt = new ElevatorCheckEvent(level, pos, state, player);

		if (state.getBlock() instanceof IElevatorBlock elevatorBlock) {
			evt.setColor(elevatorBlock.getColor(level, pos, state));
			evt.setRotation(elevatorBlock.getRotation(level, pos, state));
		}

		ElevatorBlockRules.configureEvent(evt);

		return NeoForge.EVENT_BUS.post(evt);
	}

	@Nullable
	private static SearchResult findLevel(ServerPlayer player, Level level, DyeColor thisColor, BlockPos pos, Direction searchDirection) {
		int blocksInTheWay = 0;
		final BlockPos.MutableBlockPos searchPos = pos.mutable();
		for (int i = 0; i < OBConfig.ELEVATOR.travelDistance.get(); i++) {
			searchPos.move(searchDirection);
			if (level.isOutsideBuildHeight(searchPos) || !level.isLoaded(searchPos)) break;
			if (level.isEmptyBlock(searchPos)) continue;

			final BlockState blockState = level.getBlockState(searchPos);
			final ElevatorCheckEvent elevatorCheckResult = checkIsElevator(player, level, searchPos, blockState);

			if (elevatorCheckResult.isElevator()
					&& elevatorCheckResult.getColor() == thisColor
					&& canTeleportPlayer(player, level, searchPos.above()))
				return new SearchResult(searchPos.immutable(), elevatorCheckResult.getRotation());

			if (!OBConfig.ELEVATOR.ignoreAllBlocks.get()) {
				switch (ElevatorBlockRules.getActionForBlock(level, searchPos, blockState)) {
					case ABORT:
						return null;
					case IGNORE:
						continue;
					case INCREMENT:
					default:
						break;
				}

				if (++blocksInTheWay > OBConfig.ELEVATOR.maxPassThrough.get()) break;
			}
		}

		return null;
	}

	private static void activate(ServerPlayer player, ServerLevel level, DyeColor color, BlockPos pos, Direction dir) {
		final SearchResult result = findLevel(player, level, color, pos, dir);
		if (result == null || !checkXpCost(player, result)) return;

		final float yaw = result.rotation != null? result.rotation.toYRot() : player.getYRot();
		final BlockPos target = result.pos;
		if (OBConfig.ELEVATOR.centerOnBlock.get()) player.teleportTo(level, target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5, yaw, player.getXRot());
		else player.teleportTo(level, player.getX(), target.getY() + 1.1, player.getZ(), yaw, player.getXRot());

		level.playSound(null, player.blockPosition(), OBSounds.ELEVATOR_ACTIVATE.get(), SoundSource.BLOCKS, 1, 1);
	}

	private static boolean checkXpCost(ServerPlayer player, SearchResult result) {
		final double ratio = OBConfig.ELEVATOR.xpDrainRatio.get();
		if (ratio == 0 || player.getAbilities().instabuild) return true;

		final int distance = (int)Math.abs(player.getY() - result.pos.getY());
		final int neededXp = Mth.ceil(ratio * distance);
		if (XpUtils.getPlayerXp(player) < neededXp) return false;

		player.giveExperiencePoints(-neededXp);
		return true;
	}

	public static void onElevatorAction(ServerPlayer player, ElevatorActionPayload.Action action) {
		if (player.isPassenger()) return;

		final ServerLevel level = player.serverLevel();
		final BlockPos blockPos = BlockPos.containing(player.getX(), player.getBoundingBox().minY, player.getZ()).below();

		final ElevatorCheckEvent elevatorCheckResult = checkIsElevator(player, level, blockPos, level.getBlockState(blockPos));
		if (!elevatorCheckResult.isElevator()) return;

		final Direction dir = action == ElevatorActionPayload.Action.JUMP? Direction.UP : Direction.DOWN;
		activate(player, level, elevatorCheckResult.getColor(), blockPos, dir);
	}
}
