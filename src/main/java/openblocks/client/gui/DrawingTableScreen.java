package openblocks.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import openblocks.OpenBlocks;
import openblocks.block.entity.DrawingTableBlockEntity;
import openblocks.item.StencilItem;
import openblocks.menu.DrawingTableMenu;
import openblocks.network.DrawingTablePrintPayload;
import org.lwjgl.glfw.GLFW;

/**
 * Port of legacy {@code GuiDrawingTable}: unprepared stencils go in on the left, the pattern or character is picked with
 * the buttons next to the output. The legacy up/down arrows became left/right buttons, and the selected pattern is
 * named below the output. In glyph mode, a text field and print button replace the pattern name.
 */
public class DrawingTableScreen extends AbstractContainerScreen<DrawingTableMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/drawing_table.png");

	private static final int BUTTON_WIDTH = 18;
	private static final int BUTTON_Y = DrawingTableMenu.SLOT_Y - 2;
	private static final int PATTERN_Y = DrawingTableMenu.SLOT_Y + 22;
	private static final int MODE_Y = 16;
	private static final int MODE_WIDTH = 52;
	private static final int PRINT_Y = DrawingTableMenu.SLOT_Y + 20;
	private static final int PRINT_WIDTH = 40;

	private Button modeButton;
	private EditBox text;
	private Button printButton;

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
		modeButton = addRenderableWidget(Button.builder(modeName(), b -> clickButton(DrawingTableMenu.BUTTON_MODE))
				.bounds(leftPos + 8, topPos + MODE_Y, MODE_WIDTH, 14)
				.build());

		final int textWidth = imageWidth - 16 - PRINT_WIDTH - 4;
		text = addRenderableWidget(new EditBox(font, leftPos + 8, topPos + PRINT_Y, textWidth, 14, Component.translatable("openblocks.gui.drawingtable.glyphs")));
		text.setMaxLength(DrawingTableBlockEntity.MAX_PRINT_SIZE);
		printButton = addRenderableWidget(Button.builder(Component.translatable("openblocks.gui.drawingtable.print"), b -> print())
				.bounds(leftPos + imageWidth - 8 - PRINT_WIDTH, topPos + PRINT_Y, PRINT_WIDTH, 14)
				.build());
		updateWidgets();
	}

	private Component modeName() {
		return Component.translatable(menu.getMode().translationKey);
	}

	private void clickButton(int id) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	/**
	 * Like legacy, what could not be printed (no glyph for it or not enough input) stays in the text field.
	 */
	private void print() {
		final String value = text.getValue();
		final int printed = DrawingTableBlockEntity.printableCount(value, menu.getInputCount());
		if (printed == 0) return;
		PacketDistributor.sendToServer(new DrawingTablePrintPayload(value));
		text.setValue(value.substring(printed));
	}

	private void updateWidgets() {
		final boolean glyphs = menu.getMode() == DrawingTableBlockEntity.Mode.GLYPHS;
		modeButton.setMessage(modeName());
		text.visible = glyphs;
		printButton.visible = glyphs;
		printButton.active = DrawingTableBlockEntity.printableCount(text.getValue(), menu.getInputCount()) > 0;
		if (!glyphs) text.setFocused(false);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		updateWidgets();
	}

	// the text field gets typed keys before they close the screen or move items
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers);
		if (text.isVisible() && text.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				print();
				return true;
			}
			return text.keyPressed(keyCode, scanCode, modifiers) || text.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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
		if (menu.getMode() != DrawingTableBlockEntity.Mode.STENCILS) return;
		final Component pattern = StencilItem.patternName(menu.getPattern());
		graphics.drawString(font, pattern, DrawingTableMenu.OUTPUT_X + 8 - font.width(pattern) / 2, PATTERN_Y, 0x404040, false);
	}
}
