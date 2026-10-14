package openblocks.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.common.BrickDrops;

/**
 * Client to server: player pressed the "Be silly" key. Replaces the legacy {@code PlayerActionEvent} of type {@code BOO}.
 */
public record BrickDropPayload() implements CustomPacketPayload {

	public static final BrickDropPayload INSTANCE = new BrickDropPayload();

	public static final CustomPacketPayload.Type<BrickDropPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("brick_drop"));

	public static final StreamCodec<ByteBuf, BrickDropPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<BrickDropPayload> type() {
		return TYPE;
	}

	public static void handle(BrickDropPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) BrickDrops.dropBrick(player);
	}
}
