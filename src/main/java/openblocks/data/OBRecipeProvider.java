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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import openblocks.OpenBlocks;
import openblocks.item.PaintBrushItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBItems;

public class OBRecipeProvider extends RecipeProvider {

	public OBRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	private static Item wool(DyeColor color) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_wool"));
	}

	private static Item carpet(DyeColor color) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_carpet"));
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

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.PAINT_MIXER.get())
				.pattern("ooo")
				.pattern("i i")
				.pattern("iii")
				.define('o', Tags.Items.OBSIDIANS)
				.define('i', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_obsidian", has(Tags.Items.OBSIDIANS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.FAN.get())
				.pattern("b")
				.pattern("i")
				.pattern("s")
				.define('b', Items.IRON_BARS)
				.define('i', Tags.Items.INGOTS_IRON)
				.define('s', Items.SMOOTH_STONE_SLAB)
				.unlockedBy("has_iron_bars", has(Items.IRON_BARS))
				.save(output);

		OBBlocks.FLAGS.forEach((color, flag) -> {
			final Item carpet = carpet(color);
			ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, flag.get())
					.group("openblocks:flag")
					.pattern("scc")
					.pattern("sc ")
					.pattern("s  ")
					.define('c', carpet)
					.define('s', Tags.Items.RODS_WOODEN)
					.unlockedBy("has_carpet", has(carpet))
					.save(output);
		});

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.BEAR_TRAP.get())
				.pattern("fif")
				.pattern("fif")
				.pattern("fif")
				.define('f', Items.IRON_BARS)
				.define('i', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_iron_bars", has(Items.IRON_BARS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.SPRINKLER.get())
				.pattern("ifi")
				.pattern("iri")
				.pattern("ifi")
				.define('i', Tags.Items.INGOTS_IRON)
				.define('f', Items.IRON_BARS)
				.define('r', Items.REDSTONE_TORCH)
				.unlockedBy("has_iron_bars", has(Items.IRON_BARS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBItems.LUGGAGE.get())
				.pattern("sds")
				.pattern("scs")
				.pattern("sss")
				.define('s', Tags.Items.RODS_WOODEN)
				.define('c', Tags.Items.CHESTS_WOODEN)
				.define('d', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
				.save(output);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, OBItems.DEV_NULL.get())
				.requires(Tags.Items.COBBLESTONES)
				.requires(Items.APPLE)
				.unlockedBy("has_apple", has(Items.APPLE))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, OBItems.CURSOR.get())
				.pattern("w  ")
				.pattern("www")
				.pattern("www")
				.define('w', Items.WHITE_WOOL)
				.unlockedBy("has_wool", has(Items.WHITE_WOOL))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, OBItems.PAINT_BRUSH.get())
				.pattern("w  ")
				.pattern(" s ")
				.pattern("  s")
				.define('w', Items.WHITE_WOOL)
				.define('s', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_wool", has(Items.WHITE_WOOL))
				.save(output);

		// dipping a brush into dye, like legacy
		for (DyeColor color : DyeColor.values()) {
			ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, PaintBrushItem.create(color))
					.group("openblocks:paintbrush")
					.requires(OBItems.PAINT_BRUSH.get())
					.requires(color.getTag())
					.unlockedBy("has_paintbrush", has(OBItems.PAINT_BRUSH.get()))
					.save(output, OpenBlocks.location("paintbrush_" + color.getSerializedName()));
		}

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, OBBlocks.CANVAS.get(), 9)
				.pattern("ppp")
				.pattern("pfp")
				.pattern("ppp")
				.define('p', Items.PAPER)
				.define('f', Tags.Items.FENCES_WOODEN)
				.unlockedBy("has_paper", has(Items.PAPER))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, OBItems.SQUEEGEE.get())
				.pattern("sss")
				.pattern(" w ")
				.pattern(" w ")
				.define('s', OBBlocks.SPONGE.get())
				.define('w', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_sponge", has(OBBlocks.SPONGE.get()))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBItems.UNPREPARED_STENCIL.get())
				.pattern(" p ")
				.pattern("pip")
				.pattern(" p ")
				.define('p', Items.PAPER)
				.define('i', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_paper", has(Items.PAPER))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBItems.SKETCHING_PENCIL.get())
				.pattern("c  ")
				.pattern(" s ")
				.pattern("  s")
				.define('c', ItemTags.COALS)
				.define('s', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_coal", has(ItemTags.COALS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, OBBlocks.DRAWING_TABLE.get())
				.pattern("sks")
				.pattern("pcp")
				.pattern("ppp")
				.define('s', OBItems.UNPREPARED_STENCIL.get())
				.define('k', OBItems.SKETCHING_PENCIL.get())
				.define('p', ItemTags.PLANKS)
				.define('c', Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
				.unlockedBy("has_unprepared_stencil", has(OBItems.UNPREPARED_STENCIL.get()))
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

		ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, OBBlocks.LADDER.get())
				.requires(Items.LADDER)
				.requires(ItemTags.WOODEN_TRAPDOORS)
				.unlockedBy("has_ladder", has(Items.LADDER))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, OBBlocks.ROPE_LADDER.get(), 8)
				.pattern("sts")
				.pattern("sts")
				.pattern("sts")
				.define('s', Tags.Items.STRINGS)
				.define('t', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_string", has(Tags.Items.STRINGS))
				.save(output);

		// any wool, like the legacy recipes for all 16 wool metadata values
		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, OBBlocks.SPONGE.get())
				.requires(ItemTags.WOOL)
				.requires(Tags.Items.SLIME_BALLS)
				.unlockedBy("has_slime_ball", has(Tags.Items.SLIME_BALLS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, OBBlocks.SCAFFOLDING.get(), 8)
				.pattern("sss")
				.pattern("s s")
				.pattern("sss")
				.define('s', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_stick", has(Tags.Items.RODS_WOODEN))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.BIG_BUTTON.get())
				.pattern("bb")
				.pattern("bb")
				.define('b', Items.STONE_BUTTON)
				.unlockedBy("has_stone_button", has(Items.STONE_BUTTON))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.BIG_BUTTON_WOOD.get())
				.pattern("bb")
				.pattern("bb")
				.define('b', ItemTags.WOODEN_BUTTONS)
				.unlockedBy("has_wooden_button", has(ItemTags.WOODEN_BUTTONS))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OBBlocks.TARGET.get())
				.pattern("www")
				.pattern("www")
				.pattern("s s")
				.define('w', Items.WHITE_WOOL)
				.define('s', Tags.Items.RODS_WOODEN)
				.unlockedBy("has_wool", has(Items.WHITE_WOOL))
				.save(output);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, OBBlocks.PATH.get(), 2)
				.requires(Tags.Items.STONES)
				.requires(Tags.Items.COBBLESTONES)
				.unlockedBy("has_cobblestone", has(Tags.Items.COBBLESTONES))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, OBItems.WRENCH.get())
				.pattern(" ii")
				.pattern("iii")
				.pattern("ii ")
				.define('i', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, OBItems.PEDOMETER.get())
				.pattern("www")
				.pattern("rcr")
				.pattern("www")
				.define('w', ItemTags.PLANKS)
				.define('r', Tags.Items.DUSTS_REDSTONE)
				.define('c', Items.CLOCK)
				.unlockedBy("has_clock", has(Items.CLOCK))
				.save(output);
	}
}
