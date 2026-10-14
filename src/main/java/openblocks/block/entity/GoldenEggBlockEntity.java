package openblocks.block.entity;

import com.mojang.authlib.properties.PropertyMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import openblocks.OBConfig;
import openblocks.common.OBFakePlayer;
import openblocks.entity.MiniMe;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code TileEntityGoldenEgg}. The server runs the stages; the client only smooths rotation and height
 * towards the values of the current stage. Picked up blocks are vanilla falling blocks without gravity instead of
 * OpenModsLib {@code EntityBlock}s, so they land as blocks (or drop as items) on their own.
 */
public class GoldenEggBlockEntity extends BlockEntity {

	private static final float SPEED_CHANGE_RATE = 0.1f;
	private static final int STAGE_CHANGE_TICK = 100;
	private static final int RISING_TIME = 400;
	private static final int FALLING_TIME = 10;
	public static final int MAX_HEIGHT = 5;
	private static final double STAGE_CHANGE_CHANCE = 0.8;
	private static final int PICK_RANGE = 10;
	// hatches when nobody placed the egg, like legacy
	private static final ResolvableProfile MR_GLITCH = new ResolvableProfile(Optional.empty(), Optional.of(UUID.fromString("d4d119aa-d410-488a-8734-0053577d4a1a")), new PropertyMap());

	private static final String TAG_STAGE = "Stage";
	private static final String TAG_TICK_COUNTER = "TickCounter";
	private static final String TAG_OWNER = "Owner";

	public enum Stage {
		INERT(0, 0, false),
		ROTATING_SLOW(1, 0, false),
		ROTATING_MEDIUM(10, 0, false),
		ROTATING_FAST(50, 0, false),
		FLOATING(100, 1.0f / RISING_TIME, true),
		FALLING(150, -1.0f / FALLING_TIME, true),
		EXPLODING(666, 0, true);

		public final float rotationSpeed;
		public final float progressSpeed;
		public final boolean specialEffects;

		Stage(float rotationSpeed, float progressSpeed, boolean specialEffects) {
			this.rotationSpeed = rotationSpeed;
			this.progressSpeed = progressSpeed;
			this.specialEffects = specialEffects;
		}
	}

	private Stage stage = Stage.INERT;
	private int tickCounter;

	@Nullable
	private ResolvableProfile owner;

	// not saved, like legacy; left over falling blocks drop as items after a while
	private final List<FallingBlockEntity> blocks = new ArrayList<>();

	// client only
	private float rotation;
	private float progress;
	private float rotationSpeed;
	private float progressSpeed;

