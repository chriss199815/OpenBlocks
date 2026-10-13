package openblocks.registry;

import net.minecraft.world.level.GameRules;

/**
 * Game rules have no registry; they must be registered before any world loads, so {@link #init()} is called from the mod constructor.
 */
public class OBGameRules {

	// legacy name, so existing commands and server setups keep working
	public static final GameRules.Key<GameRules.BooleanValue> SPAWN_GRAVES = GameRules.register("openblocks:spawn_graves", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));

	public static void init() {
		// loading the class registers the rules
	}
}
