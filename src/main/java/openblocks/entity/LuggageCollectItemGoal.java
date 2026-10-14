package openblocks.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Port of legacy {@code EntityAICollectItem}: walks to the closest item on the ground within 10 blocks and eats it.
 */
public class LuggageCollectItemGoal extends Goal {

	private static final double SEARCH_RANGE = 10;
	private static final double SPEED = 0.4;

	private final Luggage luggage;

	@Nullable
	private ItemEntity target;

	public LuggageCollectItemGoal(Luggage luggage) {
		this.luggage = luggage;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!luggage.getNavigation().isDone()) return false;

		ItemEntity closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (ItemEntity item : luggage.level().getEntitiesOfClass(ItemEntity.class, luggage.getBoundingBox().inflate(SEARCH_RANGE))) {
			if (!item.isAlive() || !item.onGround() || item.isInWater()) continue;
			final double distance = item.distanceTo(luggage);
			if (distance < closestDistance && luggage.canCollect(item.getItem())) {
				closest = item;
				closestDistance = distance;
			}
		}

		target = closest;
		return target != null;
	}

	@Override
	public boolean canContinueToUse() {
		return luggage.isAlive() && !luggage.getNavigation().isDone() && target != null && target.isAlive();
	}

	@Override
	public void start() {
		// accuracy 0: moveTo(x, y, z) would stop a block short, out of reach
		if (target != null) luggage.getNavigation().moveTo(luggage.getNavigation().createPath(target, 0), SPEED);
	}

	@Override
	public void stop() {
		luggage.getNavigation().stop();
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target != null && target.isAlive() && luggage.distanceTo(target) < 1.0) luggage.collect(target);
	}
}
