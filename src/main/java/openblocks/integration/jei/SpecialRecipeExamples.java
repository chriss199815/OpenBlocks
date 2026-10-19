package openblocks.integration.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import openblocks.OpenBlocks;
import openblocks.common.HeightMapData;
import openblocks.item.EmptyMapItem;
import openblocks.item.GoldenEyeItem;
import openblocks.item.ImaginaryItem;
import openblocks.item.ImaginaryPlacementMode;
import openblocks.recipe.CrayonGlassesRecipe;
import openblocks.recipe.CrayonMergeRecipe;
import openblocks.recipe.CrayonMixingRecipe;
import openblocks.recipe.EpicEraserRecipe;
import openblocks.recipe.GoldenEyeRechargeRecipe;
import openblocks.recipe.MapResizeRecipe;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.util.PaintColors;

/**
 * JEI skips special recipes ({@link CustomRecipe}), so the crafting category gets fixed examples of them, like JEI does for
 * vanilla's shulker box coloring. Results come from the real recipes, so the examples can't drift from them.
 */
class SpecialRecipeExamples {

	private final HolderLookup.Provider registries;
	private final List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

	private SpecialRecipeExamples(HolderLookup.Provider registries) {
		this.registries = registries;
	}

	static List<RecipeHolder<CraftingRecipe>> create(HolderLookup.Provider registries) {
		final SpecialRecipeExamples examples = new SpecialRecipeExamples(registries);
		examples.goldenEyeRecharge();
		examples.mapClone();
		examples.mapResize();
		examples.crayons();
		examples.epicEraser();
		return examples.recipes;
	}

	private void goldenEyeRecharge() {
		final GoldenEyeRechargeRecipe recipe = new GoldenEyeRechargeRecipe(CraftingBookCategory.MISC);
		final ItemStack eye = GoldenEyeItem.createEmpty(OBItems.GOLDEN_EYE.get());
		for (int pearls : new int[] { 1, 8 }) {
			final ItemStack[] inputs = new ItemStack[pearls + 1];
			inputs[0] = eye;
			Arrays.fill(inputs, 1, inputs.length, new ItemStack(Items.ENDER_PEARL));
			shapeless("golden_eye_recharge", pearls, recipe, inputs);
		}
	}

	/**
	 * Not from {@link openblocks.recipe.MapCloneRecipe}: that one needs a height map with data, and its id would make the
	 * tooltip ask the server for the map.
	 */
	private void mapClone() {
		for (int scale = 0; scale <= HeightMapData.MAX_SCALE; scale++) {
			final ItemStack heightMap = new ItemStack(OBItems.HEIGHT_MAP.get());
			heightMap.set(OBComponents.MAP_SCALE, scale);
			add("map_clone", scale, new ShapelessRecipe("", CraftingBookCategory.MISC, heightMap.copyWithCount(2),
					NonNullList.of(Ingredient.EMPTY, Ingredient.of(heightMap), Ingredient.of(EmptyMapItem.create(scale)))));
		}
	}

	private void mapResize() {
		final MapResizeRecipe recipe = new MapResizeRecipe(CraftingBookCategory.MISC);
		final ItemStack memory = new ItemStack(OBItems.MAP_MEMORY.get());
		for (int scale = 0; scale < HeightMapData.MAX_SCALE; scale++) {
			final List<ItemStack> inputs = List.of(memory, EmptyMapItem.create(scale), memory);
			final ShapedRecipePattern pattern = new ShapedRecipePattern(3, 1, toIngredients(inputs), Optional.empty());
			add("map_resize", scale, new ShapedRecipe("", CraftingBookCategory.MISC, pattern, assemble("map_resize", recipe, 3, inputs)));
		}
	}

	private void crayons() {
		final ItemStack red = crayon(DyeColor.RED);
		final ItemStack blue = crayon(DyeColor.BLUE);
		final ItemStack yellow = crayon(DyeColor.YELLOW);

		final CrayonMergeRecipe merge = new CrayonMergeRecipe(CraftingBookCategory.MISC);
		shapeless("pencil_merge", 0, merge, pencil(), pencil());
		shapeless("crayon_merge", 0, merge, red, red);

		final CrayonMixingRecipe mixing = new CrayonMixingRecipe(CraftingBookCategory.MISC);
		shapeless("crayon_mix", 0, mixing, red, blue);
		shapeless("crayon_mix", 1, mixing, red, yellow);
		shapeless("crayon_mix", 2, mixing, blue, yellow);

		shapeless("crayon_glasses", 0, new CrayonGlassesRecipe(CraftingBookCategory.MISC), red, new ItemStack(Items.PAPER));
	}

	private void epicEraser() {
		final ItemStack withLore = new ItemStack(Items.DIAMOND_SWORD);
		withLore.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("openblocks.jei.example_lore"))));
		shapeless("epic_eraser", 0, new EpicEraserRecipe(CraftingBookCategory.MISC), new ItemStack(OBItems.EPIC_ERASER.get()), withLore);
	}

	private static ItemStack crayon(DyeColor color) {
		return ImaginaryItem.create(PaintColors.rgb(color), ImaginaryPlacementMode.BLOCK, ImaginaryItem.DEFAULT_USES);
	}

	private static ItemStack pencil() {
		return ImaginaryItem.create(null, ImaginaryPlacementMode.BLOCK, ImaginaryItem.DEFAULT_USES);
	}

	private void shapeless(String name, int index, CustomRecipe recipe, ItemStack... inputs) {
		final List<ItemStack> stacks = List.of(inputs);
		add(name, index, new ShapelessRecipe("", CraftingBookCategory.MISC, assemble(name, recipe, stacks.size(), stacks), toIngredients(stacks)));
	}

	private ItemStack assemble(String name, CustomRecipe recipe, int width, List<ItemStack> inputs) {
		final ItemStack result = recipe.assemble(CraftingInput.of(width, inputs.size() / width, inputs), registries);
		if (result.isEmpty()) throw new IllegalStateException("Example of " + name + " doesn't match its recipe");
		return result;
	}

	private void add(String name, int index, CraftingRecipe recipe) {
		final ResourceLocation id = OpenBlocks.location("jei/" + name + "_" + index);
		recipes.add(new RecipeHolder<>(id, recipe));
	}

	/**
	 * JEI shows the exact stacks (with charge, scale or color), matching doesn't matter for an example.
	 */
	private static NonNullList<Ingredient> toIngredients(List<ItemStack> stacks) {
		final NonNullList<Ingredient> ingredients = NonNullList.create();
		for (ItemStack stack : stacks)
			ingredients.add(Ingredient.of(stack));
		return ingredients;
	}
}
