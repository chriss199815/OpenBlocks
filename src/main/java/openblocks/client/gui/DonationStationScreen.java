package openblocks.client.gui;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import openblocks.OpenBlocks;
import openblocks.common.DonationInfo;
import openblocks.menu.DonationStationMenu;

/**
 * Port of legacy {@code GuiDonationStation}: shows the mod and authors of the item in the slot, with a button to their donation (or mod) page.
 * Long texts are cut off and shown in full as a tooltip, instead of the legacy half-size author line.
 */
public class DonationStationScreen extends AbstractContainerScreen<DonationStationMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/donation_station.png");

	private static final int TEXT_X = 55;
	private static final int TEXT_WIDTH = 113;
	private static final int MOD_NAME_Y = DonationStationMenu.SLOT_Y + 1;
	private static final int AUTHORS_Y = MOD_NAME_Y + 11;
	private static final int BUTTON_X = 30;
	private static final int BUTTON_Y = 55;
	private static final int BUTTON_WIDTH = 116;

	private Button button;
	// the stack the info was looked up for; null until the first lookup
	@Nullable
	private ItemStack shownStack;
	private Optional<DonationInfo> info = Optional.empty();

	public DonationStationScreen(DonationStationMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 172;
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		button = addRenderableWidget(Button.builder(Component.translatable("openblocks.gui.donate"), b -> openUrl())
				.bounds(leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, 20)
				.build());
		shownStack = null;
		updateInfo();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		updateInfo();
	}

	private void updateInfo() {
		final ItemStack stack = menu.getDonation();
		if (shownStack != null && ItemStack.isSameItem(stack, shownStack)) return;
		shownStack = stack.copy();
		info = DonationInfo.of(stack);
		button.active = info.map(DonationInfo::url).isPresent();
		button.setMessage(Component.translatable(info.map(DonationInfo::isDonationUrl).orElse(true)? "openblocks.gui.donate" : "openblocks.gui.mod_page"));
	}

	private void openUrl() {
		info.map(DonationInfo::url).ifPresent(url -> {
			// like clicking a link in chat
			if (minecraft.options.chatLinksPrompt().get()) ConfirmLinkScreen.confirmLinkNow(this, url);
			else Util.getPlatform().openUri(url);
		});
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		info.ifPresent(donation -> {
			renderCutOffTooltip(graphics, donation.modName(), MOD_NAME_Y, mouseX, mouseY);
			renderCutOffTooltip(graphics, donation.authors(), AUTHORS_Y, mouseX, mouseY);
		});
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		if (menu.getDonation().isEmpty()) return;
		final String modName = info.map(DonationInfo::modName).orElseGet(() -> Component.translatable("openblocks.gui.unknown_mod").getString());
		drawCutOff(graphics, modName, MOD_NAME_Y);
		info.ifPresent(donation -> drawCutOff(graphics, donation.authors(), AUTHORS_Y));
	}

	private void drawCutOff(GuiGraphics graphics, String text, int y) {
		final FormattedText line = font.width(text) <= TEXT_WIDTH? FormattedText.of(text)
				: FormattedText.composite(font.substrByWidth(FormattedText.of(text), TEXT_WIDTH - font.width(CommonComponents.ELLIPSIS)), CommonComponents.ELLIPSIS);
		graphics.drawString(font, Language.getInstance().getVisualOrder(line), TEXT_X, y, 0x404040, false);
	}

	private void renderCutOffTooltip(GuiGraphics graphics, String text, int y, int mouseX, int mouseY) {
		if (font.width(text) > TEXT_WIDTH && isHovering(TEXT_X, y, TEXT_WIDTH, font.lineHeight, mouseX, mouseY))
			graphics.renderTooltip(font, font.split(Component.literal(text), 200), mouseX, mouseY);
	}
}
