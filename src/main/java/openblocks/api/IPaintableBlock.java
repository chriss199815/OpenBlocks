package openblocks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Blocks that can be painted in any color with the paint brush, like the canvas.
 */
public interface IPaintableBlock {

	/**
	 * @param color 24 bit RGB
	 * @return whether the block changed
	 */
	boolean recolorBlock(Level level, BlockPos pos, Direction side, int color);
}
