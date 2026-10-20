package openblocks.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import openblocks.OBConfig;
import openblocks.integration.patchouli.OBPatchouli;
import openblocks.registry.OBAttachments;
import openblocks.registry.OBItems;

/**
 * Port of the info book part of legacy {@code EntityEventHandler}: every player gets the manual once, on first login.
 */
public class InfoBookHandler {

	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		final Player player = event.getEntity();
		if (!OBConfig.ADDITIONAL.infoBook.getAsBoolean() || !OBPatchouli.isLoaded() || player.getData(OBAttachments.INFO_BOOK_GIVEN)) return;

		final ItemStack book = new ItemStack(OBItems.INFO_BOOK.get());
		if (!player.getInventory().add(book)) player.drop(book, false);
		player.setData(OBAttachments.INFO_BOOK_GIVEN, true);
	}
}
