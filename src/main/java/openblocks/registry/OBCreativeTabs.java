package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;

public class OBCreativeTabs {

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenBlocks.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.openblocks"))
			// TODO: switch to the hang glider once it is ported
			.icon(() -> new ItemStack(Items.FEATHER))
			.displayItems((params, output) -> OBItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
			.build());
}
