package openblocks.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * A flim flam: something weird that happens to a player with bad luck (see {@link FlimFlamRegistry}).
 */
@FunctionalInterface
public interface IFlimFlamAction {

	/**
	 * @return false if it could not happen (e.g. no mob around), another one is tried then
	 */
	boolean execute(ServerPlayer target);
}
