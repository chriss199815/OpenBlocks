package openblocks.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code LoreFlimFlam.DisplayHandler}: shows the silly lore of the lore flim flam (client config
 * {@code tomfoolery.sillyLoreDisplay}), unless the item has vanilla lore.
 */
public class SillyLoreTooltip {

	public static void onItemTooltip(ItemTooltipEvent event) {
		final int display = OBClientConfig.TOMFOOLERY.sillyLoreDisplay.get();
		if (display <= 0) return;

		final List<String> lore = event.getItemStack().get(OBComponents.SILLY_LORE);
		if (lore == null || event.getItemStack().has(DataComponents.LORE)) return;

		if (display > 1 || Screen.hasAltDown()) {
			for (String line : lore)
				event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
		} else {
			event.getToolTip().add(Component.translatable("openblocks.misc.hidden_lore"));
		}
	}
}
