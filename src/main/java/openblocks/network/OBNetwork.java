package openblocks.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Replacement for OpenModsLib RPC and network events: one {@code CustomPacketPayload} per action.
 */
public class OBNetwork {

	private static final String PROTOCOL_VERSION = "1";

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
		registrar.playToServer(ElevatorActionPayload.TYPE, ElevatorActionPayload.STREAM_CODEC, ElevatorActionPayload::handle);
		registrar.playToServer(BrickDropPayload.TYPE, BrickDropPayload.STREAM_CODEC, BrickDropPayload::handle);
		registrar.playToServer(HeightMapRequestPayload.TYPE, HeightMapRequestPayload.STREAM_CODEC, HeightMapRequestPayload::handle);
		registrar.playToClient(HeightMapPayload.TYPE, HeightMapPayload.STREAM_CODEC, HeightMapPayload::handle);
		registrar.playToClient(HeightMapUpdatesPayload.TYPE, HeightMapUpdatesPayload.STREAM_CODEC, HeightMapUpdatesPayload::handle);
	}
}
