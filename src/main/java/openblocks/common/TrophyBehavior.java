package openblocks.common;

import net.minecraft.world.entity.player.Player;
import openblocks.block.entity.TrophyBlockEntity;

/**
 * Port of legacy {@code ITrophyBehavior}: what clicking a trophy does. The tick behaviour was empty for all trophies and is gone.
 */
@FunctionalInterface
public interface TrophyBehavior {

	/**
	 * @return ticks until the trophy can do this again
	 */
	int activate(TrophyBlockEntity trophy, Player player);
}
