package openblocks.common;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import openblocks.entity.Magnet;
import openblocks.registry.OBAttachments;

/**
 * Port of legacy {@code CraneRegistry.Data}: the crane of a player wearing the crane backpack. Kept on both sides, never
 * saved or synced; only the server changes the length, clients follow the magnet's position.
 */
public class CraneState {

	public static final double MIN_LENGTH = 0.25;
	public static final double MAX_LENGTH = 10;
	private static final double LENGTH_DELTA = 0.1;
	public static final double ARM_RADIUS = 2.0;
	private static final int TOGGLE_COOLDOWN = 5;

	public boolean extending;
	public double length = MIN_LENGTH;
	@Nullable
	private Magnet magnet;
	private long lastToggle = -TOGGLE_COOLDOWN - 1;

	public static CraneState of(Player player) {
		return player.getData(OBAttachments.CRANE);
	}

	public void updateLength() {
		if (extending) length = Math.min(length + LENGTH_DELTA, MAX_LENGTH);
		else length = Math.max(length - LENGTH_DELTA, MIN_LENGTH);
	}

	@Nullable
	public Magnet getMagnet() {
		return magnet != null && !magnet.isRemoved()? magnet : null;
	}

	public void setMagnet(Magnet magnet) {
		this.magnet = magnet;
	}

	/**
	 * Like legacy, clicks less than five ticks apart count once.
	 */
	public boolean tryToggle(long gameTime) {
		if (gameTime - lastToggle <= TOGGLE_COOLDOWN) return false;
		lastToggle = gameTime;
		return true;
	}
}
