package openblocks.integration.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import openblocks.block.entity.CannonBlockEntity;

/**
 * Legacy {@code AdapterCannon}: sets the angles and speed the item cannon shoots at (the pointer aims by target block instead).
 */
class CannonPeripheral extends BlockEntityPeripheral<CannonBlockEntity> {

	// legacy had no limit; clients only get item motion up to 3.9 per axis
	private static final double MAX_SPEED = 3.9;

	CannonPeripheral(CannonBlockEntity cannon) {
		super(cannon, "openblocks_cannon");
	}

	@LuaFunction(mainThread = true)
	public final void setYaw(double yaw) throws LuaException {
		checkFinite(yaw, "yaw");
		blockEntity.setAim(yaw, blockEntity.getAimPitch(), blockEntity.getSpeed());
	}

	@LuaFunction(mainThread = true)
	public final void setPitch(double pitch) throws LuaException {
		checkFinite(pitch, "pitch");
		// straight up or down would lose the yaw
		if (pitch <= -90 || pitch >= 90) throw new LuaException("Pitch must be between -90 and 90 (exclusive)");
		blockEntity.setAim(blockEntity.getAimYaw(), pitch, blockEntity.getSpeed());
	}

	@LuaFunction(mainThread = true)
	public final void setSpeed(double speed) throws LuaException {
		checkFinite(speed, "speed");
		// the angles are kept in the velocity, so it can't be zero
		if (speed <= 0 || speed > MAX_SPEED) throw new LuaException("Speed must be above 0 and at most " + MAX_SPEED);
		blockEntity.setAim(blockEntity.getAimYaw(), blockEntity.getAimPitch(), speed);
	}

	@LuaFunction(mainThread = true)
	public final MethodResult getAim() {
		return MethodResult.of(blockEntity.getAimYaw(), blockEntity.getAimPitch(), blockEntity.getSpeed());
	}

	private static void checkFinite(double value, String name) throws LuaException {
		if (!Double.isFinite(value)) throw new LuaException("Invalid " + name);
	}
}
