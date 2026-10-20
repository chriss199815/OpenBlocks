package openblocks.integration.computercraft;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import openblocks.registry.OBBlockEntities;

class OBPeripherals {

	static void register(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(PeripheralCapability.get(), OBBlockEntities.CANNON.get(), (cannon, side) -> new CannonPeripheral(cannon));
		event.registerBlockEntity(PeripheralCapability.get(), OBBlockEntities.GUIDE.get(), (guide, side) -> new GuidePeripheral(guide));
		event.registerBlockEntity(PeripheralCapability.get(), OBBlockEntities.BUILDER_GUIDE.get(), (guide, side) -> new GuidePeripheral(guide));
		event.registerBlockEntity(PeripheralCapability.get(), OBBlockEntities.PROJECTOR.get(), (projector, side) -> new ProjectorPeripheral(projector));
		event.registerBlockEntity(PeripheralCapability.get(), OBBlockEntities.DONATION_STATION.get(), (station, side) -> new DonationStationPeripheral(station));
		event.registerBlockEntity(PeripheralCapability.get(), OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), (highlighter, side) -> new VillageHighlighterPeripheral(highlighter));
	}
}
