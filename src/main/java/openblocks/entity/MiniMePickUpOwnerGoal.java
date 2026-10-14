package openblocks.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Port of legacy {@code EntityAIPickupPlayer}: walks to its owner and lifts them up. Legacy only went for owners that
 * were already riding something (an inverted check), so it practically never happened; now it is owners on foot.
 */
public class MiniMePickUpOwnerGoal extends Goal {

	private static final double SEARCH_RANGE = 10;
	private static final double PICKUP_DISTANCE = 1.5;
	private static final int REPATH_INTERVAL = 10;

	private final MiniMe miniMe;

	@Nullable
	private Player target;

	private int repathDelay;

	public MiniMePickUpOwnerGoal(MiniMe miniMe) {
		this.miniMe = miniMe;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (miniMe.getPickupCooldown() > 0 || miniMe.isVehicle() || !miniMe.getNavigation().isDone()) return false;

		for (Player player : miniMe.level().getEntitiesOfClass(Player.class, miniMe.getBoundingBox().inflate(SEARCH_RANGE))) {
			if (canPickUp(player)) {
				target = player;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return miniMe.isAlive() && !miniMe.isVehicle() && canPickUp(target);
	}

	@Override
	public void start() {
		repathDelay = 0;
	}

	@Override
	public void stop() {
		miniMe.getNavigation().stop();
		target = null;
	}

	@Override
	public void tick() {
		if (target == null) return;
		miniMe.getLookControl().setLookAt(target);

		if (miniMe.distanceTo(target) < PICKUP_DISTANCE) {
			target.startRiding(miniMe);
		} else if (--repathDelay <= 0) {
			repathDelay = REPATH_INTERVAL;
			miniMe.getNavigation().moveTo(target, 1.0);
		}
	}

	private boolean canPickUp(@Nullable Player player) {
		return player != null && player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isShiftKeyDown()
				&& miniMe.isOwner(player) && miniMe.distanceTo(player) <= SEARCH_RANGE * 1.5;
	}
}
