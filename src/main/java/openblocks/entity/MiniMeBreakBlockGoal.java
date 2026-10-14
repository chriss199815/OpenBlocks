package openblocks.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Port of legacy {@code EntityAIBreakBlock}: every few ticks looks for a small flower or torch nearby, walks there and
 * breaks it (with drops). Follows the mob griefing rule instead of acting as a fake player.
 */
public class MiniMeBreakBlockGoal extends Goal {

	// legacy used 1, which the path often ends just outside of
	private static final double REACH_SQR = 1.5;

	private final MiniMe miniMe;
	private final int tickOffset;

	@Nullable
	private BlockPos target;

	public MiniMeBreakBlockGoal(MiniMe miniMe) {
		this.miniMe = miniMe;
		this.tickOffset = miniMe.getRandom().nextInt(10);
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!miniMe.getNavigation().isDone() || (miniMe.tickCount + tickOffset) % 4 != 0) return false;
		if (!EventHooks.canEntityGrief(miniMe.level(), miniMe)) return false;

		for (int i = 0; i < 20; i++) {
			final BlockPos pos = miniMe.blockPosition().offset(miniMe.getRandom().nextInt(16) - 8, miniMe.getRandom().nextInt(3) - 1, miniMe.getRandom().nextInt(16) - 8);
			if (canBreak(pos)) {
				target = pos;
				return true;
			}
		}
		return false;
	}

	// unlike legacy, keeps going when the path ended right at the block, so it can still break it
	@Override
	public boolean canContinueToUse() {
		return miniMe.isAlive() && target != null && canBreak(target) && (!miniMe.getNavigation().isDone() || isInReach(target));
	}

	@Override
	public void start() {
		if (target != null) miniMe.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
	}

	@Override
	public void stop() {
		target = null;
	}

	@Override
	public void tick() {
		if (target != null && canBreak(target) && isInReach(target)) {
			miniMe.level().destroyBlock(target, true, miniMe);
			target = null;
		}
	}

	private boolean isInReach(BlockPos pos) {
		return pos.distToCenterSqr(miniMe.position()) < REACH_SQR;
	}

	private boolean canBreak(BlockPos pos) {
		final BlockState state = miniMe.level().getBlockState(pos);
		return state.is(BlockTags.SMALL_FLOWERS) || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
	}
}
