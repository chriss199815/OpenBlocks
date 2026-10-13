package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import openblocks.block.entity.TankBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBComponents;

public class TankBlock extends BaseEntityBlock {

	public static final MapCodec<TankBlock> CODEC = simpleCodec(TankBlock::new);

	/**
	 * Light of the contained fluid, kept in the state so the light engine doesn't need the block entity.
	 */
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

	public TankBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIGHT);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TankBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide? null : createTickerHelper(type, OBBlockEntities.TANK.get(), TankBlockEntity::serverTick);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		// handles both directions: buckets and tank items are emptied into or filled from the tank
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection()))
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof TankBlockEntity tank)) return InteractionResult.PASS;

		if (player.isShiftKeyDown()) {
			if (!level.isClientSide) player.displayClientMessage(describeContents(tank), true);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		// the client knows the synced fluid, so it can predict whether the hand should swing
		if (level.isClientSide) return FluidXpUtils.isXpFluid(tank.getFluid())? InteractionResult.SUCCESS : InteractionResult.PASS;
		return tank.tryDrainXp(player)? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	private static Component describeContents(TankBlockEntity tank) {
		final FluidStack fluid = tank.getFluid();
		if (fluid.isEmpty()) return Component.translatable("openblocks.misc.tank.empty", tank.getCapacity());
		final String percent = String.format("%.1f", 100 * tank.getFluidRatio());
		return Component.translatable("openblocks.misc.tank.contents", fluid.getHoverName(), fluid.getAmount(), tank.getCapacity(), percent);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) tank.onNeighborChanged();
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof TankBlockEntity tank)) return 0;
		final double value = tank.getFluidRatio() * 15;
		if (value == 0) return 0;
		return Math.max((int)value, 1);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		// like legacy, picking a filled tank gives a full one
		if (level.getBlockEntity(pos) instanceof TankBlockEntity tank && !tank.getFluid().isEmpty()) return filledTank(this, tank.getFluid());
		return new ItemStack(this);
	}

	public static ItemStack filledTank(Block tank, FluidStack fluid) {
		final ItemStack result = new ItemStack(tank);
		result.set(OBComponents.TANK_CONTENTS, SimpleFluidContent.copyOf(fluid.copyWithAmount(TankBlockEntity.getTankCapacity())));
		return result;
	}
}
