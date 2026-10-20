package openblocks.integration.jei;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import openblocks.OpenBlocks;
import openblocks.client.gui.AutoAnvilScreen;
import openblocks.client.gui.AutoEnchantmentTableScreen;
import openblocks.client.gui.VacuumHopperScreen;
import openblocks.client.gui.XpBottlerScreen;
import openblocks.item.Paint;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

/**
 * Replaces the legacy NEI plugin ({@code NEIOpenBlocksConfig}), which hid internal items from the item list. Also shows
 * examples of the special crafting recipes, see {@link SpecialRecipeExamples}.
 */
@JeiPlugin
public class OpenBlocksJeiPlugin implements IModPlugin {

	private static final ResourceLocation UID = OpenBlocks.location("jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	/**
	 * Items whose variants differ by a component; without these JEI shows only one of each (and warns about duplicates in
	 * the creative tab). Paint and crayon colors count, not how much is left.
	 */
	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		registerComponentSubtype(registration, OBItems.GLYPH.get(), OBComponents.GLYPH.get());
		registerComponentSubtype(registration, OBItems.EMPTY_MAP.get(), OBComponents.MAP_SCALE.get());
		registerComponentSubtype(registration, OBItems.TROPHY.get(), OBComponents.TROPHY.get());
		registerComponentSubtype(registration, OBItems.CRAYON.get(), OBComponents.CRAYON_COLOR.get());
		registerComponentSubtype(registration, OBItems.CRAYON_GLASSES.get(), OBComponents.CRAYON_COLOR.get());
		final ISubtypeInterpreter<ItemStack> paintColor = componentInterpreter(stack -> {
			final Paint paint = stack.get(OBComponents.PAINT);
			return paint != null? paint.color() : null;
		});
		registration.registerSubtypeInterpreter(OBItems.PAINT_BRUSH.get(), paintColor);
		registration.registerSubtypeInterpreter(OBItems.PAINT_CAN.get(), paintColor);
	}

	private static void registerComponentSubtype(ISubtypeRegistration registration, Item item, DataComponentType<?> component) {
		registration.registerSubtypeInterpreter(item, componentInterpreter(stack -> stack.get(component)));
	}

	private static ISubtypeInterpreter<ItemStack> componentInterpreter(Function<ItemStack, Object> subtype) {
		return new ISubtypeInterpreter<>() {
			@Nullable
			@Override
			public Object getSubtypeData(ItemStack stack, UidContext context) {
				return subtype.apply(stack);
			}

			@Override
			public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
				return String.valueOf(subtype.apply(stack));
			}
		};
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		// JEI only starts in a world
		final var registries = Objects.requireNonNull(Minecraft.getInstance().level).registryAccess();
		registration.addRecipes(RecipeTypes.CRAFTING, SpecialRecipeExamples.create(registries));
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		// keeps the item list clear of the side configuration tabs
		registration.addGuiContainerHandler(VacuumHopperScreen.class, new IGuiContainerHandler<>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(VacuumHopperScreen screen) {
				return screen.getExtraAreas();
			}
		});
		registration.addGuiContainerHandler(AutoAnvilScreen.class, new IGuiContainerHandler<>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(AutoAnvilScreen screen) {
				return screen.getExtraAreas();
			}
		});
		registration.addGuiContainerHandler(AutoEnchantmentTableScreen.class, new IGuiContainerHandler<>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(AutoEnchantmentTableScreen screen) {
				return screen.getExtraAreas();
			}
		});
		registration.addGuiContainerHandler(XpBottlerScreen.class, new IGuiContainerHandler<>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(XpBottlerScreen screen) {
				return screen.getExtraAreas();
			}
		});
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		List<ItemStack> hidden = hiddenItems();
		if (!hidden.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
		JeiBookmarks.setRuntime(runtime);
	}

	@Override
	public void onRuntimeUnavailable() {
		JeiBookmarks.setRuntime(null);
	}

	/**
	 * A height map without data is useless; canvas glass, also hidden by legacy, no longer has an item.
	 */
	private static List<ItemStack> hiddenItems() {
		return List.of(new ItemStack(OBItems.HEIGHT_MAP.get()));
	}
}
