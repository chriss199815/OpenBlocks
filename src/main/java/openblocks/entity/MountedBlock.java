package openblocks.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import openblocks.OpenBlocks;
import openblocks.api.IMagnetAware;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code EntityMountedBlock} (and the parts of OpenModsLib {@code EntityBlock} it used): a block picked
 * up by the crane magnet, with its block entity data. As soon as it is no longer carried, it puts itself back as a
 * block, next to its position if that is taken, or drops as items.
 */
public class MountedBlock extends Entity implements IMagnetAware {

	/**
	 * Blocks with block entities the magnet may pick up; replaces the legacy tile entity whitelist (and its config).
	 */
	public static final TagKey<Block> PICKABLE = TagKey.create(Registries.BLOCK, OpenBlocks.location("magnet_pickable"));

	private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(MountedBlock.class, EntityDataSerializers.BLOCK_STATE);

	private static final Direction[] PLACE_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP };

	private static final String TAG_BLOCK_STATE = "BlockState";
	private static final String TAG_BLOCK_ENTITY = "TileEntity";

	@Nullable
	private CompoundTag blockEntityData;

	public MountedBlock(EntityType<? extends MountedBlock> type, Level level) {
		super(type, level);
	}

	private MountedBlock(Level level, BlockState state, @Nullable CompoundTag blockEntityData) {
		this(OBEntities.MOUNTED_BLOCK.get(), level);
		entityData.set(DATA_BLOCK_STATE, state);
		this.blockEntityData = blockEntityData;
	}

	/**
	 * Replaces legacy {@code MagnetWhitelists.testBlock}: no fluids or unbreakable blocks, no halves of doors or beds;
	 * blocks with block entities only from the tag.
	 */
	public static boolean canPickUp(Level level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, pos) < 0) return false;
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) return false;
		if (state.is(PICKABLE)) return true;
		return !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL;
	}

	/**
	 * Takes the block out of the world, if the player may break it there (other mods can veto through the break event).
	 */
	@Nullable
	public static MountedBlock pickUp(ServerLevel level, BlockPos pos, Player player) {
		final BlockState state = level.getBlockState(pos);
		if (!canPickUp(level, pos, state) || !player.mayBuild() || !level.mayInteract(player, pos)) return null;
		if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return null;

		final BlockEntity blockEntity = level.getBlockEntity(pos);
		CompoundTag blockEntityData = null;
		if (blockEntity != null) {
			blockEntityData = blockEntity.saveWithoutMetadata(level.registryAccess());
			// removed first, so containers do not spill their contents
			level.removeBlockEntity(pos);
		}
		// a waterlogged block leaves its water
		level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);

		final MountedBlock result = new MountedBlock(level, state, blockEntityData);
		result.setPos(Vec3.atBottomCenterOf(pos));
		level.addFreshEntity(result);
		return result;
	}

	public BlockState getBlockState() {
		return entityData.get(DATA_BLOCK_STATE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_BLOCK_STATE, Blocks.STONE.defaultBlockState());
	}

	private BlockPos getDropPos() {
		return BlockPos.containing(getBoundingBox().getCenter());
	}

	@Override
	public boolean canRelease() {
		return level().getBlockState(getDropPos()).canBeReplaced();
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel serverLevel && !isPassenger()) {
			if (!tryPlace(serverLevel)) dropAsItems(serverLevel);
			discard();
		}
	}

	private boolean tryPlace(ServerLevel level) {
		final BlockPos pos = getDropPos();
		if (tryPlace(level, pos)) return true;
		for (Direction direction : PLACE_DIRECTIONS)
			if (tryPlace(level, pos.relative(direction))) return true;
		return false;
	}

	private boolean tryPlace(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).canBeReplaced()) return false;

		BlockState state = getBlockState();
		if (state.hasProperty(BlockStateProperties.WATERLOGGED))
			state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
		if (!state.canSurvive(level, pos)) return false;
		// fences, stairs and chests connect to their new neighbours
		state = Block.updateFromNeighbourShapes(state, level, pos);
		if (state.isAir() || !level.setBlock(pos, state, Block.UPDATE_ALL)) return false;

		if (blockEntityData != null) {
			final BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity != null) {
				blockEntity.loadWithComponents(blockEntityData, level.registryAccess());
				blockEntity.setChanged();
			}
		}
		return true;
	}

	private void dropAsItems(ServerLevel level) {
		final BlockState state = getBlockState();
		final BlockPos pos = getDropPos();
		final BlockEntity blockEntity = blockEntityData != null? BlockEntity.loadStatic(pos, state, blockEntityData, level.registryAccess()) : null;
		for (var stack : Block.getDrops(state, level, pos, blockEntity))
			spawnAtLocation(stack);
		// shulker boxes keep their contents in the dropped item
		if (blockEntity instanceof Container container && !(state.getBlock() instanceof ShulkerBoxBlock))
			Containers.dropContents(level, pos, container);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	/**
	 * Like legacy, hitting it breaks it.
	 */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (isInvulnerableTo(source)) return false;
		if (level() instanceof ServerLevel serverLevel && !isRemoved()) {
			dropAsItems(serverLevel);
			discard();
		}
		return true;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.put(TAG_BLOCK_STATE, NbtUtils.writeBlockState(getBlockState()));
		if (blockEntityData != null) tag.put(TAG_BLOCK_ENTITY, blockEntityData.copy());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		entityData.set(DATA_BLOCK_STATE, NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound(TAG_BLOCK_STATE)));
		blockEntityData = tag.contains(TAG_BLOCK_ENTITY)? tag.getCompound(TAG_BLOCK_ENTITY) : null;
	}
}
