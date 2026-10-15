package openblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.client.HeightMapCache;
import openblocks.common.HeightMapData;

/**
 * Server to client: contents of a height map. Replaces legacy {@code MapDataManager.MapDataResponseEvent}.
 */
public record HeightMapPayload(int mapId, HeightMapData data) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<HeightMapPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("height_map"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HeightMapPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HeightMapPayload::mapId,
			HeightMapData.STREAM_CODEC, HeightMapPayload::data,
			HeightMapPayload::new);

	@Override
	public CustomPacketPayload.Type<HeightMapPayload> type() {
		return TYPE;
	}

	// only called on the client
	public static void handle(HeightMapPayload payload, IPayloadContext context) {
		HeightMapCache.put(payload.mapId(), payload.data());
	}
}
