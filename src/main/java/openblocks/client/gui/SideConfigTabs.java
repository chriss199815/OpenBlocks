package openblocks.client.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Folding tabs on the right edge of a screen, each holding a side selector; replaces OpenModsLib {@code GuiComponentTab(Wrapper)}.
 * At most one tab is open. Coordinates passed in are absolute screen coordinates.
 */
public class SideConfigTabs {

	private static final int FOLDED_SIZE = 24;
	private static final int EXPANDED_WIDTH = 100;
	private static final int EXPANDED_HEIGHT = 100;
	// tabs overlap the screen border, which has no edge on the tab side
	private static final int OVERLAP = 5;
	private static final int TOP_OFFSET = 4;

	private final List<Tab> tabs = new ArrayList<>();

	@Nullable
	private Tab activeTab;

	private int right;
	private int top;

	public static final class Tab {
		private final int color;
		private final ItemStack icon;
		private final Component label;
		private final SideSelectorWidget selector;

		private float width = FOLDED_SIZE;
		private float height = FOLDED_SIZE;
		private int x;
		private int y;

		/**
		 * @param color RGB, like the legacy {@code StandardPalette}
		 * @param selector positioned relative to the tab's top left corner
		 */
		public Tab(int color, ItemStack icon, Component label, SideSelectorWidget selector) {
			this.color = color;
			this.icon = icon;
			this.label = label;
			this.selector = selector;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + Math.round(width) && mouseY >= y && mouseY < y + Math.round(height);
		}

		private boolean isHeader(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + FOLDED_SIZE && mouseY >= y && mouseY < y + FOLDED_SIZE;
		}
	}

	public void add(Tab tab) {
		tabs.add(tab);
	}

	/**
	 * Must be called when the screen is (re)initialized, before rendering.
	 */
	public void setPosition(int screenRight, int screenTop) {
		this.right = screenRight;
		this.top = screenTop;
		layout();
	}

	private void layout() {
		int y = top + TOP_OFFSET;
		for (Tab tab : tabs) {
			tab.x = right - OVERLAP;
			tab.y = y;
			y += Math.round(tab.height) - 1;
		}
	}

	private boolean isOpen(Tab tab) {
		return tab == activeTab && Math.round(tab.width) == EXPANDED_WIDTH && Math.round(tab.height) == EXPANDED_HEIGHT;
	}

	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		for (Tab tab : tabs) {
			final int targetWidth = tab == activeTab? EXPANDED_WIDTH : FOLDED_SIZE;
			final int targetHeight = tab == activeTab? EXPANDED_HEIGHT : FOLDED_SIZE;
			tab.width += (targetWidth - tab.width) / 4;
			tab.height += (targetHeight - tab.height) / 4;
			if (Math.abs(targetWidth - tab.width) < 0.5f) tab.width = targetWidth;
			if (Math.abs(targetHeight - tab.height) < 0.5f) tab.height = targetHeight;
		}
		layout();

		for (Tab tab : tabs) {
			drawBox(graphics, tab.x, tab.y, Math.round(tab.width), Math.round(tab.height), tab.color);
			graphics.renderItem(tab.icon, tab.x + 3, tab.y + 3);
			if (isOpen(tab)) {
				graphics.drawString(font, tab.label, tab.x + 24, tab.y + 8, 0x404040, false);
				tab.selector.render(graphics, tab.x, tab.y, mouseX, mouseY);
			}
		}
	}

	/**
	 * Vanilla-style panel in the tab color, open on the left.
	 */
	private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		final int base = 0xFF000000 | color;
		final int light = 0xFF000000 | scale(color, 1.25f);
		final int dark = 0xFF000000 | scale(color, 0.6f);
		graphics.fill(x, y, x + width - 1, y + 1, 0xFF000000);
		graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFF000000);
		graphics.fill(x, y + height - 1, x + width - 1, y + height, 0xFF000000);
		graphics.fill(x, y + 1, x + width - 1, y + height - 1, base);
		graphics.fill(x, y + 1, x + width - 2, y + 3, light);
		graphics.fill(x + width - 3, y + 3, x + width - 1, y + height - 1, dark);
		graphics.fill(x, y + height - 3, x + width - 3, y + height - 1, dark);
	}

	private static int scale(int color, float factor) {
		final int r = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
		final int g = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
		final int b = Math.min(255, Math.round((color & 0xFF) * factor));
		return (r << 16) | (g << 8) | b;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return tabs.stream().anyMatch(tab -> tab.contains(mouseX, mouseY));
	}

	/**
	 * Areas covered by the tabs, e.g. so JEI doesn't draw its item list over them.
	 */
	public List<Rect2i> getAreas() {
		return tabs.stream().map(tab -> new Rect2i(tab.x, tab.y, Math.round(tab.width), Math.round(tab.height))).toList();
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (Tab tab : tabs) {
			if (!tab.contains(mouseX, mouseY)) continue;
			if (tab.isHeader(mouseX, mouseY)) activeTab = tab == activeTab? null : tab;
			else if (isOpen(tab)) tab.selector.mouseClicked(mouseX, mouseY, button);
			// clicks on a tab never reach the screen, which would drop the carried item
			return true;
		}
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return activeTab != null && activeTab.selector.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return activeTab != null && activeTab.selector.mouseReleased(mouseX, mouseY, button);
	}
}
