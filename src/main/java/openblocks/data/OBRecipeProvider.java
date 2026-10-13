package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import openblocks.registry.OBBlocks;

public class OBRecipeProvider extends RecipeProvider {

	public OBRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	private static Item wool(DyeColor color) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_wool"));
	}

	@Override
	protected void buildRecipes(RecipeOutput output) {
		OBBlocks.ELEVATORS.forEach((color, block) -> ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, block.get())
				.group("openblocks:elevator")
				.pattern("www")
				.pattern("wew")
				.pattern("www")
				.define('w', wool(color))
				.define('e', Tags.Items.ENDER_PEARLS)
				.unlockedBy("has_ender_pearl", has(Tags.Items.ENDER_PEARLS))
				.save(output));

		OBBlocks.ROTATING_ELEVATORS.forEach((color, block) -> ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, block.get())
				.group("openblocks:elevator_rotating")
				.pattern("wiw")
				.pattern("wew")
				.pattern("wiw")
				.define('w', wool(color))
				.define('i', Tags.Items.INGOTS_IRON)
				.define('e', Tags.Items.ENDER_PEARLS)
				.unlockedBy("has_ender_pearl", has(Tags.Items.ENDER_PEARLS))
				.save(output));

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.TANK.get(), 2)
				.pattern("ogo")
				.pattern("ggg")
				.pattern("ogo")
				.define('o', Items.OBSIDIAN)
				.define('g', Tags.Items.GLASS_PANES)
				.unlockedBy("has_obsidian", has(Items.OBSIDIAN))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.XP_DRAIN.get())
				.pattern("iii")
				.pattern("iii")
				.pattern("iii")
				.define('i', Items.IRON_BARS)
				.unlockedBy("has_iron_bars", has(Items.IRON_BARS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.XP_SHOWER.get())
				.pattern("iii")
				.pattern("  o")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('o', Items.OBSIDIAN)
				.unlockedBy("has_obsidian", has(Items.OBSIDIAN))
				.save(output);
	}
}
