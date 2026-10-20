package openblocks.integration.patchouli;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import openblocks.integration.jei.JeiBookmarks;
import vazkii.patchouli.api.IComponentRenderContext;

/**
 * Small ribbon on manual pages that toggles a JEI bookmark; hidden without JEI.
 */
class BookmarkButton {

	private static final int WIDTH = 7;
	private static final int HEIGHT = 10;

	private static final int COLOR_ON = 0xFFC0392B;
	private static final int COLOR_ON_HOVER = 0xFFE74C3C;
	private static final int COLOR_OFF = 0xFF9E9E9E;
	private static final int COLOR_OFF_HOVER = 0xFF6E6E6E;

	private boolean hovered;

	void render(GuiGraphics graphics, IComponentRenderContext context, int x, int y, int mouseX, int mouseY, BooleanSupplier isBookmarked) {
		hovered = JeiBookmarks.isAvailable() && context.isAreaHovered(mouseX, mouseY, x, y, WIDTH, HEIGHT);
		if (!JeiBookmarks.isAvailable()) return;

		final boolean bookmarked = isBookmarked.getAsBoolean();
		final int color = bookmarked? (hovered? COLOR_ON_HOVER : COLOR_ON) : (hovered? COLOR_OFF_HOVER : COLOR_OFF);
		// ribbon with a notch at the bottom
		graphics.fill(x, y, x + WIDTH, y + HEIGHT - 3, color);
		for (int row = 0; row < 3; row++) {
			final int y0 = y + HEIGHT - 3 + row;
			graphics.fill(x, y0, x + 3 - row, y0 + 1, color);
			graphics.fill(x + WIDTH - 3 + row, y0, x + WIDTH, y0 + 1, color);
		}

		if (hovered) context.setHoverTooltipComponents(List.of(Component.translatable(bookmarked? "openblocks.gui.jei_bookmark.remove" : "openblocks.gui.jei_bookmark.add")));
	}

	boolean click(Runnable toggle) {
		if (!hovered) return false;
		toggle.run();
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		return true;
	}
}
