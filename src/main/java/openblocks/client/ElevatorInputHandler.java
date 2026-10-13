package openblocks.client;

import net.minecraft.client.player.Input;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import openblocks.network.ElevatorActionPayload;

/**
 * Replaces OpenModsLib {@code PlayerMovementManager}: sends an elevator action when jump or sneak is pressed.
 */
public class ElevatorInputHandler {

	private static boolean wasJumping;
	private static boolean wasSneaking;

	public static void onMovementInput(MovementInputUpdateEvent event) {
		final Input input = event.getInput();
		if (input.jumping && !wasJumping) PacketDistributor.sendToServer(new ElevatorActionPayload(ElevatorActionPayload.Action.JUMP));
		if (input.shiftKeyDown && !wasSneaking) PacketDistributor.sendToServer(new ElevatorActionPayload(ElevatorActionPayload.Action.SNEAK));

		wasJumping = input.jumping;
		wasSneaking = input.shiftKeyDown;
	}
}
