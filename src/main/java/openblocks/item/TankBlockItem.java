package openblocks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import openblocks.block.entity.TankBlockEntity;
import openblocks.registry.OBComponents;

/**
 * Tank item; contents are kept in {@link OBComponents#TANK_CONTENTS} and copied to/from the block entity by vanilla.
 */
public class TankBlockItem extends BlockItem {

	public TankBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static FluidStack getContents(ItemStack stack) {
		return stack.getOrDefault(OBComponents.TANK_CONTENTS, SimpleFluidContent.EMPTY).copy();
	}

	@Override
	public Component getName(ItemStack stack) {
		final FluidStack fluid = getContents(stack);
		if (fluid.isEmpty()) return super.getName(stack);
		return Component.translatable(getDescriptionId() + ".filled", fluid.getHoverName());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		final FluidStack fluid = getContents(stack);
		if (fluid.isEmpty()) return;

		final float percent = Math.max(100.0f * fluid.getAmount() / TankBlockEntity.getTankCapacity(), 1);
		tooltip.add(Component.literal(String.format("%d mB (%.0f%%)", fluid.getAmount(), percent)));
		if (flag.isAdvanced())
			tooltip.add(Component.literal(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()).withStyle(ChatFormatting.DARK_GRAY));
	}
}
