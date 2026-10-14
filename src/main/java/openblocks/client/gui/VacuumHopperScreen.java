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
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.menu.VacuumHopperMenu;

/**
 * Port of legacy {@code GuiVacuumHopper}: inventory, XP tank and one tab each for the item and XP output sides.
 */
public class VacuumHopperScreen extends AbstractContainerScreen<VacuumHopperMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/vacuum_hopper.png");

	private static final int ITEMS_TAB_COLOR = 0x84c7c8;
	private static final int XP_TAB_COLOR = 0x8784c8;

	private final TankWidget tank = new TankWidget(140, 18, 17, 37);
	private final SideConfigTabs tabs = new SideConfigTabs();

	public VacuumHopperScreen(VacuumHopperMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 151;
		this.inventoryLabelY = imageHeight - 94;

		tabs.add(outputTab(VacuumHopperBlockEntity.Output.ITEMS, ITEMS_TAB_COLOR, new ItemStack(Items.CHEST), "openblocks.gui.item_outputs"));
		tabs.add(outputTab(VacuumHopperBlockEntity.Output.XP, XP_TAB_COLOR, new ItemStack(Items.EXPERIENCE_BOTTLE), "openblocks.gui.xp_outputs"));
	}

	private SideConfigTabs.Tab outputTab(VacuumHopperBlockEntity.Output output, int color, ItemStack icon, String label) {
		final VacuumHopperBlockEntity hopper = menu.getHopper();
		// the nozzles of the block model already show the outputs, so no extra highlight
		final SideSelectorWidget selector = new SideSelectorWidget(15, 22, 40, hopper, () -> hopper.getOutputs(output), false,
				side -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, VacuumHopperMenu.toggleButtonId(output, side)));
		return new SideConfigTabs.Tab(color, icon, Component.translatable(label), selector);
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
			tank.renderTooltip(graphics, font, mouseX, mouseY, menu.getHopper().getFluid(), VacuumHopperBlockEntity.getTankCapacity());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		tank.render(graphics, leftPos, topPos, menu.getHopper().getFluid(), VacuumHopperBlockEntity.getTankCapacity());
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
