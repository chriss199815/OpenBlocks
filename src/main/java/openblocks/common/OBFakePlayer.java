package openblocks.common;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The fake player machines act as; replaces OpenModsLib {@code FakePlayerPool}.
 */
public final class OBFakePlayer {

	private static final GameProfile PROFILE = new GameProfile(UUID.fromString("2c2d0e5e-1a0b-4c9b-9a47-0b1f5e3b7a10"), "[OpenBlocks]");

	private OBFakePlayer() {}

	public static FakePlayer get(ServerLevel level) {
		return FakePlayerFactory.get(level, PROFILE);
	}
}
