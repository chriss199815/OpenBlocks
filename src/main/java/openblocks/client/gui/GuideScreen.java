package openblocks.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import openblocks.block.entity.GuideBlockEntity;
import openblocks.common.GuideOrientation;
import openblocks.common.GuideSettings;
import openblocks.menu.GuideMenu;
import openblocks.util.PaintColors;

/**
 * Settings of a building guide (new, legacy only had the touch buttons): shape, size towards each side (labelled with
 * the world direction it points to, shift for steps of 10), rotation, marker color and redstone mode.
 */
public class GuideScreen extends AbstractContainerScreen<GuideMenu> {

	private static final int WIDTH = 240;
	private static final int HEIGHT = 172;

	private static final int SHAPE_Y = 22;
	private static final int SIZE_Y = 46;
	private static final int SIZE_ROW_HEIGHT = 20;
	private static final int BUTTON_SIZE = 16;
	// x of the label, minus button, value center and plus button of the negative and positive half
	private static final int[][] SIZE_COLUMNS = { { 8, 56, 84, 96 }, { 128, 176, 204, 216 } };
	// local axes of the rows, negative half first
	private static final Direction[][] SIZE_ROWS = {
			{ Direction.WEST, Direction.EAST }, { Direction.DOWN, Direction.UP }, { Direction.NORTH, Direction.SOUTH } };

	private static final int COLOR_Y = 112;
	private static final int COLOR_SIZE = 12;
	private static final int COLOR_STEP = 14;
	private static final int CONTROLS_Y = 132;
	private static final int COUNT_Y = 156;

	private Button redstoneButton;

	public GuideScreen(GuideMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
	}

	private GuideBlockEntity guide() {
		return menu.getGuide();
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(button("◀", 40, SHAPE_Y - 4, 20, GuideMenu.BUTTON_PREVIOUS_SHAPE));
		addRenderableWidget(button("▶", WIDTH - 60, SHAPE_Y - 4, 20, GuideMenu.BUTTON_NEXT_SHAPE));

		final Tooltip stepTooltip = Tooltip.create(Component.translatable("openblocks.gui.guide.size_step"));
		for (int row = 0; row < SIZE_ROWS.length; row++)
			for (int half = 0; half < 2; half++) {
				final Direction halfAxis = SIZE_ROWS[row][half];
				final int[] columns = SIZE_COLUMNS[half];
				final int y = SIZE_Y + row * SIZE_ROW_HEIGHT;
				final Button minus = Button.builder(Component.literal("-"), b -> clickSize(halfAxis, false))
						.bounds(leftPos + columns[1], topPos + y, BUTTON_SIZE, BUTTON_SIZE).tooltip(stepTooltip).build();
				final Button plus = Button.builder(Component.literal("+"), b -> clickSize(halfAxis, true))
						.bounds(leftPos + columns[3], topPos + y, BUTTON_SIZE, BUTTON_SIZE).tooltip(stepTooltip).build();
				addRenderableWidget(minus);
				addRenderableWidget(plus);
			}

		addRenderableWidget(button("↺", 8, CONTROLS_Y, 20, GuideMenu.BUTTON_ROTATE_CCW))
				.setTooltip(Tooltip.create(Component.translatable("openblocks.gui.guide.rotate_ccw")));
		addRenderableWidget(button("↻", 30, CONTROLS_Y, 20, GuideMenu.BUTTON_ROTATE_CW))
				.setTooltip(Tooltip.create(Component.translatable("openblocks.gui.guide.rotate_cw")));
		redstoneButton = addRenderableWidget(Button.builder(redstoneLabel(), b -> click(GuideMenu.BUTTON_REDSTONE_MODE))
				.bounds(leftPos + 56, topPos + CONTROLS_Y, WIDTH - 64, 20).build());
	}

	private Button button(String label, int x, int y, int width, int id) {
		return Button.builder(Component.literal(label), b -> click(id)).bounds(leftPos + x, topPos + y, width, 20).build();
	}

	private void click(int id) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	private void clickSize(Direction halfAxis, boolean increase) {
		final int step = (Screen.hasShiftDown()? 2 : 0) + (increase? 1 : 0);
		click(GuideMenu.sizeButton(halfAxis, step));
	}

	private Component redstoneLabel() {
		return Component.translatable("openblocks.gui.guide.redstone", guide().getSettings().redstoneMode().displayName());
	}

	@Override
	public void containerTick() {
		super.containerTick();
		redstoneButton.setMessage(redstoneLabel());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	// vanilla-style panel, there is no texture
	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		final int x0 = leftPos;
		final int y0 = topPos;
		final int x1 = leftPos + imageWidth;
		final int y1 = topPos + imageHeight;
		graphics.fill(x0, y0, x1, y1, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFFFFFFF);
		graphics.fill(x0 + 2, y0 + 2, x1 - 1, y1 - 1, 0xFF555555);
		graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0xFFC6C6C6);

		final int selected = guide().getSettings().color();
		for (DyeColor dye : DyeColor.values()) {
			final int x = x0 + colorX(dye);
			final int y = y0 + COLOR_Y;
			final int rgb = PaintColors.rgb(dye);
			graphics.fill(x - 1, y - 1, x + COLOR_SIZE + 1, y + COLOR_SIZE + 1, rgb == selected? 0xFFFFFFFF : 0xFF373737);
			graphics.fill(x, y, x + COLOR_SIZE, y + COLOR_SIZE, 0xFF000000 | rgb);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		final GuideBlockEntity guide = guide();
		final GuideSettings settings = guide.getSettings();
		final GuideOrientation orientation = guide.getOrientation();

		graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
		drawCentered(graphics, settings.shape().displayName(), imageWidth / 2, SHAPE_Y + 2);

		for (int row = 0; row < SIZE_ROWS.length; row++)
			for (int half = 0; half < 2; half++) {
				final Direction halfAxis = SIZE_ROWS[row][half];
				final int[] columns = SIZE_COLUMNS[half];
				final int y = SIZE_Y + row * SIZE_ROW_HEIGHT + 4;
				final Component direction = Component.translatable("openblocks.gui.guide.direction." + orientation.toWorld(halfAxis).getSerializedName());
				graphics.drawString(font, direction, columns[0], y, 0x404040, false);
				drawCentered(graphics, Component.literal(Integer.toString(settings.size(halfAxis))), columns[2], y);
			}

		graphics.drawString(font, Component.translatable("openblocks.misc.total_blocks", guide.getShape().size()), 8, COUNT_Y, 0x404040, false);
	}

	private void drawCentered(GuiGraphics graphics, Component text, int x, int y) {
		graphics.drawString(font, text, x - font.width(text) / 2, y, 0x404040, false);
	}

	private static int colorX(DyeColor dye) {
		return 8 + dye.getId() * COLOR_STEP;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			final double y = mouseY - topPos - COLOR_Y;
			if (y >= 0 && y < COLOR_SIZE)
				for (DyeColor dye : DyeColor.values()) {
					final double x = mouseX - leftPos - colorX(dye);
					if (x >= 0 && x < COLOR_SIZE) {
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
						click(GuideMenu.BUTTON_COLOR + dye.getId());
						return true;
					}
				}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
