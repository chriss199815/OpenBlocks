package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import openblocks.block.entity.GoldenEggBlockEntity;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code BlockGoldenEgg}: after a while it starts spinning, floats up while picking up blocks around it,
 * drops them again and hatches a Mini Me of the player who placed it. The egg is drawn by {@code GoldenEggRenderer}.
 */
public class GoldenEggBlock extends BaseEntityBlock {

	public static final MapCodec<GoldenEggBlock> CODEC = simpleCodec(GoldenEggBlock::new);

	private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

	public GoldenEggBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof GoldenEggBlockEntity egg)
			egg.setOwner(new ResolvableProfile(player.getGameProfile()));
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GoldenEggBlockEntity egg) egg.dropBlocks();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GoldenEggBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, OBBlockEntities.GOLDEN_EGG.get(), level.isClientSide? GoldenEggBlockEntity::clientTick : GoldenEggBlockEntity::serverTick);
	}
}
