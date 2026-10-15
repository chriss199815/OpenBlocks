package openblocks.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.client.HeightMapCache;

/**
 * Server to client: height maps that changed; the client asks again for the ones it knows. Replaces legacy
 * {@code MapDataManager.MapUpdatesEvent}.
 */
public record HeightMapUpdatesPayload(List<Integer> mapIds) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<HeightMapUpdatesPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("height_map_updates"));

	public static final StreamCodec<ByteBuf, HeightMapUpdatesPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())
			.map(HeightMapUpdatesPayload::new, HeightMapUpdatesPayload::mapIds);

	@Override
	public CustomPacketPayload.Type<HeightMapUpdatesPayload> type() {
		return TYPE;
	}

	// only called on the client
	public static void handle(HeightMapUpdatesPayload payload, IPayloadContext context) {
		HeightMapCache.onUpdates(payload.mapIds());
	}
}
