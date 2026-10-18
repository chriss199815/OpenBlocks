package openblocks.entity;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.api.IMagnetAware;
import openblocks.common.CraneState;
import openblocks.item.CraneBackpackItem;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code EntityMagnet.PlayerBound}: hangs from the arm of the crane backpack, two blocks in front of its
 * owner and as far down as the crane control lowered it. Picks up the entity below it or, if there is none, the block
 * below it; the carried entity hangs inside its (growing) bounding box, so it does not move into walls. The server
 * moves it (legacy {@code EntitySmoothMove}), clients only follow.
 */
public class Magnet extends Entity {

	/**
	 * Entities besides living ones the magnet may pick up; replaces the legacy entity whitelist (and its config).
	 */
	public static final TagKey<EntityType<?>> PICKABLE = TagKey.create(Registries.ENTITY_TYPE, OpenBlocks.location("magnet_pickable"));

	private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(Magnet.class, EntityDataSerializers.INT);

	public static final float SIZE = 0.5f;
	private static final double DAMP = 0.5;
	private static final double MAX_STEP = 5;
	private static final double JUMP_DISTANCE = 128;
	private static final double MIN_DISTANCE = 0.01;

	private static final String TAG_OWNER = "Owner";

	@Nullable
	private UUID ownerId;
	private boolean aboveTarget;

	public Magnet(EntityType<? extends Magnet> type, Level level) {
		super(type, level);
	}

	public Magnet(Player owner) {
		this(OBEntities.MAGNET.get(), owner.level());
		ownerId = owner.getUUID();
		entityData.set(DATA_OWNER, owner.getId());
		setPos(getTarget(owner));
	}

	/**
	 * Called by the crane backpack every tick on the server.
	 */
	public static void ensureExists(Player owner) {
		final CraneState state = CraneState.of(owner);
		if (state.getMagnet() != null) return;
		final Magnet magnet = new Magnet(owner);
		state.setMagnet(magnet);
		owner.level().addFreshEntity(magnet);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_OWNER, -1);
	}

	@Nullable
	private Player findOwner() {
		if (level().isClientSide) return level().getEntity(entityData.get(DATA_OWNER)) instanceof Player player? player : null;
		return ownerId != null? level().getPlayerByUUID(ownerId) : null;
	}

	/**
	 * Below the end of the arm, with the top of the magnet at the end of the line.
	 */
	private Vec3 getTarget(Player owner) {
		final double yaw = Math.toRadians(owner.getYRot());
		return new Vec3(owner.getX() - CraneState.ARM_RADIUS * Math.sin(yaw),
				owner.getY() + owner.getBbHeight() - CraneState.of(owner).length - getBbHeight(),
				owner.getZ() + CraneState.ARM_RADIUS * Math.cos(yaw));
	}

	private boolean isValidFor(Player owner) {
		if (!owner.isAlive() || owner.level() != level() || !CraneBackpackItem.isWearing(owner)) return false;
		final Magnet current = CraneState.of(owner).getMagnet();
		return current == null || current == this;
	}

	@Override
	public void tick() {
		super.tick();
		final Player owner = findOwner();

		if (!level().isClientSide) {
			if (owner == null || !isValidFor(owner)) {
				ejectPassengers();
				discard();
				return;
			}
			entityData.set(DATA_OWNER, owner.getId());
			moveTowards(getTarget(owner));
		}

		// also binds a magnet loaded with the world, and the magnet on clients
		if (owner != null) CraneState.of(owner).setMagnet(this);
		aboveTarget = !findTargets(owner).isEmpty();
	}

	private void moveTowards(Vec3 target) {
		Vec3 delta = target.subtract(position());
		final double distanceSq = delta.lengthSqr();
		if (distanceSq > JUMP_DISTANCE * JUMP_DISTANCE || distanceSq < MIN_DISTANCE * MIN_DISTANCE) {
			setPos(target);
		} else {
			if (distanceSq > MAX_STEP * MAX_STEP) delta = delta.scale(MAX_STEP / Math.sqrt(distanceSq));
			move(MoverType.SELF, delta.scale(DAMP));
		}
	}

	private List<Entity> findTargets(@Nullable Player owner) {
		final AABB area = getBoundingBox().inflate(0.25, 0, 0.25).move(0, -1, 0);
		return level().getEntities(this, area, entity -> entity != owner && !hasPassenger(entity) && EntitySelector.NO_SPECTATORS.test(entity) && canPickUp(entity));
	}

	private static boolean canPickUp(Entity entity) {
		return entity instanceof LivingEntity || entity.getType().is(PICKABLE);
	}

	/**
	 * Releases the carried entity or picks up a new one: the first entity below, otherwise the block below.
	 */
	public boolean toggle(Player owner) {
		final Entity passenger = getFirstPassenger();
		if (passenger != null) {
			if (passenger instanceof IMagnetAware aware && !aware.canRelease()) return false;
			passenger.stopRiding();
			return true;
		}

		if (!(level() instanceof ServerLevel level)) return false;

		Entity target = null;
		if (OBConfig.CRANE.canPickEntities.getAsBoolean()) target = findTargets(owner).stream().findFirst().orElse(null);
		if (target == null && OBConfig.CRANE.canPickBlocks.getAsBoolean())
			target = MountedBlock.pickUp(level, BlockPos.containing(getX(), getY() - 0.5, getZ()), owner);

		return target != null && target.startRiding(this, true);
	}

	public boolean isAboveTarget() {
		return aboveTarget && OBConfig.CRANE.canPickEntities.getAsBoolean();
	}

	public boolean isLocked() {
		return isVehicle();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		final Entity passenger = getFirstPassenger();
		if (passenger == null) return super.getDimensions(pose);
		return EntityDimensions.scalable(Math.max(SIZE, passenger.getBbWidth()), SIZE + passenger.getBbHeight());
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		refreshDimensions();
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		refreshDimensions();
	}

	// the passenger hangs at the bottom of the magnet's bounding box
	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		callback.accept(passenger, getX(), getY(), getZ());
	}

	// like legacy, released entities stay where they hung (instead of being put on top)
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return passenger.position();
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public boolean dismountsUnderwater() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (ownerId != null) tag.putUUID(TAG_OWNER, ownerId);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		ownerId = tag.hasUUID(TAG_OWNER)? tag.getUUID(TAG_OWNER) : null;
	}
}
