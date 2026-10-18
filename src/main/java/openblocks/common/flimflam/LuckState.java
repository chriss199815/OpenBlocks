package openblocks.common.flimflam;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import openblocks.registry.OBAttachments;

/**
 * Port of the legacy {@code Luck} capability: the player's luck, lowered by flim flam attacks, raised by flim flams
 * happening. Like legacy, only the luck is saved and it is lost on death.
 */
public class LuckState {

	public static final Codec<LuckState> CODEC = Codec.INT.xmap(LuckState::new, state -> state.luck);

	int luck;

	int cooldown;

	boolean forceNext;

	public LuckState() {}

	private LuckState(int luck) {
		this.luck = luck;
	}

	public static LuckState of(Player player) {
		return player.getData(OBAttachments.LUCK);
	}

	public int getLuck() {
		return luck;
	}

	public int modifyLuck(int amount) {
		luck += amount;
		return luck;
	}
}
