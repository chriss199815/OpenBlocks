package openblocks.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import openblocks.OpenBlocks;
import openblocks.block.entity.XpBottlerBlockEntity;
import openblocks.menu.XpBottlerMenu;

/**
 * Port of legacy {@code GuiXPBottler}, without the side configuration tabs (see PORTING_PLAN).
 */
public class XpBottlerScreen extends AbstractContainerScreen<XpBottlerMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/xp_bottler.png");
	private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final ResourceLocation PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

	private static final int ARROW_X = 75;
	private static final int ARROW_Y = 30;
	private static final int ARROW_WIDTH = 24;
	private static final int ARROW_HEIGHT = 16;

	private final TankWidget tank = new TankWidget(140, 18, 17, 37);

	public XpBottlerScreen(XpBottlerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 151;
		this.inventoryLabelY = imageHeight - 94;
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
	}
}
