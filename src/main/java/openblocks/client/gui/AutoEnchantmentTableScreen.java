package openblocks.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
import openblocks.block.entity.AutoEnchantmentTableBlockEntity;
import openblocks.common.AutoSlotConfig;
import openblocks.menu.AutoEnchantmentTableMenu;

/**
 * Port of legacy {@code GuiAutoEnchantmentTable}: tool, lapis and output slots, available bookshelf power, power limit slider,
 * a button choosing one of the three vanilla options, the XP tank and one tab per auto slot.
 */
public class AutoEnchantmentTableScreen extends AbstractContainerScreen<AutoEnchantmentTableMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/auto_enchantment_table.png");
	private static final ResourceLocation[] OPTION_SPRITES = {
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1"),
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2"),
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3")
	};

	private static final int TOOL_TAB_COLOR = 0x8784c8;
	private static final int LAPIS_TAB_COLOR = 0x8784c8;
	private static final int OUTPUT_TAB_COLOR = 0x84c7c8;
	private static final int XP_TAB_COLOR = 0x84c892;

	private static final int CONTROLS_X = 38;
	private static final int POWER_Y = 20;
	private static final int SLIDER_Y = 32;
	private static final int SLIDER_WIDTH = 62;
	private static final int OPTION_X = CONTROLS_X + SLIDER_WIDTH + 4;
	private static final int OPTION_SIZE = 20;

	private final TankWidget tank = new TankWidget(151, 29, 17, 37);
	private final SideConfigTabs tabs = new SideConfigTabs();

	private LimitSlider slider;
	private Button optionButton;

	public AutoEnchantmentTableScreen(AutoEnchantmentTableMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		this.inventoryLabelY = imageHeight - 94;

		tabs.add(slotTab(AutoEnchantmentTableBlockEntity.AutoSlot.TOOL, TOOL_TAB_COLOR, new ItemStack(Items.DIAMOND_PICKAXE), "openblocks.gui.autoextract"));
		tabs.add(slotTab(AutoEnchantmentTableBlockEntity.AutoSlot.LAPIS, LAPIS_TAB_COLOR, new ItemStack(Items.LAPIS_LAZULI), "openblocks.gui.autoextract"));
		tabs.add(slotTab(AutoEnchantmentTableBlockEntity.AutoSlot.XP, XP_TAB_COLOR, new ItemStack(Items.BUCKET), "openblocks.gui.autodrink"));
		tabs.add(slotTab(AutoEnchantmentTableBlockEntity.AutoSlot.OUTPUT, OUTPUT_TAB_COLOR, enchantedPickaxe(playerInventory), "openblocks.gui.autoeject"));
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

	private SideConfigTabs.Tab slotTab(AutoEnchantmentTableBlockEntity.AutoSlot slot, int color, ItemStack icon, String label) {
		final AutoEnchantmentTableBlockEntity table = menu.getTable();
		final AutoSlotConfig<AutoEnchantmentTableBlockEntity.AutoSlot> config = table.getConfig();
		final SideSelectorWidget selector = new SideSelectorWidget(15, 22, 40, table, () -> config.getSides(slot), true,
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
		slider = addRenderableWidget(new LimitSlider(leftPos + CONTROLS_X, topPos + SLIDER_Y));
		optionButton = addRenderableWidget(Button.builder(Component.empty(), button -> clickButton(menu.optionButtonId((menu.getOption() + 1) % 3)))
				.bounds(leftPos + OPTION_X, topPos + SLIDER_Y, OPTION_SIZE, OPTION_SIZE)
				.tooltip(Tooltip.create(Component.translatable("openblocks.gui.enchantment_option")))
				.build());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		slider.sync();
	}

	public List<Rect2i> getExtraAreas() {
		return tabs.getAreas();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		// the vanilla level icon on the option button
		graphics.blitSprite(OPTION_SPRITES[menu.getOption()], optionButton.getX() + 2, optionButton.getY() + 2, 16, 16);
		renderTooltip(graphics, mouseX, mouseY);
		if (tank.isMouseOver(leftPos, topPos, mouseX, mouseY))
			tank.renderTooltip(graphics, font, mouseX, mouseY, menu.getTable().getFluid(), AutoEnchantmentTableBlockEntity.getTankCapacity());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		tank.render(graphics, leftPos, topPos, menu.getTable().getFluid(), AutoEnchantmentTableBlockEntity.getTankCapacity());
		tabs.render(graphics, font, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		graphics.drawString(font, Component.translatable("openblocks.gui.available_power", menu.getAvailablePower()), CONTROLS_X, POWER_Y, 0x404040, false);
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

	/**
	 * Caps the bookshelf power, so lower enchantment levels can be chosen.
	 */
	private class LimitSlider extends AbstractSliderButton {

		private int limit = -1;
		private boolean dragging;

		private LimitSlider(int x, int y) {
			super(x, y, SLIDER_WIDTH, 20, Component.empty(), 0);
			sync();
		}

		/**
		 * Takes over the server's value, unless the player is moving the slider.
		 */
		private void sync() {
			if (dragging || menu.getPowerLimit() == limit) return;
			limit = menu.getPowerLimit();
			value = (limit - 1) / (double)(AutoEnchantmentTableBlockEntity.MAX_POWER - 1);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("openblocks.gui.limit").append(": " + Math.max(limit, 1)));
		}

		@Override
		protected void applyValue() {
			final int newLimit = 1 + (int)Math.round(value * (AutoEnchantmentTableBlockEntity.MAX_POWER - 1));
			if (newLimit == limit) return;
			limit = newLimit;
			clickButton(menu.limitButtonId(limit));
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
