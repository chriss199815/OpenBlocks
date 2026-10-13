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
	}
}
