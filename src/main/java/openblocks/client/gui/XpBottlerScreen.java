package openblocks.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import openblocks.OpenBlocks;
import openblocks.block.entity.XpBottlerBlockEntity;
import openblocks.common.AutoSlotConfig;
import openblocks.menu.XpBottlerMenu;

/**
 * Port of legacy {@code GuiXPBottler}: progress, XP tank and one tab each for the glass bottle, XP bottle and XP fluid sides.
 */
public class XpBottlerScreen extends AbstractContainerScreen<XpBottlerMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/xp_bottler.png");
	private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final ResourceLocation PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

	private static final int ARROW_X = 75;
	private static final int ARROW_Y = 30;
	private static final int ARROW_WIDTH = 24;
	private static final int ARROW_HEIGHT = 16;

	private static final int INPUT_TAB_COLOR = 0x8784c8;
	private static final int OUTPUT_TAB_COLOR = 0x84c7c8;
	private static final int XP_TAB_COLOR = 0x84c892;

	private final TankWidget tank = new TankWidget(140, 18, 17, 37);
	private final SideConfigTabs tabs = new SideConfigTabs();

	public XpBottlerScreen(XpBottlerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 151;
		this.inventoryLabelY = imageHeight - 94;

		tabs.add(slotTab(XpBottlerBlockEntity.AutoSlot.INPUT, INPUT_TAB_COLOR, new ItemStack(Items.GLASS_BOTTLE), "openblocks.gui.autoextract"));
		tabs.add(slotTab(XpBottlerBlockEntity.AutoSlot.XP, XP_TAB_COLOR, new ItemStack(Items.BUCKET), "openblocks.gui.autodrink"));
		tabs.add(slotTab(XpBottlerBlockEntity.AutoSlot.OUTPUT, OUTPUT_TAB_COLOR, new ItemStack(Items.EXPERIENCE_BOTTLE), "openblocks.gui.autoeject"));
	}

	private SideConfigTabs.Tab slotTab(XpBottlerBlockEntity.AutoSlot slot, int color, ItemStack icon, String label) {
		final XpBottlerBlockEntity bottler = menu.getBottler();
		final AutoSlotConfig<XpBottlerBlockEntity.AutoSlot> config = bottler.getConfig();
		final SideSelectorWidget selector = new SideSelectorWidget(15, 22, 40, bottler, () -> config.getSides(slot), true,
				side -> clickButton(config.toggleSideButtonId(slot, side)));
		return new SideConfigTabs.Tab(color, icon, Component.translatable(label), selector)
				.withCheckbox(() -> config.isAuto(slot), () -> clickButton(config.toggleAutoButtonId(slot)));
	}

	private void clickButton(int id) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		tabs.setPosition(leftPos + imageWidth, topPos);
	}

	public List<Rect2i> getExtraAreas() {
		return tabs.getAreas();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		if (tank.isMouseOver(leftPos, topPos, mouseX, mouseY))
			tank.renderTooltip(graphics, font, mouseX, mouseY, menu.getBottler().getFluid(), XpBottlerBlockEntity.getTankCapacity());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		// empty arrow from the vanilla furnace background, filled one from its sprite
		graphics.blit(FURNACE, leftPos + ARROW_X, topPos + ARROW_Y, 79, 34, ARROW_WIDTH, ARROW_HEIGHT);
		final int progress = menu.getProgress() * ARROW_WIDTH / XpBottlerBlockEntity.PROGRESS_TICKS;
		if (progress > 0)
			graphics.blitSprite(PROGRESS_SPRITE, ARROW_WIDTH, ARROW_HEIGHT, 0, 0, leftPos + ARROW_X, topPos + ARROW_Y, progress, ARROW_HEIGHT);

		tank.render(graphics, leftPos, topPos, menu.getBottler().getFluid(), XpBottlerBlockEntity.getTankCapacity());
		tabs.render(graphics, font, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return tabs.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return tabs.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return tabs.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton) && !tabs.isMouseOver(mouseX, mouseY);
	}
}
