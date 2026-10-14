package openblocks.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Port of legacy {@code GuiBlockPlacer} and {@code GuiSprinkler}: a 3x3 grid, drawn with the vanilla dispenser background, which has the same layout.
 */
public class DispenserLayoutScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

	private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

	public DispenserLayoutScreen(M menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
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
}
