package openblocks.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.common.ElevatorActionHandler;

/**
 * Client to server: the player started jumping or sneaking. Replaces the legacy {@code ElevatorActionEvent}.
 */
public record ElevatorActionPayload(Action action) implements CustomPacketPayload {

	public enum Action {
		JUMP, SNEAK
	}

	public static final CustomPacketPayload.Type<ElevatorActionPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("elevator_action"));

	public static final StreamCodec<ByteBuf, ElevatorActionPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(
			jump -> new ElevatorActionPayload(jump? Action.JUMP : Action.SNEAK),
			payload -> payload.action == Action.JUMP);

	@Override
	public CustomPacketPayload.Type<ElevatorActionPayload> type() {
		return TYPE;
	}

	public static void handle(ElevatorActionPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) ElevatorActionHandler.onElevatorAction(player, payload.action);
	}
}
