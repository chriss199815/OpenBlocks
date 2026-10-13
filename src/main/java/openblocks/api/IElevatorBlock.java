package openblocks.api;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface IElevatorBlock {

	DyeColor getColor(BlockGetter level, BlockPos pos, BlockState state);

	/**
	 * @return direction the player should face after arriving at this elevator, {@code null} to keep the current one
	 */
	@Nullable
	Direction getRotation(BlockGetter level, BlockPos pos, BlockState state);
}
