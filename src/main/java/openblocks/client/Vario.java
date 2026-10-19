package openblocks.client;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import openblocks.common.HangGliderHandler;
import org.lwjgl.glfw.GLFW;

/**
 * Port of legacy {@code Vario} and the vario part of {@code KeyInputHandler}: while the local player glides, a tone tells the
 * vertical speed - low when sinking, high and beeping when rising. Instead of the watchdog thread the client tick stops the
 * sound once the glider is gone. Keys are always registered (legacy only with thermals enabled), on/off and volume are stored
 * in the client config.
 */
public class Vario {

	public static final int VOLUME_MIN = 2;
	public static final int VOLUME_MAX = 20;
	public static final int VOLUME_DEFAULT = 8;
	private static final int VOLUME_STEP = 2;

	private static final int TICKS_PER_UPDATE = 4;

	private static final int FREQ_MIN = 300;
	private static final int FREQ_AVG = 600;
	private static final int FREQ_MAX = 2000;

	private static final int BEEP_RATE_AVG = 4;
	private static final int BEEP_RATE_MAX = 24;

	public static final KeyMapping SWITCH = new KeyMapping("openblocks.keybind.vario_switch", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "openblocks.keybind.category");
	public static final KeyMapping VOLUME_UP = new KeyMapping("openblocks.keybind.vario_vol_up", KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, "openblocks.keybind.category");
	public static final KeyMapping VOLUME_DOWN = new KeyMapping("openblocks.keybind.vario_vol_down", KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, "openblocks.keybind.category");

	@Nullable
	private static VarioSound sound;
	private static int ticksSinceUpdate;
	private static double verticalMotionSinceUpdate;

	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(SWITCH);
		event.register(VOLUME_UP);
		event.register(VOLUME_DOWN);
	}

	/**
	 * Legacy volume 2-20 scaled the tone amplitude; now the full amplitude is the legacy maximum.
	 */
	public static float getVolumeFactor() {
		return OBClientConfig.HANG_GLIDER.varioVolume.getAsInt() / (float)VOLUME_MAX;
	}

	public static void onClientTick(ClientTickEvent.Post event) {
		final LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			stop();
			return;
		}

		handleKeys(player);

		if (!OBClientConfig.HANG_GLIDER.varioEnabled.getAsBoolean() || !HangGliderHandler.hasGlider(player)) {
			stop();
			return;
		}

		final Minecraft minecraft = Minecraft.getInstance();
		if (sound == null || !minecraft.getSoundManager().isActive(sound)) {
			sound = new VarioSound();
			minecraft.getSoundManager().play(sound);
		}

		if (HangGliderHandler.isDeployed(player)) {
			verticalMotionSinceUpdate += player.getY() - player.yo;
			if (++ticksSinceUpdate >= TICKS_PER_UPDATE) {
				update(sound, verticalMotionSinceUpdate / ticksSinceUpdate);
				resetAverage();
			}
		} else {
			sound.setFrequencies(0, 0);
			resetAverage();
		}
	}

	private static void handleKeys(LocalPlayer player) {
		while (SWITCH.consumeClick()) {
			final boolean enabled = !OBClientConfig.HANG_GLIDER.varioEnabled.getAsBoolean();
			OBClientConfig.HANG_GLIDER.varioEnabled.set(enabled);
			OBClientConfig.HANG_GLIDER.varioEnabled.save();
			player.displayClientMessage(Component.translatable(enabled? "openblocks.misc.vario.enabled" : "openblocks.misc.vario.disabled"), true);
		}

		while (VOLUME_UP.consumeClick())
			changeVolume(player, VOLUME_STEP);

		while (VOLUME_DOWN.consumeClick())
			changeVolume(player, -VOLUME_STEP);
	}

	private static void changeVolume(LocalPlayer player, int delta) {
		final int volume = Math.clamp(OBClientConfig.HANG_GLIDER.varioVolume.getAsInt() + delta, VOLUME_MIN, VOLUME_MAX);
		OBClientConfig.HANG_GLIDER.varioVolume.set(volume);
		OBClientConfig.HANG_GLIDER.varioVolume.save();
		player.displayClientMessage(Component.translatable("openblocks.misc.vario.volume", volume / VOLUME_STEP, VOLUME_MAX / VOLUME_STEP), true);
	}

	private static void update(VarioSound sound, double vspeed) {
		if (vspeed <= 0) {
			vspeed = Math.max(HangGliderHandler.VSPEED_MIN, vspeed);
			final double freq = (vspeed - HangGliderHandler.VSPEED_MIN) / Math.abs(HangGliderHandler.VSPEED_MIN) * (FREQ_AVG - FREQ_MIN) + FREQ_MIN;
			sound.setFrequencies(freq, 0);
		} else {
			vspeed = Math.min(HangGliderHandler.VSPEED_MAX, vspeed);
			final double freq = vspeed / HangGliderHandler.VSPEED_MAX * (FREQ_MAX - FREQ_AVG) + FREQ_AVG;
			final double beepFreq = vspeed / HangGliderHandler.VSPEED_MAX * (BEEP_RATE_MAX - BEEP_RATE_AVG) + BEEP_RATE_AVG;
			sound.setFrequencies(freq, beepFreq);
		}
	}

	private static void resetAverage() {
		ticksSinceUpdate = 0;
		verticalMotionSinceUpdate = 0;
	}

	private static void stop() {
		if (sound != null) {
			Minecraft.getInstance().getSoundManager().stop(sound);
			sound = null;
		}
		resetAverage();
	}
}
