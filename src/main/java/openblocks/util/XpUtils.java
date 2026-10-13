package openblocks.util;

import net.minecraft.world.entity.player.Player;

/**
 * Port of the XP helpers from OpenModsLib {@code EnchantmentUtils}.
 */
public class XpUtils {

	/**
	 * Minecraft doesn't keep {@code totalExperience} accurate, so it is calculated from level and progress.
	 */
	public static int getPlayerXp(Player player) {
		// rounded, since experienceProgress is a float and truncating would lose a point (e.g. 10/15 -> 9.9999)
		return getExperienceForLevel(player.experienceLevel) + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
	}

	public static int getExperienceForLevel(int level) {
		if (level <= 0) return 0;
		if (level <= 15) return sum(level, 7, 2);
		if (level <= 30) return 315 + sum(level - 15, 37, 5);
		return 1395 + sum(level - 30, 112, 9);
	}

	private static int sum(int n, int a0, int d) {
		return n * (2 * a0 + (n - 1) * d) / 2;
	}
}
