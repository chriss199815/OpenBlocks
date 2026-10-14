package openblocks.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import openblocks.network.BrickDropPayload;
import org.lwjgl.glfw.GLFW;

/**
 * Port of the brick part of legacy {@code KeyInputHandler}: the "Be silly" key (B). Always registered, since whether brick
 * drops are enabled is up to the server; while a screen is open only with {@code tomfoolery.doItWhileTyping}.
 */
public class BrickDropInputHandler {

	public static final KeyMapping DROP_BRICK = new KeyMapping("openblocks.keybind.drop_brick", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "openblocks.keybind.category");

	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(DROP_BRICK);
	}

	public static void onKey(InputEvent.Key event) {
		if (event.getAction() != GLFW.GLFW_PRESS || !DROP_BRICK.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))) return;

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getConnection() == null) return;
		if (minecraft.screen != null && !OBClientConfig.TOMFOOLERY.doItWhileTyping.getAsBoolean()) return;

		PacketDistributor.sendToServer(BrickDropPayload.INSTANCE);
	}
}
