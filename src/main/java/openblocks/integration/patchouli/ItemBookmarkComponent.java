package openblocks.integration.patchouli;

import java.util.function.UnaryOperator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import openblocks.integration.jei.JeiBookmarks;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

/**
 * Bookmark button of the spotlight template: bookmarks the item currently shown (spotlights cycle through the colors once a
 * second, like Patchouli's item component).
 */
public class ItemBookmarkComponent implements ICustomComponent {

	IVariable item;

	private transient ItemStack[] stacks;
	private transient int x;
	private transient int y;
	private transient BookmarkButton button;

	@Override
	public void build(int componentX, int componentY, int pageNum) {
		x = componentX;
		y = componentY;
		button = new BookmarkButton();
	}

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
		stacks = lookup.apply(item).as(ItemStack[].class);
	}

	@Override
	public void render(GuiGraphics graphics, IComponentRenderContext context, float partialTicks, int mouseX, int mouseY) {
		if (stacks.length == 0) return;
		final ItemStack stack = stacks[context.getTicksInBook() / 20 % stacks.length];
		button.render(graphics, context, x, y, mouseX, mouseY, () -> JeiBookmarks.isBookmarked(stack));
	}

	@Override
	public boolean mouseClicked(IComponentRenderContext context, double mouseX, double mouseY, int mouseButton) {
		if (stacks.length == 0) return false;
		final ItemStack stack = stacks[context.getTicksInBook() / 20 % stacks.length];
		return button.click(() -> JeiBookmarks.toggle(stack));
	}
}
