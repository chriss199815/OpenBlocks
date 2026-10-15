package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.recipe.GoldenEyeRechargeRecipe;
import openblocks.recipe.MapCloneRecipe;
import openblocks.recipe.MapResizeRecipe;

/**
 * Serializers of the custom recipes; replaces legacy {@code CustomRecipesSetup}.
 */
public class OBRecipeSerializers {

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, OpenBlocks.MODID);

	public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<GoldenEyeRechargeRecipe>> GOLDEN_EYE_RECHARGE = RECIPE_SERIALIZERS.register("golden_eye_recharge",
			() -> new SimpleCraftingRecipeSerializer<>(GoldenEyeRechargeRecipe::new));

	public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<MapCloneRecipe>> MAP_CLONE = RECIPE_SERIALIZERS.register("map_clone",
			() -> new SimpleCraftingRecipeSerializer<>(MapCloneRecipe::new));

	public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<MapResizeRecipe>> MAP_RESIZE = RECIPE_SERIALIZERS.register("map_resize",
			() -> new SimpleCraftingRecipeSerializer<>(MapResizeRecipe::new));
}
