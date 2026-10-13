package openblocks.client.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Fluid level display for screens; replaces OpenModsLib {@code GuiComponentTankLevel}.
 * The frame is part of the screen background, this only draws the fluid and the tooltip.
 */
public class TankWidget {

	private static final int BORDER = 1;

	private final int x;
	private final int y;
	private final int width;
	private final int height;

	/**
	 * Coordinates are those of the frame, relative to the screen's top left corner.
	 */
	public TankWidget(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void render(GuiGraphics graphics, int left, int top, FluidStack fluid, int capacity) {
		if (fluid.isEmpty() || capacity <= 0) return;

		final IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
		final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
		final int color = extensions.getTintColor(fluid);
		final float r = ((color >> 16) & 0xFF) / 255f;
		final float g = ((color >> 8) & 0xFF) / 255f;
		final float b = (color & 0xFF) / 255f;

		final int innerWidth = width - 2 * BORDER;
		final int innerHeight = height - 2 * BORDER;
		final int fluidHeight = Math.max(1, Math.round(innerHeight * Math.min(1.0f, (float)fluid.getAmount() / capacity)));
		final int bottom = top + y + BORDER + innerHeight;
		final int x0 = left + x + BORDER;

		// tile whole 16x16 sprites from the bottom up and cut them to the fluid level, so they aren't squashed
		graphics.enableScissor(x0, bottom - fluidHeight, x0 + innerWidth, bottom);
		for (int drawn = 0; drawn < fluidHeight; drawn += 16)
			for (int dx = 0; dx < innerWidth; dx += 16)
				graphics.blit(x0 + dx, bottom - drawn - 16, 0, 16, 16, sprite, r, g, b, 1.0f);
		graphics.disableScissor();
	}

	public boolean isMouseOver(int left, int top, double mouseX, double mouseY) {
		return mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height;
	}

	public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, FluidStack fluid, int capacity) {
		final List<Component> lines = fluid.isEmpty()
				? List.of(Component.literal(String.format("0/%d", capacity)))
				: List.of(fluid.getHoverName(), Component.literal(String.format("%d/%d", fluid.getAmount(), capacity)));
		graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
	}
}
