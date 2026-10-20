package openblocks.integration.patchouli;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import openblocks.integration.jei.JeiBookmarks;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

/**
 * Patchouli's {@code patchouli:crafting} page (up to two recipes, same layout) with a bookmark button per recipe; Patchouli
 * has no way to add widgets to its built-in pages. Each recipe slot takes a comma separated list and cycles through it once
 * a second, in step with the spotlight of colored items.
 */
public class CraftingComponent implements ICustomComponent {

	private static final int RECIPE_HEIGHT = 78;
	private static final int PAGE_CENTER = 58;
	// Patchouli's default book header color, the manual doesn't change it
	private static final int HEADER_COLOR = 0x333333;

	IVariable recipe;
	IVariable recipe2;

	private transient List<List<ResourceLocation>> ids;
	private transient List<Slot> slots;
	private transient int x;
	private transient int y;

	private record Slot(List<RecipeHolder<CraftingRecipe>> recipes, BookmarkButton button) {

		RecipeHolder<CraftingRecipe> current(IComponentRenderContext context) {
			return recipes.get(context.getTicksInBook() / 20 % recipes.size());
		}
	}

	@Override
	public void build(int componentX, int componentY, int pageNum) {
		x = componentX;
		y = componentY;
	}

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
		ids = new ArrayList<>();
		for (IVariable variable : new IVariable[] { recipe, recipe2 }) {
			final String list = variable == null? "" : lookup.apply(variable).asString("");
			if (list.isEmpty()) continue;
			final List<ResourceLocation> slot = new ArrayList<>();
			for (String id : list.split(","))
				slot.add(ResourceLocation.parse(id.strip()));
			ids.add(slot);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onDisplayed(IComponentRenderContext context) {
		slots = new ArrayList<>();
		final Level level = Minecraft.getInstance().level;
		if (level == null) return;

		for (List<ResourceLocation> slotIds : ids) {
			final List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
			for (ResourceLocation id : slotIds) {
				final RecipeHolder<?> holder = level.getRecipeManager().byKey(id).orElse(null);
				if (holder != null && holder.value() instanceof CraftingRecipe) recipes.add((RecipeHolder<CraftingRecipe>)holder);
			}
			if (!recipes.isEmpty()) slots.add(new Slot(recipes, new BookmarkButton()));
		}
	}

	@Override
	public void render(GuiGraphics graphics, IComponentRenderContext context, float partialTicks, int mouseX, int mouseY) {
		final Level level = Minecraft.getInstance().level;
		if (level == null) return;

		int recipeY = y;
		Component previousTitle = null;
		for (Slot slot : slots) {
			final RecipeHolder<CraftingRecipe> shown = slot.current(context);
			// like Patchouli, the second title is left out when it repeats the first
			Component title = shown.value().getResultItem(level.registryAccess()).getHoverName();
			if (title.equals(previousTitle)) {
				title = Component.empty();
				recipeY -= 10;
			}
			drawRecipe(graphics, context, level, slot, shown, title, recipeY, mouseX, mouseY);
			previousTitle = title;
			recipeY += RECIPE_HEIGHT;
		}
	}

	private void drawRecipe(GuiGraphics graphics, IComponentRenderContext context, Level level, Slot slot, RecipeHolder<CraftingRecipe> shown, Component title, int recipeY, int mouseX, int mouseY) {
		final CraftingRecipe recipe = shown.value();
		RenderSystem.enableBlend();
		graphics.blit(context.getCraftingTexture(), x - 2, recipeY - 2, 0, 0, 100, 62, 128, 256);

		final boolean shaped = recipe instanceof ShapedRecipe;
		if (!shaped) {
			final int iconX = x + 62;
			final int iconY = recipeY + 2;
			graphics.blit(context.getCraftingTexture(), iconX, iconY, 0, 64, 11, 11, 128, 256);
			if (context.isAreaHovered(mouseX, mouseY, iconX, iconY, 11, 11))
				context.setHoverTooltipComponents(List.of(Component.translatable("patchouli.gui.lexicon.shapeless")));
		}

		final Font font = Minecraft.getInstance().font;
		final Component styledTitle = title.copy().withStyle(context.getFont());
		graphics.drawString(font, styledTitle, PAGE_CENTER - font.width(styledTitle) / 2, recipeY - 10, HEADER_COLOR, false);

		final ItemStack result = recipe.getResultItem(level.registryAccess());
		context.renderItemStack(graphics, x + 79, recipeY + 22, mouseX, mouseY, result);

		final NonNullList<Ingredient> ingredients = recipe.getIngredients();
		final int wrap = shaped? ((ShapedRecipe)recipe).getWidth() : 3;
		for (int i = 0; i < ingredients.size(); i++)
			context.renderIngredient(graphics, x + i % wrap * 19 + 3, recipeY + i / wrap * 19 + 3, mouseX, mouseY, ingredients.get(i));

		context.renderItemStack(graphics, x + 79, recipeY + 41, mouseX, mouseY, recipe.getToastSymbol());

		// free corner above the result
		slot.button.render(graphics, context, x + 84, recipeY + 6, mouseX, mouseY, () -> JeiBookmarks.isBookmarked(shown));
	}

	@Override
	public boolean mouseClicked(IComponentRenderContext context, double mouseX, double mouseY, int mouseButton) {
		for (Slot slot : slots) {
			final RecipeHolder<CraftingRecipe> shown = slot.current(context);
			if (slot.button.click(() -> JeiBookmarks.toggle(shown))) return true;
		}
		return false;
	}
}
