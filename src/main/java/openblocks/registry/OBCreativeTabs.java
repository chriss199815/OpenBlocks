package openblocks.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.block.TankBlock;

public class OBCreativeTabs {

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenBlocks.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.openblocks"))
			// TODO: switch to the hang glider once it is ported
			.icon(() -> new ItemStack(Items.FEATHER))
			.displayItems((params, output) -> {
				OBItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));

				if (OBConfig.TANKS.displayAllFilledTanks.getAsBoolean()) {
					for (Fluid fluid : BuiltInRegistries.FLUID)
						if (fluid.isSource(fluid.defaultFluidState()))
							output.accept(TankBlock.filledTank(OBBlocks.TANK.get(), new FluidStack(fluid, 1)), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
				}
			})
			.build());
}