	public GoldenEggBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.GOLDEN_EGG.get(), pos, state);
	}

	public Stage getStage() {
		return stage;
	}

	public void setOwner(ResolvableProfile owner) {
		this.owner = owner;
		setChanged();
	}

	public float getRotation(float partialTick) {
		return rotation + rotationSpeed * partialTick;
	}

	public float getProgress(float partialTick) {
		return progress + progressSpeed * partialTick;
	}

	public float getOffset(float partialTick) {
		return getProgress(partialTick) * MAX_HEIGHT;
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, GoldenEggBlockEntity egg) {
		egg.rotationSpeed = (1 - SPEED_CHANGE_RATE) * egg.rotationSpeed + SPEED_CHANGE_RATE * egg.stage.rotationSpeed;
		egg.rotation += egg.rotationSpeed;

		egg.progressSpeed = (1 - SPEED_CHANGE_RATE) * egg.progressSpeed + SPEED_CHANGE_RATE * egg.stage.progressSpeed;
		egg.progress += egg.progressSpeed;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, GoldenEggBlockEntity egg) {
		final Stage next = egg.tickStage((ServerLevel)level);
		if (next != null) egg.changeStage(next);
	}

	@Nullable
	private Stage tickStage(ServerLevel level) {
		final RandomSource random = level.random;
		return switch (stage) {
			case INERT -> tryRandomlyChangeStage(random, Stage.ROTATING_SLOW);
			case ROTATING_SLOW -> tryRandomlyChangeStage(random, Stage.ROTATING_MEDIUM);
			case ROTATING_MEDIUM -> tryRandomlyChangeStage(random, Stage.ROTATING_FAST);
			case ROTATING_FAST -> tryRandomlyChangeStage(random, Stage.FLOATING);
			case FLOATING -> {
				tickCounter--;
				if (OBConfig.EGG.canPickBlocks.getAsBoolean() && random.nextInt(6) == 0)
					pickUpBlock(level, worldPosition.offset(random.nextInt(2 * PICK_RANGE) - PICK_RANGE, random.nextInt(2) - 1, random.nextInt(2 * PICK_RANGE) - PICK_RANGE));
				yield tickCounter <= 0? Stage.FALLING : null;
			}
			case FALLING -> {
				tickCounter--;
				yield tickCounter <= 0? Stage.EXPLODING : null;
			}
			case EXPLODING -> null;
		};
	}

	@Nullable
	private Stage tryRandomlyChangeStage(RandomSource random, Stage next) {
		tickCounter++;
		return tickCounter % STAGE_CHANGE_TICK == 0 && random.nextDouble() < STAGE_CHANGE_CHANCE? next : null;
	}

	/**
	 * Switches to the given stage and runs its entry action; also used by GameTests to skip the random waiting.
	 */
	public void changeStage(Stage next) {
		stage = next;
		switch (next) {
			case FLOATING -> tickCounter = RISING_TIME;
			case FALLING -> {
				tickCounter = FALLING_TIME;
				dropBlocks();
			}
			case EXPLODING -> {
				hatch();
				return;
			}
			default -> {}
		}
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/**
	 * Lifts the block at the given position, if it can be moved. Replaces legacy {@code MagnetWhitelists.testBlock}:
	 * blocks with block entities, multi-block parts, fluids and unbreakable blocks stay, and other mods can veto through
	 * the break event.
	 */
	public boolean pickUpBlock(ServerLevel level, BlockPos pos) {
		final BlockState state = level.getBlockState(pos);
		if (!canPickUp(level, pos, state)) return false;
		if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, OBFakePlayer.get(level))).isCanceled()) return false;

		final FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
		block.setNoGravity(true);
		block.setDeltaMovement(0, 0.1, 0);
		blocks.add(block);
		return true;
	}

	private static boolean canPickUp(Level level, BlockPos pos, BlockState state) {
		return !state.isAir()
				&& !state.hasBlockEntity()
				&& state.getRenderShape() == RenderShape.MODEL
				&& state.getFluidState().isEmpty()
				&& state.getDestroySpeed(level, pos) >= 0
				&& !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
				&& !state.hasProperty(BlockStateProperties.BED_PART);
	}

	public void dropBlocks() {
		for (FallingBlockEntity block : blocks) {
			block.setNoGravity(false);
			block.setDeltaMovement(0, -0.9, 0);
		}
		blocks.clear();
	}

	private void hatch() {
		if (!(level instanceof ServerLevel serverLevel)) return;
		final Vec3 center = Vec3.atCenterOf(worldPosition);
		level.removeBlock(worldPosition, false);
		level.explode(null, center.x, center.y, center.z, 2, Level.ExplosionInteraction.BLOCK);

		final MiniMe miniMe = OBEntities.MINI_ME.get().create(serverLevel);
		if (miniMe == null) return;
		miniMe.moveTo(center.x, center.y, center.z, 0, 0);
		miniMe.setOwner(owner != null? owner : MR_GLITCH);
		serverLevel.addFreshEntity(miniMe);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString(TAG_STAGE, stage.name());
		tag.putInt(TAG_TICK_COUNTER, tickCounter);
		if (owner != null) tag.put(TAG_OWNER, ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, owner).getOrThrow());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		stage = readStage(tag);
		tickCounter = tag.getInt(TAG_TICK_COUNTER);
		owner = tag.contains(TAG_OWNER)? ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_OWNER)).result().orElse(null) : null;
	}

	private static Stage readStage(CompoundTag tag) {
		try {
			return Stage.valueOf(tag.getString(TAG_STAGE));
		} catch (IllegalArgumentException e) {
			return Stage.INERT;
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		tag.putString(TAG_STAGE, stage.name());
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
