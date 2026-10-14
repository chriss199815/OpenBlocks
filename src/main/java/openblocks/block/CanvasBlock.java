package openblocks.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import openblocks.OBConfig;
import openblocks.api.IPaintableBlock;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.registry.OBBlocks;

/**
 * Port of legacy {@code BlockCanvas} and {@code BlockCanvasGlass}: a block that can be painted in any color, either placed as is,
 * or replacing another block (by the paint brush or a stencil), which it then looks and behaves like.
 * Blocks that do not fill their space become the see-through canvas glass, like legacy.
 */
public class CanvasBlock extends BaseEntityBlock implements IPaintableBlock {

	public static final MapCodec<CanvasBlock> CODEC = simpleCodec(CanvasBlock::new);

	public CanvasBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	/**
	 * Replaces the block at the position with a canvas showing it; true if there is a canvas there now.
	 */
	public static boolean replaceBlock(Level level, BlockPos pos) {
		final BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof CanvasBlock) return true;
		if (!canReplace(state)) return false;

		final Block canvas = state.isSolidRender(level, pos)? OBBlocks.CANVAS.get() : OBBlocks.CANVAS_GLASS.get();
		level.setBlock(pos, canvas.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof CanvasBlockEntity canvasEntity) canvasEntity.setPaintedBlock(state);
		// neighbours were updated before the canvas knew the painted block, e.g. its redstone signal
		level.updateNeighborsAt(pos, canvas);
		return true;
	}

	/**
	 * Port of legacy {@code CanvasReplaceBlacklist}: plain blocks only, not in the config blacklist.
	 * Besides the legacy checks (no block entity, no doors or beds), unbreakable and waterlogged blocks are kept.
	 */
	public static boolean canReplace(BlockState state) {
		if (state.isAir() || state.hasBlockEntity() || state.getRenderShape() != RenderShape.MODEL) return false;
		// two-part blocks do not work with canvas
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) return false;
		if (!state.getFluidState().isEmpty()) return false;
		if (state.getBlock().defaultDestroyTime() < 0) return false;
		return !OBConfig.CANVAS.replaceBlacklist.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
	}

	@Override
	public boolean recolorBlock(Level level, BlockPos pos, Direction side, int color) {
		return level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas && canvas.applyPaint(side, color);
	}

	/**
	 * Restores the painted block once all paint is wiped off, dropping the stencils that are left.
	 */
	public static void restoreIfUnpainted(Level level, BlockPos pos, CanvasBlockEntity canvas) {
		final BlockState painted = canvas.getPaintedBlock();
		if (painted == null || !canvas.isUnpainted()) return;
		for (ItemStack stencil : canvas.getStencils())
			popResource(level, pos, stencil);
		level.setBlock(pos, painted, Block.UPDATE_ALL);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas)) return InteractionResult.PASS;
		final Direction side = hitResult.getDirection();
		if (canvas.getSide(side).cover().isEmpty()) return InteractionResult.PASS;

		// like legacy: sneaking takes the stencil off, otherwise it is turned
		if (!level.isClientSide) {
			if (player.isSecondaryUseActive()) popResourceFromFace(level, pos, side, canvas.removeStencil(side));
			else canvas.rotateStencil(side);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CanvasBlockEntity(pos, state);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CanvasBlockEntity canvas)) return super.getDrops(state, params);
		final BlockState painted = canvas.getPaintedBlock();
		final List<ItemStack> drops = new ArrayList<>(painted != null? painted.getDrops(params) : super.getDrops(state, params));
		drops.addAll(canvas.getStencils());
		return drops;
	}

	// the painted block decides how this one behaves, like legacy

	@Nullable
	private static BlockState painted(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas? canvas.getPaintedBlock() : null;
	}

	private static <T> T fromPainted(BlockGetter level, BlockPos pos, Function<BlockState, T> getter, T fallback) {
		final BlockState painted = painted(level, pos);
		return painted != null? getter.apply(painted) : fallback;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return fromPainted(level, pos, painted -> painted.getShape(level, pos, context), Shapes.block());
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return fromPainted(level, pos, painted -> painted.getCollisionShape(level, pos, context), Shapes.block());
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return fromPainted(level, pos, painted -> painted.getDestroyProgress(player, level, pos), super.getDestroyProgress(state, player, level, pos));
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
		return fromPainted(level, pos, painted -> painted.canHarvestBlock(level, pos, player), super.canHarvestBlock(state, level, pos, player));
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return fromPainted(level, pos, painted -> painted.getExplosionResistance(level, pos, explosion), super.getExplosionResistance(state, level, pos, explosion));
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		return fromPainted(level, pos, painted -> painted.getLightEmission(level, pos), super.getLightEmission(state, level, pos));
	}

	@Override
	public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
		return fromPainted(level, pos, painted -> painted.getSoundType(level, pos, entity), super.getSoundType(state, level, pos, entity));
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		return fromPainted(level, pos, painted -> painted.getCloneItemStack(target, level, pos, player), super.getCloneItemStack(state, target, level, pos, player));
	}

	/**
	 * For {@code Properties.isRedstoneConductor}: only conducts if the painted block does.
	 */
	public static boolean isRedstoneConductor(BlockState state, BlockGetter level, BlockPos pos) {
		return fromPainted(level, pos, painted -> painted.isRedstoneConductor(level, pos), true);
	}

	// redstone, like legacy: every canvas may be a signal source, the painted block decides the actual signal

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
		return fromPainted(level, pos, painted -> painted.canRedstoneConnectTo(level, pos, direction), false);
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return fromPainted(level, pos, painted -> painted.getSignal(level, pos, direction), 0);
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return fromPainted(level, pos, painted -> painted.getDirectSignal(level, pos, direction), 0);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return fromPainted(level, pos, painted -> painted.hasAnalogOutputSignal()? painted.getAnalogOutputSignal(level, pos) : 0, 0);
	}

	@Override
	public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
		return fromPainted(level, pos, painted -> painted, state);
	}
}
