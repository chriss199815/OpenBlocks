package openblocks.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import openblocks.api.IElevatorBlock;
import openblocks.registry.OBBlocks;

/**
 * One block per color (like vanilla wool). Clicking with a dye swaps the block for the elevator of that color.
 */
public class ElevatorBlock extends Block implements IElevatorBlock {

	private final DyeColor color;

	public ElevatorBlock(DyeColor color, Properties properties) {
		super(properties);
		this.color = color;
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	public String getDescriptionId() {
		// all colors share one name, as in the legacy mod
		return "block.openblocks.elevator";
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		final DyeColor dye = DyeColor.getColor(stack);
		if (dye == null || dye == color) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		if (!level.isClientSide) level.setBlockAndUpdate(pos, recolor(state, dye));
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	protected BlockState recolor(BlockState state, DyeColor dye) {
		return OBBlocks.ELEVATORS.get(dye).get().defaultBlockState();
	}

	@Override
	public DyeColor getColor(BlockGetter level, BlockPos pos, BlockState state) {
		return color;
	}

	@Nullable
	@Override
	public Direction getRotation(BlockGetter level, BlockPos pos, BlockState state) {
		return null;
	}
}
