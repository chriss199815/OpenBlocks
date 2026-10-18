package openblocks.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.block.TankBlock;
import openblocks.common.HeightMapData;
import openblocks.common.TrophyType;
import openblocks.item.EmptyMapItem;
import openblocks.item.GlyphItem;
import openblocks.item.PaintBrushItem;
import openblocks.item.ImaginaryItem;
import openblocks.item.ImaginaryPlacementMode;
import openblocks.item.ImaginationGlassesItem;
import openblocks.item.PaintCanItem;
import openblocks.util.PaintColors;
import openblocks.item.TrophyItem;

public class OBCreativeTabs {

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenBlocks.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.openblocks"))
			.icon(() -> new ItemStack(OBItems.HANG_GLIDER.get()))
			.displayItems((params, output) -> {
				OBItems.ITEMS.getEntries().forEach(item -> {
					if (item != OBItems.TROPHY && item != OBItems.HEIGHT_MAP && item != OBItems.EMPTY_MAP && item != OBItems.PENCIL && item != OBItems.CRAYON && item != OBItems.CRAYON_GLASSES && item != OBItems.GLYPH)
						output.accept(item.get());
				});
				// like legacy, without the largest scale
				for (int scale = 0; scale < HeightMapData.MAX_SCALE; scale++)
					output.accept(EmptyMapItem.create(scale));
				// like legacy, letters and digits; all characters only in the search tab, if enabled
				for (char ch : GlyphItem.DISPLAY_CHARS.toCharArray())
					output.accept(GlyphItem.create(ch, 1));
				if (OBConfig.GLYPHS.showInCreativeSearch.getAsBoolean())
					for (char ch : GlyphItem.CHARS.toCharArray())
						if (GlyphItem.DISPLAY_CHARS.indexOf(ch) < 0) output.accept(GlyphItem.create(ch, 1), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
				for (TrophyType type : TrophyType.values())
					output.accept(TrophyItem.create(OBItems.TROPHY.get(), type));
				for (DyeColor color : DyeColor.values()) {
					output.accept(PaintBrushItem.create(color));
					output.accept(PaintCanItem.create(color));
				}
				final float uses = OBConfig.IMAGINARY.numberOfUses.get().floatValue();
				output.accept(ImaginaryItem.create(null, ImaginaryPlacementMode.BLOCK, uses));
				for (DyeColor color : DyeColor.values())
					output.accept(ImaginaryItem.create(PaintColors.rgb(color), ImaginaryPlacementMode.BLOCK, uses));
				for (DyeColor color : DyeColor.values())
					output.accept(ImaginationGlassesItem.createCrayonGlasses(PaintColors.rgb(color)));

				if (OBConfig.TANKS.displayAllFilledTanks.getAsBoolean()) {
					for (Fluid fluid : BuiltInRegistries.FLUID)
						if (fluid.isSource(fluid.defaultFluidState()))
							output.accept(TankBlock.filledTank(OBBlocks.TANK.get(), new FluidStack(fluid, 1)), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
				}
			})
			.build());
}
