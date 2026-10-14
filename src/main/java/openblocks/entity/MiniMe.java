package openblocks.entity;

import com.mojang.authlib.properties.PropertyMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code EntityMiniMe}: a small copy of the player who placed the golden egg, with their skin. It wanders
 * around, picks flowers and torches, and carries its owner around. Naming it with a name tag changes the skin to the
 * player of that name, like legacy. The owner is synced with its resolved skin (like player heads), replacing the legacy
 * owner change event.
 */
public class MiniMe extends PathfinderMob {

	public static final int PICKUP_COOLDOWN = 1200;

	private static final EntityDataAccessor<Optional<ResolvableProfile>> DATA_OWNER = SynchedEntityData.defineId(MiniMe.class, OBEntities.OPTIONAL_PROFILE.get());

	private static final String TAG_OWNER = "Owner";
	private static final String TAG_PICKUP_COOLDOWN = "PickupCooldown";

	private int pickupCooldown;

	private boolean wasRidden;

	public MiniMe(EntityType<? extends MiniMe> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setPathfindingMalus(PathType.WATER, -1.0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MiniMePickUpOwnerGoal(this));
		goalSelector.addGoal(3, new MiniMeBreakBlockGoal(this));
		goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		final GroundPathNavigation navigation = new GroundPathNavigation(this, level);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER, Optional.empty());
	}

	@Nullable
	public ResolvableProfile getOwner() {
		return entityData.get(DATA_OWNER).orElse(null);
	}

	/**
	 * Sets the owner; on the server an unresolved profile (only a name or id) is looked up first, so clients get the skin.
	 */
	public void setOwner(ResolvableProfile owner) {
		entityData.set(DATA_OWNER, Optional.of(owner));
		if (!level().isClientSide && !owner.isResolved() && getServer() != null)
			owner.resolve().thenAcceptAsync(resolved -> {
				// only if nobody changed the owner in the meantime
				if (getOwner() == owner) entityData.set(DATA_OWNER, Optional.of(resolved));
			}, getServer());
	}

	public boolean isOwner(Player player) {
		final ResolvableProfile owner = getOwner();
		return owner != null && player.getGameProfile().getId().equals(owner.gameProfile().getId());
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		if (level().isClientSide || name == null) return;

		final String playerName = name.getString();
		final ResolvableProfile owner = getOwner();
		if (owner == null || !playerName.equalsIgnoreCase(owner.gameProfile().getName()))
			setOwner(new ResolvableProfile(Optional.of(playerName), Optional.empty(), new PropertyMap()));
	}

	@Override
	public void tick() {
		super.tick();
		if (pickupCooldown > 0) pickupCooldown--;
		if (wasRidden && !isVehicle()) {
			wasRidden = false;
			pickupCooldown = PICKUP_COOLDOWN;
		} else if (isVehicle()) {
			wasRidden = true;
		}
	}

	public int getPickupCooldown() {
		return pickupCooldown;
	}

	public void setPickupCooldown(int cooldown) {
		pickupCooldown = cooldown;
	}

	@Override
	public boolean isBaby() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		final ResolvableProfile owner = getOwner();
		if (owner != null) tag.put(TAG_OWNER, ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, owner).getOrThrow());
		tag.putInt(TAG_PICKUP_COOLDOWN, pickupCooldown);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		// before the name, so loading does not look up the owner by name again
		if (tag.contains(TAG_OWNER))
			ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_OWNER)).result().ifPresent(this::setOwner);
		super.readAdditionalSaveData(tag);
		pickupCooldown = tag.getInt(TAG_PICKUP_COOLDOWN);
	}
}
