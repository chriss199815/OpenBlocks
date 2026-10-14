package openblocks.integration.jei;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import openblocks.OpenBlocks;
import openblocks.client.gui.AutoAnvilScreen;
import openblocks.client.gui.AutoEnchantmentTableScreen;
import openblocks.client.gui.VacuumHopperScreen;
import openblocks.client.gui.XpBottlerScreen;

/**
 * Replaces the legacy NEI plugin ({@code NEIOpenBlocksConfig}), which hid internal items from the item list.
 */
@JeiPlugin
public class OpenBlocksJeiPlugin implements IModPlugin {

	private static final ResourceLocation UID = OpenBlocks.location("jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
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
	}

	private static List<ItemStack> hiddenItems() {
		// TODO: add canvas glass (wave 3) and height map (wave 4), as in the legacy NEI plugin
		return List.of();
	}
}
