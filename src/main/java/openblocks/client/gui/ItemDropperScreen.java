package openblocks.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import openblocks.OpenBlocks;
import openblocks.block.entity.ItemDropperBlockEntity;
import openblocks.menu.ItemDropperMenu;

/**
 * Port of legacy {@code GuiItemDropper}: inventory grid, speed slider and "scale by redstone" checkbox.
 * Unlike legacy (300 pixels wide), it has the vanilla dispenser size, with the grid moved to the left.
 */
public class ItemDropperScreen extends AbstractContainerScreen<ItemDropperMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/item_dropper.png");
	private static final ResourceLocation CHECKBOX = ResourceLocation.withDefaultNamespace("widget/checkbox");
	private static final ResourceLocation CHECKBOX_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
	private static final ResourceLocation CHECKBOX_SELECTED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
	private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");

	private static final int CONTROLS_X = 70;
	private static final int CONTROLS_WIDTH = 98;
	private static final int SLIDER_Y = 17;
	private static final int CHECKBOX_Y = 44;
	private static final int CHECKBOX_SIZE = 17;
	private static final int LABEL_X = CONTROLS_X + CHECKBOX_SIZE + 4;

	private SpeedSlider slider;

	public ItemDropperScreen(ItemDropperMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		slider = addRenderableWidget(new SpeedSlider(leftPos + CONTROLS_X, topPos + SLIDER_Y));
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		slider.sync();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		final boolean hovered = isOverCheckbox(mouseX, mouseY);
		final ResourceLocation sprite = menu.useRedstoneStrength()? (hovered? CHECKBOX_SELECTED_HIGHLIGHTED : CHECKBOX_SELECTED) : (hovered? CHECKBOX_HIGHLIGHTED : CHECKBOX);
		graphics.blitSprite(sprite, leftPos + CONTROLS_X, topPos + CHECKBOX_Y, CHECKBOX_SIZE, CHECKBOX_SIZE);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		int y = CHECKBOX_Y;
		for (FormattedCharSequence line : font.split(Component.translatable("openblocks.misc.dropper.use_redstone"), CONTROLS_X + CONTROLS_WIDTH - LABEL_X)) {
			graphics.drawString(font, line, LABEL_X, y, 0x404040, false);
			y += font.lineHeight;
		}
	}

	private boolean isOverCheckbox(double mouseX, double mouseY) {
		// the label is part of the checkbox, like vanilla
		return isHovering(CONTROLS_X, CHECKBOX_Y, CONTROLS_WIDTH, CHECKBOX_SIZE + font.lineHeight, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isOverCheckbox(mouseX, mouseY)) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ItemDropperMenu.toggleRedstoneButtonId());
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private class SpeedSlider extends AbstractSliderButton {

		private int speed = -1;
		private boolean dragging;

		private SpeedSlider(int x, int y) {
			super(x, y, CONTROLS_WIDTH, 20, Component.empty(), 0);
			sync();
		}

		/**
		 * Takes over the server's value, unless the player is moving the slider.
		 */
		private void sync() {
			if (dragging || menu.getMaxSpeed() <= 0) return;
			if (menu.getSpeed() == speed) return;
			speed = menu.getSpeed();
			value = (double)speed / menu.getMaxSpeed();
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("openblocks.misc.dropper.speed").append(String.format(": %.1f", Math.max(speed, 0) * ItemDropperBlockEntity.SPEED_STEP)));
		}

		@Override
		protected void applyValue() {
			final int newSpeed = (int)Math.round(value * menu.getMaxSpeed());
			if (newSpeed == speed) return;
			speed = newSpeed;
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ItemDropperMenu.speedButtonId(speed));
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			dragging = true;
			super.onClick(mouseX, mouseY);
		}

		@Override
		public void onRelease(double mouseX, double mouseY) {
			dragging = false;
			super.onRelease(mouseX, mouseY);
		}
	}
}
