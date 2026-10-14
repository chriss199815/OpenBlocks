package openblocks.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import openblocks.OpenBlocks;
import openblocks.block.entity.AutoAnvilBlockEntity;
import openblocks.common.AutoSlotConfig;
import openblocks.menu.AutoAnvilMenu;

/**
 * Port of legacy {@code GuiAutoAnvil}: tool, modifier and output slots like the vanilla anvil, XP tank and one tab per auto slot.
 */
public class AutoAnvilScreen extends AbstractContainerScreen<AutoAnvilMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/auto_anvil.png");

	private static final int TOOL_TAB_COLOR = 0x8784c8;
	private static final int MODIFIER_TAB_COLOR = 0x84c7c8;
	private static final int OUTPUT_TAB_COLOR = 0x84c892;
	private static final int XP_TAB_COLOR = 0xc7c884;

	private final TankWidget tank = new TankWidget(151, 29, 17, 37);
	private final SideConfigTabs tabs = new SideConfigTabs();

	public AutoAnvilScreen(AutoAnvilMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		this.inventoryLabelY = imageHeight - 94;

		tabs.add(slotTab(AutoAnvilBlockEntity.AutoSlot.TOOL, TOOL_TAB_COLOR, new ItemStack(Items.DIAMOND_PICKAXE), "openblocks.gui.autoextract"));
		tabs.add(slotTab(AutoAnvilBlockEntity.AutoSlot.MODIFIER, MODIFIER_TAB_COLOR, new ItemStack(Items.ENCHANTED_BOOK), "openblocks.gui.autoextract"));
		tabs.add(slotTab(AutoAnvilBlockEntity.AutoSlot.XP, XP_TAB_COLOR, new ItemStack(Items.BUCKET), "openblocks.gui.autodrink"));
		tabs.add(slotTab(AutoAnvilBlockEntity.AutoSlot.OUTPUT, OUTPUT_TAB_COLOR, enchantedPickaxe(playerInventory), "openblocks.gui.autoeject"));
	}

	/**
	 * Enchantments are data driven, so the icon needs the registry.
	 */
	private static ItemStack enchantedPickaxe(Inventory playerInventory) {
		final ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
		final Holder<Enchantment> fortune = playerInventory.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
		stack.enchant(fortune, 1);
		return stack;
	}

	private SideConfigTabs.Tab slotTab(AutoAnvilBlockEntity.AutoSlot slot, int color, ItemStack icon, String label) {
		final AutoAnvilBlockEntity anvil = menu.getAnvil();
		final AutoSlotConfig<AutoAnvilBlockEntity.AutoSlot> config = anvil.getConfig();
		final SideSelectorWidget selector = new SideSelectorWidget(15, 22, 40, anvil, () -> config.getSides(slot), true,
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
			tank.renderTooltip(graphics, font, mouseX, mouseY, menu.getAnvil().getFluid(), AutoAnvilBlockEntity.getTankCapacity());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		tank.render(graphics, leftPos, topPos, menu.getAnvil().getFluid(), AutoAnvilBlockEntity.getTankCapacity());
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
