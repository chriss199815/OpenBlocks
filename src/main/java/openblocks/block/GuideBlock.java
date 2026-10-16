package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import openblocks.block.entity.GuideBlockEntity;
import openblocks.common.GuideButtons;
import openblocks.common.GuideOrientation;

/**
 * Port of legacy {@code BlockGuide}: shows the outline of a shape around itself (drawn by {@code GuideRenderer}) while
 * enabled by redstone. Touch buttons on its faces change size, shape and orientation, dye changes the marker color.
 * Legacy found the button on the client and sent it in a {@code GuideActionEvent}; here the server finds it from the
 * hit position of the click.
 */
public class GuideBlock extends BaseEntityBlock {

	public static final MapCodec<GuideBlock> CODEC = simpleCodec(GuideBlock::new);

	public static final EnumProperty<GuideOrientation> ORIENTATION = EnumProperty.create("orientation", GuideOrientation.class);

	public GuideBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ORIENTATION, GuideOrientation.XP_YP));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ORIENTATION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(ORIENTATION, GuideOrientation.forClickedFace(context.getClickedFace()));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		final GuideOrientation orientation = state.getValue(ORIENTATION);
		return state.setValue(ORIENTATION, GuideOrientation.lookup(rotation.rotate(orientation.x), rotation.rotate(orientation.y)));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/**
	 * Whether the touch buttons react to the player; the builder guide turns them off while a block is held.
	 */
	public boolean areButtonsActive(Player player) {
		return true;
	}

	/**
	 * Touch button under a hit, or {@code null} if there is none or the buttons are off for the player.
	 */
	@Nullable
	public GuideButtons.Button findButton(BlockState state, BlockPos pos, Vec3 hitLocation, Player player) {
		if (!areButtonsActive(player)) return null;
		final Vec3 local = state.getValue(ORIENTATION).blockToLocal(hitLocation.subtract(Vec3.atLowerCornerOf(pos)));
		return GuideButtons.find(local);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof GuideBlockEntity guide) || !guide.canUseItem(stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		if (player instanceof ServerPlayer serverPlayer) guide.useItem(serverPlayer, stack, hitResult);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/**
	 * A touch button reacts to the click; anywhere else, or while sneaking, the settings GUI opens (new, legacy only had
	 * the touch buttons). Like legacy, clicks with the main hand are always taken, so blocks are not placed against the
	 * guide by accident.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GuideBlockEntity guide) {
			final GuideButtons.Button button = player.isSecondaryUseActive()? null : findButton(state, pos, hitResult.getLocation(), player);
			if (button != null) guide.press(serverPlayer, button);
			else if (areButtonsActive(player)) serverPlayer.openMenu(guide, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.getBlockEntity(pos) instanceof GuideBlockEntity guide) guide.updateRedstone();
	}

	// the block entity does not exist yet in onPlace
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!state.is(oldState.getBlock())) level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof GuideBlockEntity guide) guide.updateRedstone();
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		final double x = pos.getX() + 0.5;
		final double y = pos.getY() + 0.7;
		final double z = pos.getZ() + 0.5;
		level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
		level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GuideBlockEntity(pos, state);
	}

	/**
	 * Port of legacy {@code BlockBuilderGuide}: also places the held block into the shape. Its touch buttons are off
	 * while a block is held.
	 */
	public static class Builder extends GuideBlock {

		public static final MapCodec<Builder> CODEC = simpleCodec(Builder::new);

		public Builder(Properties properties) {
			super(properties);
		}

		@Override
		protected MapCodec<? extends BaseEntityBlock> codec() {
			return CODEC;
		}

		@Override
		public boolean areButtonsActive(Player player) {
			return !(player.getMainHandItem().getItem() instanceof BlockItem);
		}

		// legacy added a flame tinted cyan, soul fire is the vanilla look-alike
		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			final double x = pos.getX() + 0.5;
			final double y = pos.getY() + 0.7;
			final double z = pos.getZ() + 0.5;
			level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0, 0);
		}

		@Nullable
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new GuideBlockEntity.Builder(pos, state);
		}
	}
}
