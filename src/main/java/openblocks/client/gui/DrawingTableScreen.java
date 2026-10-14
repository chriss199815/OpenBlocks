package openblocks.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import openblocks.OpenBlocks;
import openblocks.item.StencilItem;
import openblocks.menu.DrawingTableMenu;

/**
 * Port of legacy {@code GuiDrawingTable}: unprepared stencils go in on the left, the pattern is picked with the buttons next to the output.
 * The legacy up/down arrows became left/right buttons, and the selected pattern is named below the output.
 */
public class DrawingTableScreen extends AbstractContainerScreen<DrawingTableMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/drawing_table.png");

	private static final int BUTTON_WIDTH = 18;
	private static final int BUTTON_Y = DrawingTableMenu.SLOT_Y - 2;
	private static final int PATTERN_Y = DrawingTableMenu.SLOT_Y + 22;

	public DrawingTableScreen(DrawingTableMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> clickButton(DrawingTableMenu.BUTTON_PREVIOUS))
				.bounds(leftPos + DrawingTableMenu.OUTPUT_X - 4 - BUTTON_WIDTH, topPos + BUTTON_Y, BUTTON_WIDTH, 20)
				.build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> clickButton(DrawingTableMenu.BUTTON_NEXT))
				.bounds(leftPos + DrawingTableMenu.OUTPUT_X + 19, topPos + BUTTON_Y, BUTTON_WIDTH, 20)
				.build());
	}

	private void clickButton(int id) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
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
		final Component pattern = StencilItem.patternName(menu.getPattern());
		graphics.drawString(font, pattern, DrawingTableMenu.OUTPUT_X + 8 - font.width(pattern) / 2, PATTERN_Y, 0x404040, false);
	}
}
