package openblocks.util;

import java.util.Set;
import net.minecraft.core.Direction;

/**
 * Stores a set of sides as a bit mask (bit {@code side.get3DDataValue()}); replaces OpenModsLib {@code SyncableSides}.
 */
public final class SideBits {

	private SideBits() {}

	public static void read(Set<Direction> sides, byte bits) {
		sides.clear();
		for (Direction side : Direction.values())
			if ((bits & (1 << side.get3DDataValue())) != 0) sides.add(side);
	}

	public static byte write(Set<Direction> sides) {
		int bits = 0;
		for (Direction side : sides)
			bits |= 1 << side.get3DDataValue();
		return (byte)bits;
	}
}
