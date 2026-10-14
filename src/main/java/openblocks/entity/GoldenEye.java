package openblocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import openblocks.registry.OBEntities;
import openblocks.registry.OBItems;
import org.joml.Vector3f;

/**
 * Port of legacy {@code EntityGoldenEye}: floats from the player six blocks towards the structure (or down, when the
 * structure is closer than 16 blocks), stays there for three seconds and then drops the thrown golden eye. The point it
 * flies to is synced, so client and server move it the same way (legacy {@code EntitySmoothMove}).
 */
public class GoldenEye extends Entity implements ItemSupplier {

	private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(GoldenEye.class, EntityDataSerializers.VECTOR3);

	private static final int TTL = 60;
	private static final double DAMP = 0.25;
	private static final double MAX_STEP = 1.0;
	private static final int MAX_DROP_RISE = 8;

	private static final String TAG_SPAWNING_ITEM = "SpawningItem";
	private static final String TAG_TARGET_X = "TargetX";
	private static final String TAG_TARGET_Y = "TargetY";
	private static final String TAG_TARGET_Z = "TargetZ";

	private int timeToLive = TTL;

	// the eye given back when it lands; empty in creative mode
	private ItemStack spawningStack = ItemStack.EMPTY;

	public GoldenEye(EntityType<? extends GoldenEye> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public GoldenEye(Level level, Player owner, BlockPos structure, ItemStack spawningStack) {
		this(OBEntities.GOLDEN_EYE.get(), level);
		this.spawningStack = spawningStack;

		final double eyeY = owner.getEyeY();
		final double yaw = Math.toRadians(owner.getYRot());
		setPos(owner.getX() - 0.75 * Math.sin(yaw), eyeY, owner.getZ() + 0.75 * Math.cos(yaw));

		double dx = structure.getX() - owner.getX();
		double dz = structure.getZ() - owner.getZ();
		final double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < 16) {
			setTarget(new Vec3(owner.getX(), eyeY - 4, owner.getZ()));
			setXRot(90);
		} else {
			dx /= distance;
			dz /= distance;
			setTarget(new Vec3(owner.getX() + 6 * dx, eyeY, owner.getZ() + 6 * dz));
			setYRot((float)Math.toDegrees(Math.atan2(dx, dz)));
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_TARGET, new Vector3f());
	}

	public Vec3 getTarget() {
		return new Vec3(entityData.get(DATA_TARGET));
	}

	private void setTarget(Vec3 target) {
		entityData.set(DATA_TARGET, target.toVector3f());
	}

	public ItemStack getSpawningStack() {
		return spawningStack;
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(OBItems.GOLDEN_EYE.get());
	}

	@Override
	public void tick() {
		super.tick();

		if (!level().isClientSide && timeToLive-- < 0) {
			if (!spawningStack.isEmpty()) {
				final Vec3 drop = findDropPos();
				level().addFreshEntity(new ItemEntity(level(), drop.x, drop.y, drop.z, spawningStack));
			}
			discard();
			return;
		}

		Vec3 step = getTarget().subtract(position()).scale(DAMP);
		if (step.length() > MAX_STEP) step = step.normalize().scale(MAX_STEP);
		setPos(position().add(step));

		if (level().isClientSide)
			level().addParticle(ParticleTypes.PORTAL,
					getX() + random.nextGaussian() * 0.3 - 0.15, getY() - 0.5 + random.nextGaussian() * 0.3 - 0.15, getZ() - random.nextGaussian() * 0.3 - 0.15,
					step.x, step.y, step.z);
	}

	/**
	 * Next to a structure the eye flies four blocks below the player's eyes, which is usually in the ground; unlike legacy,
	 * the eye then drops on top of it instead of inside, where it could get lost.
	 */
	private Vec3 findDropPos() {
		BlockPos pos = blockPosition();
		for (int i = 0; i < MAX_DROP_RISE && !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty(); i++)
			pos = pos.above();
		return pos.equals(blockPosition())? position() : new Vec3(getX(), pos.getY(), getZ());
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < Mth.square(64 * getViewScale());
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (!spawningStack.isEmpty()) tag.put(TAG_SPAWNING_ITEM, spawningStack.save(registryAccess()));
		final Vec3 target = getTarget();
		tag.putDouble(TAG_TARGET_X, target.x);
		tag.putDouble(TAG_TARGET_Y, target.y);
		tag.putDouble(TAG_TARGET_Z, target.z);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		spawningStack = ItemStack.parseOptional(registryAccess(), tag.getCompound(TAG_SPAWNING_ITEM));
		setTarget(tag.contains(TAG_TARGET_X)? new Vec3(tag.getDouble(TAG_TARGET_X), tag.getDouble(TAG_TARGET_Y), tag.getDouble(TAG_TARGET_Z)) : position());
	}
}
