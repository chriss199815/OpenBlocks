package openblocks.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import openblocks.OpenBlocks;
import openblocks.menu.BigButtonMenu;

/**
 * Port of legacy {@code GuiBigButton}: the 8 slots and the total press time below them.
 */
public class BigButtonScreen extends AbstractContainerScreen<BigButtonMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/big_button.png");

	public BigButtonScreen(BigButtonMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 182;
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		final int ticks = menu.getTickTime();
		final Component total = Component.translatable("openblocks.misc.total_ticks", ticks, String.format("%.2f", ticks / 20.0));
		graphics.drawString(font, total, (imageWidth - font.width(total)) / 2, 72, 0x404040, false);
	}
}
