package openblocks.common;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Flight of an item shot by the cannon. Replaces legacy {@code TileEntityCannonLogic}, which aimed for a projectile
 * without air drag (its custom item entity never actually ran that physics). The shot is a vanilla item entity, so this
 * solves the vanilla item physics exactly: each tick gravity, then the move, then the drag.
 */
public final class CannonBallistics {

	public static final double GRAVITY = 0.04;
	public static final double DRAG = 0.98;

	// velocities above this are cut off when sent to clients
	private static final double MAX_SPEED = 3.9;

	private static final int MIN_TICKS = 10;
	private static final int MAX_TICKS = 100;

	private CannonBallistics() {}

	/**
	 * Velocity that gets from the origin to the origin plus {@code delta}; longer and higher shots take longer, which
	 * makes higher arcs, like legacy.
	 */
	public static Vec3 velocityTo(Vec3 delta) {
		final double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		int ticks = Mth.clamp((int)Math.round(MIN_TICKS + horizontal * 0.6 + Math.max(delta.y, 0) * 2), MIN_TICKS, MAX_TICKS);
		Vec3 velocity = velocityTo(delta, ticks);
		// far shots: slower and higher, until it fits
		while (ticks < MAX_TICKS && maxComponent(velocity) > MAX_SPEED)
			velocity = velocityTo(delta, ++ticks);
		return velocity;
	}

	/**
	 * Velocity that gets to {@code delta} after exactly {@code ticks} ticks.
	 */
	public static Vec3 velocityTo(Vec3 delta, int ticks) {
		// distance covered with start speed 1: 1 + d + d^2 + ... (T terms)
		final double s = (1 - Math.pow(DRAG, ticks)) / (1 - DRAG);
		// the speed after gravity, u0 = vy - g, follows u(k+1) = d * u(k) - g
		final double vy = (delta.y + GRAVITY * (ticks - s) / (1 - DRAG)) / s + GRAVITY;
		return new Vec3(delta.x / s, vy, delta.z / s);
	}

	private static double maxComponent(Vec3 v) {
		return Math.max(Math.abs(v.x), Math.max(Math.abs(v.y), Math.abs(v.z)));
	}

	/**
	 * Position after the given number of ticks in the air, starting at the origin.
	 */
	public static Vec3 positionAfter(Vec3 velocity, int ticks) {
		Vec3 pos = Vec3.ZERO;
		Vec3 v = velocity;
		for (int i = 0; i < ticks; i++) {
			v = v.subtract(0, GRAVITY, 0);
			pos = pos.add(v);
			v = v.scale(DRAG);
		}
		return pos;
	}
}
