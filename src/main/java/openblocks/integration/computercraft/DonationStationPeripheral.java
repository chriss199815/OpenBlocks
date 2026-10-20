package openblocks.integration.computercraft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import java.util.Arrays;
import java.util.List;
import openblocks.block.entity.DonationStationBlockEntity;
import openblocks.common.DonationInfo;

/**
 * Legacy {@code AdapterDonationStation}: which mod added the item in the station, and who made it. Also returns the link the
 * GUI offers.
 */
class DonationStationPeripheral extends BlockEntityPeripheral<DonationStationBlockEntity> {

	DonationStationPeripheral(DonationStationBlockEntity station) {
		super(station, "openblocks_donation_station");
	}

	@LuaFunction(mainThread = true)
	public final MethodResult getItemAuthor() {
		return DonationInfo.of(blockEntity.getInventory().getStackInSlot(0))
				.map(info -> MethodResult.of(info.modName(), authors(info.authors()), info.url()))
				.orElse(MethodResult.of());
	}

	private static List<String> authors(String authors) {
		return Arrays.stream(authors.split(",")).map(String::strip).filter(author -> !author.isEmpty()).toList();
	}
}
