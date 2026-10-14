package openblocks.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import openblocks.OpenBlocks;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBItems;

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

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.XP_BOTTLER.get())
				.pattern("iii")
				.pattern("ibi")
				.pattern("iii")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('b', Items.GLASS_BOTTLE)
				.unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
				.save(output);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, OBBlocks.VACUUM_HOPPER.get())
				.requires(Items.HOPPER)
				.requires(Tags.Items.OBSIDIANS)
				.requires(Items.ENDER_EYE)
				.unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.ITEM_DROPPER.get())
				.pattern("icc")
				.pattern("src")
				.pattern("icc")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('c', Tags.Items.COBBLESTONES)
				.define('s', Items.HOPPER)
				.define('r', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy("has_hopper", has(Items.HOPPER))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.BLOCK_BREAKER.get())
				.pattern("icc")
				.pattern("src")
				.pattern("icc")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('c', Tags.Items.COBBLESTONES)
				.define('s', Items.DIAMOND_PICKAXE)
				.define('r', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy("has_diamond_pickaxe", has(Items.DIAMOND_PICKAXE))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.BLOCK_PLACER.get())
				.pattern("icc")
				.pattern("src")
				.pattern("icc")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('c', Tags.Items.COBBLESTONES)
				.define('s', Items.PISTON)
				.define('r', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy("has_piston", has(Items.PISTON))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.AUTO_ANVIL.get())
				.pattern("iii")
				.pattern("iai")
				.pattern("rrr")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('a', Items.ANVIL)
				.define('r', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy("has_anvil", has(Items.ANVIL))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.AUTO_ENCHANTMENT_TABLE.get())
				.pattern("iii")
				.pattern("iei")
				.pattern("rrr")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('e', Items.ENCHANTING_TABLE)
				.define('r', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy("has_enchanting_table", has(Items.ENCHANTING_TABLE))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.DONATION_STATION.get())
				.pattern("ppp")
				.pattern("pcp")
				.pattern("ppp")
				.define('p', Items.PORKCHOP)
				.define('c', Tags.Items.CHESTS_WOODEN)
				.unlockedBy("has_porkchop", has(Items.PORKCHOP))
				.save(output);

		// two mirrored wing recipes, like legacy
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBItems.GLIDER_WING.get())
				.group("openblocks:glider_wing")
				.pattern(" sl")
				.pattern("sll")
				.pattern("lll")
				.define('s', Tags.Items.RODS_WOODEN)
				.define('l', Tags.Items.LEATHERS)
				.unlockedBy("has_leather", has(Tags.Items.LEATHERS))
				.save(output, OpenBlocks.location("glider_wing_right"));

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBItems.GLIDER_WING.get())
				.group("openblocks:glider_wing")
				.pattern("ls ")
				.pattern("lls")
				.pattern("lll")
				.define('s', Tags.Items.RODS_WOODEN)
				.define('l', Tags.Items.LEATHERS)
				.unlockedBy("has_leather", has(Tags.Items.LEATHERS))
				.save(output, OpenBlocks.location("glider_wing_left"));

		ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, OBItems.HANG_GLIDER.get())
				.pattern("wsw")
				.define('w', OBItems.GLIDER_WING.get())
				.define('s', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_glider_wing", has(OBItems.GLIDER_WING.get()))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBItems.SLEEPING_BAG.get())
				.pattern("cc ")
				.pattern("www")
				.pattern("ccw")
				.define('c', Items.BLUE_CARPET)
				.define('w', Items.WHITE_WOOL)
				.unlockedBy("has_white_wool", has(Items.WHITE_WOOL))
				.save(output);
	}
}
