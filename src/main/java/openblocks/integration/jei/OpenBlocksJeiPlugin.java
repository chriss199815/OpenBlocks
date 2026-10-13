package openblocks.integration.jei;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import openblocks.OpenBlocks;

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
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		List<ItemStack> hidden = hiddenItems();
		if (!hidden.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
	}

	private static List<ItemStack> hiddenItems() {
		// TODO: add canvas glass (wave 3) and height map (wave 4), as in the legacy NEI plugin
		return List.of();
	}
}
