package openblocks.common;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code DonationUrlManager} and the mod lookup of {@code TileEntityDonationStation}: which mod added an item, and where to support its authors.
 * Mods can still send a donation link with the IMC message {@code donateUrl}; otherwise the mod's {@code displayURL} is used.
 * The legacy built-in list of 1.12 mods is gone.
 */
public record DonationInfo(String modName, String authors, @Nullable String url, boolean isDonationUrl) {

	public static final String IMC_DONATE_URL = "donateUrl";

	private static final Map<String, String> DONATION_URLS = new ConcurrentHashMap<>();

	public static void onInterModProcess(InterModProcessEvent event) {
		InterModComms.getMessages(OpenBlocks.MODID, IMC_DONATE_URL::equals).forEach(message -> {
			if (message.messageSupplier().get() instanceof String url) DONATION_URLS.put(message.senderModId(), url);
			else OpenBlocks.LOG.warn("Ignoring {} message from {}: expected a string", IMC_DONATE_URL, message.senderModId());
		});
	}

	public static Optional<DonationInfo> of(ItemStack stack) {
		if (stack.isEmpty()) return Optional.empty();
		final String modId = stack.getItem().getCreatorModId(stack);
		if (modId == null) return Optional.empty();
		return ModList.get().getModContainerById(modId).map(container -> of(container.getModInfo()));
	}

	private static DonationInfo of(IModInfo mod) {
		final String authors = mod.getConfig().getConfigElement("authors").map(String::valueOf).orElse("");
		final String donationUrl = DONATION_URLS.get(mod.getModId());
		if (donationUrl != null) return new DonationInfo(mod.getDisplayName(), authors, donationUrl, true);
		return new DonationInfo(mod.getDisplayName(), authors, mod.getModURL().map(URL::toString).orElse(null), false);
	}
}
