package openblocks.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.common.HeightMaps;

/**
 * Client to server: asks for the contents of height maps. Replaces legacy {@code MapDataManager.MapDataRequestEvent}.
 */
public record HeightMapRequestPayload(List<Integer> mapIds) implements CustomPacketPayload {

	private static final int MAX_MAPS = 64;

	public static final CustomPacketPayload.Type<HeightMapRequestPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("height_map_request"));

	public static final StreamCodec<ByteBuf, HeightMapRequestPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_MAPS))
			.map(HeightMapRequestPayload::new, HeightMapRequestPayload::mapIds);

	@Override
	public CustomPacketPayload.Type<HeightMapRequestPayload> type() {
		return TYPE;
	}

	public static void handle(HeightMapRequestPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) HeightMaps.sendMaps(player, payload.mapIds());
	}
}
