package openblocks.entity;

import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import openblocks.common.HeightMapBuilder;
import openblocks.common.HeightMapData;
import openblocks.common.HeightMaps;
import openblocks.item.HeightMapItem;
import openblocks.registry.OBComponents;
import openblocks.registry.OBEntities;
import openblocks.registry.OBItems;
import org.joml.Vector3f;

/**
 * Port of legacy {@code EntityCartographer} (with {@code EntityAssistant}): floats in front of its owner's eyes. Sneak-use
 * it with an empty or height map to let it map the area around it, and with an empty hand to take the map back; while
 * mapping, it flutters around its owner. Hitting it turns it back into an item, keeping the map and progress.
 * Like {@link GoldenEye}, client and server move it the same way towards a synced target (legacy {@code EntitySmoothMove}).
 */
public class Cartographer extends Entity {

	/**
	 * Which chunks of the map are done, for the progress shown while looking at it.
	 */
	public record Progress(int chunksPerSide, BitSet finished) {
		public static final Progress NONE = new Progress(0, new BitSet());

		public static final StreamCodec<ByteBuf, Progress> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Progress::chunksPerSide,
				ByteBufCodecs.BYTE_ARRAY.map(BitSet::valueOf, BitSet::toByteArray), Progress::finished,
				Progress::new);
	}

	private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(Cartographer.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Boolean> DATA_MAPPING = SynchedEntityData.defineId(Cartographer.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Progress> DATA_PROGRESS = SynchedEntityData.defineId(Cartographer.class, OBEntities.CARTOGRAPHER_PROGRESS.get());

	private static final int MAP_JOB_DELAY = 5;
	private static final int MOVE_DELAY = 35;
	private static final double USE_DISTANCE = 3;
	private static final double DAMP = 0.5;
	private static final double MAX_STEP = 5;
	private static final double TELEPORT_DISTANCE = 128;
	private static final float EYE_SPEED = 0.1f;

	private static final String TAG_OWNER = "Owner";
	private static final String TAG_DATA = "Data";

	@Nullable
	private UUID ownerId;
	private ItemStack mapItem = ItemStack.EMPTY;
	private Optional<ResourceKey<Level>> mappingDimension = Optional.empty();
	private BitSet finishedChunks = new BitSet();

	private int countdownToAction = MAP_JOB_DELAY;
	private int countdownToMove = MOVE_DELAY;
	private float randomYaw;

	// client only: the wandering eye
	public float eyeYaw;
	public float eyePitch;
	public float prevEyeYaw;
	public float prevEyePitch;
	private float targetEyeYaw;
	private float targetEyePitch;

	public Cartographer(EntityType<? extends Cartographer> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public Cartographer(Level level, Player owner, ItemStack stack) {
		this(OBEntities.CARTOGRAPHER.get(), level);
		ownerId = owner.getUUID();
		setPos(owner.getX() + 1, owner.getEyeY(), owner.getZ());
		setTarget(position());
		readData(stack.getOrDefault(OBComponents.CARTOGRAPHER, CartographerData.EMPTY));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_TARGET, new Vector3f());
		builder.define(DATA_MAPPING, false);
		builder.define(DATA_PROGRESS, Progress.NONE);
	}

	private Vec3 getTarget() {
		return new Vec3(entityData.get(DATA_TARGET));
	}

	private void setTarget(Vec3 target) {
		entityData.set(DATA_TARGET, target.toVector3f());
	}

	public boolean isMapping() {
		return entityData.get(DATA_MAPPING);
	}

	public Progress getProgress() {
		return entityData.get(DATA_PROGRESS);
	}

	/**
	 * Legacy {@code getNewMapCenterX/Z}: a new map is centered on the chunk the cartographer is in.
	 */
	public int getNewMapCenterX() {
		return Mth.floor(getX()) & ~0xF;
	}

	public int getNewMapCenterZ() {
		return Mth.floor(getZ()) & ~0xF;
	}

	@Nullable
	private Player findOwner() {
		return ownerId != null? level().getPlayerByUUID(ownerId) : null;
	}

	@Override
	public void tick() {
		super.tick();

		if (!level().isClientSide) updateTarget();

		final Vec3 offset = getTarget().subtract(position());
		if (offset.length() > TELEPORT_DISTANCE) {
			setPos(getTarget());
		} else {
			Vec3 step = offset.scale(DAMP);
			if (step.length() > MAX_STEP) step = step.normalize().scale(MAX_STEP);
			setPos(position().add(step));
		}

		if (level() instanceof ServerLevel serverLevel) {
			if (isMapping() && mappingDimension.equals(Optional.of(level().dimension())) && countdownToAction-- <= 0) {
				countdownToAction = MAP_JOB_DELAY;
				mapNextChunk(serverLevel);
			}
		} else {
			updateEye();
		}
	}

	// in front of the owner's eyes, or around them while mapping
	private void updateTarget() {
		final Player owner = findOwner();
		if (owner == null || owner.level() != level()) return;

		final float yaw;
		if (isMapping()) {
			if (countdownToMove-- <= 0) {
				countdownToMove = MOVE_DELAY;
				randomYaw = random.nextFloat() * Mth.TWO_PI;
			}
			yaw = randomYaw;
		} else {
			yaw = owner.getYRot() * Mth.DEG_TO_RAD;
		}
		setTarget(new Vec3(owner.getX() - Mth.sin(yaw), owner.getEyeY(), owner.getZ() + Mth.cos(yaw)));
	}

	private void mapNextChunk(ServerLevel level) {
		final Integer mapId = HeightMapItem.getMapId(mapItem);
		if (mapId == null) return;
		final HeightMapData data = HeightMaps.get(level.getServer(), mapId);
		if (data == null) return;

		if (HeightMapBuilder.mapNextChunk(level, data, finishedChunks, getX(), getZ())) {
			HeightMaps.markUpdated(mapId, data);
			syncProgress(data);
			// only once: afterwards there is no chunk left to map
			if (HeightMapBuilder.isFinished(data, finishedChunks)) {
				playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT);
				final Player owner = findOwner();
				if (owner != null) owner.displayClientMessage(Component.translatable("openblocks.misc.cartographer.finished"), true);
			}
		}
	}

	private void syncProgress(@Nullable HeightMapData data) {
		entityData.set(DATA_PROGRESS, data != null? new Progress(HeightMapBuilder.chunksPerSide(data.getScale()), (BitSet)finishedChunks.clone()) : Progress.NONE);
	}

	private void updateEye() {
		prevEyeYaw = eyeYaw;
		prevEyePitch = eyePitch;
		final float diffYaw = targetEyeYaw - eyeYaw;
		final float diffPitch = targetEyePitch - eyePitch;
		if (Math.abs(diffYaw) + Math.abs(diffPitch) < 0.01f) {
			targetEyeYaw = eyeYaw + (random.nextFloat() - 0.5f) * Mth.TWO_PI;
			targetEyePitch = (random.nextFloat() - 0.5f) * Mth.PI;
		} else {
			eyeYaw += diffYaw * EYE_SPEED;
			eyePitch += diffPitch * EYE_SPEED;
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || distanceTo(player) >= USE_DISTANCE) return InteractionResult.PASS;
		if (level().isClientSide) return InteractionResult.SUCCESS;

		final ItemStack holding = player.getMainHandItem();
		if (holding.isEmpty() && !mapItem.isEmpty()) {
			final Progress progress = getProgress();
			player.displayClientMessage(Component.translatable("openblocks.misc.cartographer.stopped",
					progress.finished().cardinality(), progress.chunksPerSide() * progress.chunksPerSide()), true);
			player.setItemInHand(hand, mapItem);
			mapItem = ItemStack.EMPTY;
			finishedChunks = new BitSet();
			entityData.set(DATA_MAPPING, false);
			syncProgress(null);
		} else if (holding.is(OBItems.HEIGHT_MAP) || holding.is(OBItems.EMPTY_MAP)) {
			// unlike legacy, a new map replaces the old one, which goes back to the player
			final ItemStack oldMap = mapItem;
			mapItem = HeightMaps.upgradeToMap(level().getServer(), holding.split(1));
			if (holding.isEmpty()) player.setItemInHand(hand, oldMap);
			else player.getInventory().placeItemBackInInventory(oldMap);
			startMapping();
			playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT);
			player.displayClientMessage(Component.translatable("openblocks.misc.cartographer.started", getNewMapCenterX(), getNewMapCenterZ()), true);
		}
		return InteractionResult.SUCCESS;
	}

	private void startMapping() {
		final Integer mapId = HeightMapItem.getMapId(mapItem);
		if (mapId == null) return;
		final HeightMapData data = HeightMaps.get(level().getServer(), mapId);
		if (data == null) return;

		HeightMapBuilder.start((ServerLevel)level(), data, getNewMapCenterX(), getNewMapCenterZ());
		HeightMaps.markUpdated(mapId, data);
		mappingDimension = Optional.of(level().dimension());
		finishedChunks = new BitSet();
		entityData.set(DATA_MAPPING, true);
		syncProgress(data);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (isInvulnerableTo(source)) return false;
		if (!isRemoved() && !level().isClientSide) {
			spawnAtLocation(toItemStack(), 0.5f);
			discard();
		}
		return true;
	}

	private ItemStack toItemStack() {
		final ItemStack stack = new ItemStack(OBItems.CARTOGRAPHER.get());
		if (!mapItem.isEmpty()) stack.set(OBComponents.CARTOGRAPHER, writeData());
		return stack;
	}

	private CartographerData writeData() {
		return CartographerData.of(mapItem, isMapping(), mappingDimension, finishedChunks);
	}

	private void readData(CartographerData data) {
		mapItem = data.map().copy();
		mappingDimension = data.dimension();
		finishedChunks = data.finishedChunkBits();
		entityData.set(DATA_MAPPING, data.mapping() && !mapItem.isEmpty());
		if (!level().isClientSide) {
			final Integer mapId = HeightMapItem.getMapId(mapItem);
			syncProgress(mapId != null? HeightMaps.get(level().getServer(), mapId) : null);
		}
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(OBItems.CARTOGRAPHER.get());
	}

	/**
	 * Set by the client: whether the local player should see this cartographer outlined, see {@code CartographerRenderer}.
	 */
	public static Predicate<Cartographer> clientHighlight = cartographer -> false;

	@Override
	public boolean isCurrentlyGlowing() {
		return super.isCurrentlyGlowing() || level().isClientSide && clientHighlight.test(this);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (ownerId != null) tag.putUUID(TAG_OWNER, ownerId);
		CartographerData.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), writeData())
				.ifSuccess(data -> tag.put(TAG_DATA, data));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		ownerId = tag.hasUUID(TAG_OWNER)? tag.getUUID(TAG_OWNER) : null;
		if (tag.contains(TAG_DATA)) CartographerData.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get(TAG_DATA))
				.ifSuccess(this::readData);
		setTarget(position());
	}
}
