package openblocks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import openblocks.block.entity.SkyBlockEntity;

/**
 * Port of legacy {@code BlockSky}: shows the sky through it while powered (the inverted one while unpowered). The
 * inverted variant is its own block instead of metadata. While active, the block entity renderer draws the sky. Unlike
 * legacy, an active block can still be targeted (legacy had no outline, so it could not be broken and clicks went through).
 */
public class SkyBlock extends BaseEntityBlock {

	public static final MapCodec<SkyBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.BOOL.fieldOf("inverted").forGetter(SkyBlock::isInverted),
			propertiesCodec()).apply(instance, SkyBlock::new));

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private final boolean inverted;

	public SkyBlock(boolean inverted, Properties properties) {
		super(properties);
		this.inverted = inverted;
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	public boolean isInverted() {
		return inverted;
	}

	public static boolean isActive(BlockState state) {
		return state.getBlock() instanceof SkyBlock sky && state.getValue(POWERED) != sky.inverted;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		// placed without a context (e.g. by commands)
		if (!oldState.is(this)) updatePowerState(state, level, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
		updatePowerState(state, level, pos);
	}

	private void updatePowerState(BlockState state, Level level, BlockPos pos) {
		if (!level.isClientSide && state.getValue(POWERED) != level.hasNeighborSignal(pos))
			level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		final boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return isActive(state)? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SkyBlockEntity(pos, state);
	}
}
