package openblocks.client.gui;

import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import openblocks.OpenBlocks;
import openblocks.block.entity.PaintMixerBlockEntity;
import openblocks.menu.PaintMixerMenu;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code GuiPaintMixer}: color picker (hue across, brightness down), saturation bar, hex field and dye palette,
 * plus the mix button, progress arrow and the four ink levels.
 */
public class PaintMixerScreen extends AbstractContainerScreen<PaintMixerMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/paint_mixer.png");
	private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

	private static final int[] INK_COLORS = { 0xFF4B9FC1, 0xFFDB7AD5, 0xFFE7E72A, 0xFF000000 };

	private static final int PICKER_X = 8;
	private static final int PICKER_Y = 18;
	private static final int PICKER_WIDTH = 100;
	private static final int PICKER_HEIGHT = 50;
	private static final int TONE_Y = PICKER_Y + PICKER_HEIGHT + 3;
	private static final int TONE_HEIGHT = 6;
	private static final int PREVIEW_Y = TONE_Y + TONE_HEIGHT + 3;
	private static final int PREVIEW_WIDTH = 20;
	private static final int FIELD_HEIGHT = 12;
	private static final int PALETTE_Y = PREVIEW_Y + FIELD_HEIGHT + 3;
	private static final int PALETTE_SIZE = 9;
	private static final int ARROW_X = 125;
	private static final int ARROW_Y = 43;

	private EditBox hexField;

	// picker position: x is the hue, y the darkness; tone is the inverse saturation, like legacy
	private int pointX;
	private int pointY;
	private int tone;
	private int selectedColor;
	private int shownServerColor = -1;
	private boolean draggingPicker;
	private boolean draggingTone;

	public PaintMixerScreen(PaintMixerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 202;
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		hexField = addRenderableWidget(new EditBox(font, leftPos + PICKER_X + PREVIEW_WIDTH + 4, topPos + PREVIEW_Y, 50, FIELD_HEIGHT, Component.empty()));
		hexField.setMaxLength(6);
		hexField.setFilter(text -> text.chars().allMatch(c -> Character.digit(c, 16) >= 0));
		hexField.setResponder(this::onHexChanged);

		addRenderableWidget(Button.builder(Component.translatable("openblocks.gui.paintmixer.mix"), b -> mix())
				.bounds(leftPos + 125, topPos + 57, 34, 14)
				.build());

		shownServerColor = -1;
		syncFromServer();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		syncFromServer();
	}

	/**
	 * Takes over the mixer's color when it changes, e.g. when a can is put in, like legacy.
	 */
	private void syncFromServer() {
		final int serverColor = menu.getColor();
		if (serverColor == shownServerColor) return;
		shownServerColor = serverColor;
		setColor(serverColor, true);
	}

	private void setColor(int color, boolean updateHex) {
		selectedColor = color & PaintColors.WHITE;
		final float[] hsb = Color.RGBtoHSB((selectedColor >> 16) & 0xFF, (selectedColor >> 8) & 0xFF, selectedColor & 0xFF, null);
		pointX = (int)(hsb[0] * PICKER_WIDTH);
		pointY = (int)((1f - hsb[2]) * PICKER_HEIGHT);
		tone = 255 - (int)(hsb[1] * 255);
		if (updateHex) setHex(selectedColor);
	}

	private void setHex(int color) {
		hexField.setResponder(text -> {});
		hexField.setValue(String.format(Locale.ROOT, "%06X", color));
		hexField.setResponder(this::onHexChanged);
	}

	private void onHexChanged(String text) {
		if (text.length() == 6) setColor(Integer.parseInt(text, 16), false);
	}

	private int pickerColor() {
		return colorAt(pointX, pointY, tone);
	}

	private static int colorAt(int x, int y, int tone) {
		return Color.HSBtoRGB(x / (float)PICKER_WIDTH, 1f - tone / 255f, 1f - y / (float)PICKER_HEIGHT) & PaintColors.WHITE;
	}

	private void mix() {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selectedColor);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		final DyeColor dye = paletteAt(mouseX, mouseY);
		if (dye != null) graphics.renderTooltip(font, Component.translatable("color.minecraft." + dye.getSerializedName()), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, 256);

		final int x = leftPos + PICKER_X;
		final int y = topPos + PICKER_Y;
		for (int column = 0; column < PICKER_WIDTH; column++)
			graphics.fillGradient(x + column, y, x + column + 1, y + PICKER_HEIGHT, 0xFF000000 | colorAt(column, 0, tone), 0xFF000000);
		graphics.fill(x + pointX - 1, y + pointY - 1, x + pointX + 1, y + pointY + 1, 0xCCCC0000);

		final int toneY = topPos + TONE_Y;
		for (int column = 0; column < PICKER_WIDTH; column++)
			graphics.fill(x + column, toneY, x + column + 1, toneY + TONE_HEIGHT, 0xFF000000 | colorAt(pointX, pointY, column * 255 / PICKER_WIDTH));
		final int toneMarker = x + tone * PICKER_WIDTH / 255;
		graphics.fill(toneMarker - 1, toneY - 1, toneMarker + 1, toneY + TONE_HEIGHT + 1, 0xFFFFFFFF);

		graphics.fill(x, topPos + PREVIEW_Y, x + PREVIEW_WIDTH, topPos + PREVIEW_Y + FIELD_HEIGHT, 0xFF000000 | selectedColor);

		for (DyeColor dye : DyeColor.values()) {
			final int px = x + (dye.getId() % 8) * PALETTE_SIZE;
			final int py = topPos + PALETTE_Y + (dye.getId() / 8) * PALETTE_SIZE;
			graphics.fill(px, py, px + PALETTE_SIZE - 1, py + PALETTE_SIZE - 1, 0xFF000000 | PaintColors.rgb(dye));
		}

		for (int ink = 0; ink < 4; ink++) {
			final int slotX = leftPos + PaintMixerMenu.DYES_X + (ink % 2) * 20 - 2;
			final int slotY = topPos + PaintMixerMenu.DYES_Y + (ink / 2) * 20 - 2;
			graphics.fill(slotX, slotY, slotX + 20, slotY + 20, INK_COLORS[ink]);
			graphics.fill(slotX + 2, slotY + 2, slotX + 18, slotY + 18, 0x40000000);

			// level bars outside the slots, like legacy
			final int barX = ink % 2 == 0? slotX - 3 : slotX + 21;
			final int level = Math.round(14 * Mth.clamp(menu.getInk(ink) / PaintMixerBlockEntity.MAX_INK, 0f, 1f));
			graphics.fill(barX, slotY + 6 - 3, barX + 2, slotY + 20 - 3, 0xFF888888);
			graphics.fill(barX, slotY + 20 - 3 - level, barX + 2, slotY + 20 - 3, INK_COLORS[ink]);
		}

		final int progress = menu.getProgress();
		if (progress > 0) {
			final int width = Mth.ceil(24f * progress / PaintMixerBlockEntity.PROGRESS_TICKS);
			graphics.blitSprite(ARROW, 24, 16, 0, 0, leftPos + ARROW_X, topPos + ARROW_Y, width, 16);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
	}

	private DyeColor paletteAt(double mouseX, double mouseY) {
		final int column = Mth.floor((mouseX - leftPos - PICKER_X) / PALETTE_SIZE);
		final int row = Mth.floor((mouseY - topPos - PALETTE_Y) / PALETTE_SIZE);
		if (column < 0 || column >= 8 || row < 0 || row >= 2) return null;
		return DyeColor.byId(row * 8 + column);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			if (isHovering(PICKER_X, PICKER_Y, PICKER_WIDTH, PICKER_HEIGHT, mouseX, mouseY)) {
				draggingPicker = true;
				pickAt(mouseX, mouseY);
				return true;
			}
			if (isHovering(PICKER_X, TONE_Y, PICKER_WIDTH, TONE_HEIGHT, mouseX, mouseY)) {
				draggingTone = true;
				toneAt(mouseX);
				return true;
			}
			final DyeColor dye = paletteAt(mouseX, mouseY);
			if (dye != null) {
				setColor(PaintColors.rgb(dye), true);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingPicker) {
			pickAt(mouseX, mouseY);
			return true;
		}
		if (draggingTone) {
			toneAt(mouseX);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingPicker = false;
		draggingTone = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void pickAt(double mouseX, double mouseY) {
		pointX = Mth.clamp((int)(mouseX - leftPos - PICKER_X), 0, PICKER_WIDTH - 1);
		pointY = Mth.clamp((int)(mouseY - topPos - PICKER_Y), 0, PICKER_HEIGHT);
		selectedColor = pickerColor();
		setHex(selectedColor);
	}

	private void toneAt(double mouseX) {
		tone = Mth.clamp((int)((mouseX - leftPos - PICKER_X) * 255 / PICKER_WIDTH), 0, 255);
		selectedColor = pickerColor();
		setHex(selectedColor);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// typing into the hex field must not close the screen with the inventory key
		if (hexField.isFocused() && keyCode != 256) return hexField.keyPressed(keyCode, scanCode, modifiers) || true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
