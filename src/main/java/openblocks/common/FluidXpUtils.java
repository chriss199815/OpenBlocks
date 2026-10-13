package openblocks.common;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import openblocks.OBConfig;
import openblocks.registry.OBFluids;

/**
 * Conversion between XP points and liquid XP.
 * Any fluid in {@code c:experience} is accepted, which replaces the legacy per-fluid converter list ({@code additionalXpFluids}).
 */
public class FluidXpUtils {

	public static boolean isXpFluid(FluidStack stack) {
		return !stack.isEmpty() && stack.is(Tags.Fluids.EXPERIENCE);
	}

	public static int fluidToXp(int fluid) {
		return fluid / OBConfig.XP.liquidRatio.getAsInt();
	}

	public static int xpToFluid(int xp) {
		return xp * OBConfig.XP.liquidRatio.getAsInt();
	}

	public static FluidStack xpJuice(int xp) {
		return new FluidStack(OBFluids.XP_JUICE.get(), xpToFluid(xp));
	}
}
