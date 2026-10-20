package openblocks.integration.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import openblocks.OBConfig;
import openblocks.entity.Magnet;

/**
 * Legacy {@code MagnetControlAdapter}. Positions are relative to the turtle: x to its right, y up, z forward (legacy mixed
 * up its axes). Like legacy, the magnet costs 5 fuel to deploy and then 1 fuel a second, 2 while carrying something.
 */
class MagnetPeripheral implements IPeripheral, Magnet.Anchor {

	private static final int DEPLOY_FUEL = 5;
	private static final int FUEL_INTERVAL = 20;

	private final ITurtleAccess turtle;
	private final TurtleSide side;

	// attach and detach come from computer threads
	private final AtomicInteger attachedComputers = new AtomicInteger();
	@Nullable
	private Magnet magnet;
	// relative to the turtle, see class comment
	private Vec3 target = Vec3.ZERO;
	private int fuelTicks;

	MagnetPeripheral(ITurtleAccess turtle, TurtleSide side) {
		this.turtle = turtle;
		this.side = side;
	}

	@Override
	public String getType() {
		return "openblocks_magnet";
	}

	@Override
	public void attach(IComputerAccess computer) {
		attachedComputers.incrementAndGet();
	}

	@Override
	public void detach(IComputerAccess computer) {
		attachedComputers.decrementAndGet();
	}

	@Override
	public boolean equals(@Nullable IPeripheral other) {
		return other == this;
	}

	@LuaFunction(mainThread = true)
	public final void activate() throws LuaException {
		if (activeMagnet() != null) throw new LuaException("Magnet already active");
		if (!(turtle.getLevel() instanceof ServerLevel level)) throw new LuaException("Turtle is unloaded");
		if (!level.isEmptyBlock(turtle.getPosition().relative(sideDirection()))) throw new LuaException("Can't deploy magnet");
		if (!turtle.consumeFuel(DEPLOY_FUEL)) throw new LuaException("No fuel");

		target = new Vec3(side == TurtleSide.LEFT? -1 : 1, 0, 0);
		fuelTicks = 0;
		magnet = new Magnet(level, this);
		level.addFreshEntity(magnet);
		magnet.playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
	}

	@LuaFunction(mainThread = true)
	public final void deactivate() throws LuaException {
		final Magnet magnet = requireMagnet();
		final double range = OBConfig.CRANE.turtleMagnetDeactivateRange.get();
		if (magnet.getBoundingBox().getCenter().distanceToSqr(turtleCenter()) > range * range) throw new LuaException("Magnet too far");
		despawn();
	}

	@LuaFunction(mainThread = true)
	public final void setTarget(double x, double y, double z) throws LuaException {
		requireMagnet();
		final double range = OBConfig.CRANE.turtleMagnetRange.get();
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new LuaException("Invalid target");
		if (Math.abs(x) > range || Math.abs(y) > range || Math.abs(z) > range) throw new LuaException("Target out of range");
		target = new Vec3(x, y, z);
	}

	@LuaFunction(value = "getTarget", mainThread = true)
	public final MethodResult getRelativeTarget() throws LuaException {
		requireMagnet();
		return MethodResult.of(target.x, target.y, target.z);
	}

	/**
	 * Where the magnet is now, relative to the turtle.
	 */
	@LuaFunction(mainThread = true)
	public final MethodResult getPosition() throws LuaException {
		final Vec3 position = toTurtle(requireMagnet().getBoundingBox().getCenter());
		return MethodResult.of(position.x, position.y, position.z);
	}

	@LuaFunction(value = { "getDistanceToTarget", "distance" }, mainThread = true)
	public final MethodResult getDistanceToTarget() throws LuaException {
		final Vec3 distance = toTurtle(requireMagnet().getBoundingBox().getCenter()).subtract(target);
		return MethodResult.of(distance.x, distance.y, distance.z);
	}

	@LuaFunction(mainThread = true)
	public final boolean isAboveEntity() throws LuaException {
		return requireMagnet().isAboveTarget();
	}

	/**
	 * Grabs the entity or block below the magnet, or releases what it carries.
	 */
	@LuaFunction(value = { "toggleMagnet", "toggle" }, mainThread = true)
	public final boolean toggleMagnet() throws LuaException {
		return requireMagnet().toggle();
	}

	@LuaFunction(mainThread = true)
	public final boolean isGrabbing() throws LuaException {
		return requireMagnet().isLocked();
	}

	void tick() {
		final Magnet magnet = activeMagnet();
		if (magnet == null || ++fuelTicks < FUEL_INTERVAL) return;
		fuelTicks = 0;
		if (!turtle.consumeFuel(magnet.isLocked()? 2 : 1)) despawn();
	}

	@Override
	public boolean isValid() {
		return attachedComputers.get() > 0 && !turtle.isRemoved() && magnet != null && magnet.level() == turtle.getLevel();
	}

	@Override
	public Vec3 getTarget() {
		final Direction forward = turtle.getDirection();
		final Direction right = forward.getClockWise();
		return turtleCenter()
				.add(Vec3.atLowerCornerOf(right.getNormal()).scale(target.x))
				.add(0, target.y, 0)
				.add(Vec3.atLowerCornerOf(forward.getNormal()).scale(target.z));
	}

	@Override
	public Player getActor(ServerLevel level) {
		return turtle.getOwningPlayer() != null? FakePlayerFactory.get(level, turtle.getOwningPlayer()) : FakePlayerFactory.getMinecraft(level);
	}

	private Vec3 toTurtle(Vec3 world) {
		final Direction forward = turtle.getDirection();
		final Direction right = forward.getClockWise();
		final Vec3 delta = world.subtract(turtleCenter());
		return new Vec3(delta.dot(Vec3.atLowerCornerOf(right.getNormal())), delta.y, delta.dot(Vec3.atLowerCornerOf(forward.getNormal())));
	}

	private Vec3 turtleCenter() {
		return Vec3.atCenterOf(turtle.getPosition());
	}

	private Direction sideDirection() {
		return side == TurtleSide.LEFT? turtle.getDirection().getCounterClockWise() : turtle.getDirection().getClockWise();
	}

	@Nullable
	private Magnet activeMagnet() {
		if (magnet != null && !magnet.isAlive()) magnet = null;
		return magnet;
	}

	private Magnet requireMagnet() throws LuaException {
		final Magnet magnet = activeMagnet();
		if (magnet == null) throw new LuaException("Magnet not active");
		return magnet;
	}

	private void despawn() {
		if (magnet == null) return;
		magnet.playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
		magnet.ejectPassengers();
		magnet.discard();
		magnet = null;
	}
}
