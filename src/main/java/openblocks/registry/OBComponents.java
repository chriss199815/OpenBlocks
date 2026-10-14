package openblocks.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.item.Paint;

public class OBComponents {

	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, OpenBlocks.MODID);

	/**
	 * Fluid stored in a tank item; replaces the legacy {@code tank} NBT tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_CONTENTS = COMPONENTS.registerComponentType("tank_contents",
			builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

	/**
	 * Color and uses left of a paint can or brush; replaces the legacy {@code color}/{@code amount} NBT tags.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Paint>> PAINT = COMPONENTS.registerComponentType("paint",
			builder -> builder.persistent(Paint.CODEC).networkSynchronized(Paint.STREAM_CODEC));
}
