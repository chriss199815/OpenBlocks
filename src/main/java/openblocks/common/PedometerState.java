package openblocks.common;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Port of legacy {@code PedometerHandler.PedometerState}: measurement of one player, kept only on their client (see
 * {@code OBAttachments.PEDOMETER}) and updated while a pedometer is in the hotbar.
 */
public class PedometerState {

	private double totalDistance;
	private long startTicks;
	private Vec3 startPos;
	private Vec3 prevTickPos;
	private long prevTickTime;
	@Nullable
	private Vec3 lastCheckPos;
	private long lastCheckTime;
	@Nullable
	private Data lastResult;
	private boolean running;

	public void reset() {
		running = false;
		totalDistance = 0;
		lastResult = null;
		lastCheckPos = null;
		lastCheckTime = 0;
		prevTickPos = null;
		prevTickTime = 0;
	}

	public void start(Entity entity) {
		lastCheckPos = prevTickPos = startPos = entity.position();
		lastCheckTime = prevTickTime = startTicks = entity.level().getGameTime();
		running = true;
	}

	public void update(Entity entity) {
		final Vec3 currentPosition = entity.position();
		final double distanceSinceLastTick = currentPosition.distanceTo(prevTickPos);
		prevTickPos = currentPosition;

		final long currentTime = entity.level().getGameTime();
		final double ticksSinceLastUpdate = currentTime - prevTickTime;
		prevTickTime = currentTime;

		final double currentSpeed = ticksSinceLastUpdate != 0? distanceSinceLastTick / ticksSinceLastUpdate : 0;
		totalDistance += distanceSinceLastTick;

		final double distanceFromLastCheck = lastCheckPos != null? currentPosition.distanceTo(lastCheckPos) : 0;
		lastResult = new Data(startPos, currentTime - startTicks, totalDistance, currentPosition.distanceTo(startPos),
				distanceFromLastCheck, currentTime - lastCheckTime, currentSpeed);
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * Latest measurement; also starts the next "since last check" interval.
	 */
	@Nullable
	public Data check() {
		lastCheckPos = prevTickPos;
		lastCheckTime = prevTickTime;
		return lastResult;
	}

	public record Data(Vec3 startingPoint, long totalTime, double totalDistance, double straightLineDistance,
			double lastCheckDistance, long lastCheckTime, double currentSpeed) {

		public double averageSpeed() {
			return totalTime != 0? totalDistance / totalTime : 0;
		}

		public double straightLineSpeed() {
			return totalTime != 0? straightLineDistance / totalTime : 0;
		}

		public double lastCheckSpeed() {
			return lastCheckTime != 0? lastCheckDistance / lastCheckTime : 0;
		}
	}
}
