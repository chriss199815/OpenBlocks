package openblocks.common;

import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Moves items and fluids between a machine and its neighbours; replaces OpenModsLib {@code ItemMover} and {@code SyncableTank.fillFromSides}.
 * Like legacy, items move one at a time and the sides are tried in random order.
 */
public final class MachineTransfer {

	private MachineTransfer() {}

	private static Direction[] shuffled(Level level, Set<Direction> sides) {
		return Util.shuffledCopy(sides.toArray(Direction[]::new), level.random).toArray(Direction[]::new);
	}

	/**
	 * Pushes one item of {@code slot} into the first neighbour that takes it.
	 */
	public static void pushItem(Level level, BlockPos pos, Set<Direction> sides, IItemHandler inventory, int slot) {
		final ItemStack stack = inventory.extractItem(slot, 1, true);
		if (stack.isEmpty()) return;
		for (Direction side : shuffled(level, sides)) {
			final IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (target != null && ItemHandlerHelper.insertItem(target, stack, false).isEmpty()) {
				inventory.extractItem(slot, 1, false);
				return;
			}
		}
	}

	/**
	 * Pulls one item that {@code slot} accepts from the first neighbour that has one.
	 */
	public static void pullItem(Level level, BlockPos pos, Set<Direction> sides, IItemHandler inventory, int slot) {
		for (Direction side : shuffled(level, sides)) {
			final IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (source == null) continue;
			for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
				final ItemStack stack = source.extractItem(sourceSlot, 1, true);
				if (stack.isEmpty() || !inventory.insertItem(slot, stack, true).isEmpty()) continue;
				inventory.insertItem(slot, source.extractItem(sourceSlot, 1, false), false);
				return;
			}
		}
	}

	/**
	 * Fills {@code tank} with up to {@code amount} from the first neighbour that has a fitting fluid.
	 */
	public static void pullFluid(Level level, BlockPos pos, Set<Direction> sides, IFluidHandler tank, int amount) {
		for (Direction side : sides) {
			final IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (source != null && !FluidUtil.tryFluidTransfer(tank, source, amount, true).isEmpty()) return;
		}
	}
}
