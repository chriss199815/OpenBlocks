package openblocks.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.Tags;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code EntityLuggage}: a tamed chest on legs that follows its owner and collects items from the floor.
 * Sneak-click turns it back into an item that keeps the inventory; lightning makes it "special" with twice the slots.
 */
public class Luggage extends TamableAnimal implements PowerableMob {

	public static final int SIZE_NORMAL = 27;
	public static final int SIZE_SPECIAL = 54;

	private static final EntityDataAccessor<Boolean> DATA_SPECIAL = SynchedEntityData.defineId(Luggage.class, EntityDataSerializers.BOOLEAN);

	private static final String TAG_SPECIAL = "Special";
	private static final String TAG_ITEM = "Item";

	private LuggageInventory inventory = new LuggageInventory(SIZE_NORMAL);

	// the item this luggage was spawned from, to keep its other components (like legacy ItemTag)
	private ItemStack item = ItemStack.EMPTY;

	private int lastSound;

	public Luggage(EntityType<? extends Luggage> type, Level level) {
		super(type, level);
		setTame(true, false);
		setPersistenceRequired();
		setPathfindingMalus(PathType.WATER, -1.0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		// legacy used the default movement speed of 0.7
		return Mob.createMobAttributes();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(1, new SitWhenOrderedToGoal(this) {
			// vanilla also sits while the owner is away, which would stop collecting
			@Override
			public boolean canUse() {
				return isOrderedToSit() && onGround() && !isInWaterOrBubble();
			}
		});
		goalSelector.addGoal(2, new FollowOwnerGoal(this, 0.7, 10.0f, 2.0f));
		goalSelector.addGoal(3, new LuggageCollectItemGoal(this));
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
		builder.define(DATA_SPECIAL, false);
	}

	public boolean isSpecial() {
		return entityData.get(DATA_SPECIAL);
	}

	public void setSpecial() {
		if (isSpecial()) return;
		entityData.set(DATA_SPECIAL, true);
		final LuggageInventory special = new LuggageInventory(SIZE_SPECIAL);
		for (int slot = 0; slot < inventory.getContainerSize(); slot++)
			special.setItem(slot, inventory.getItem(slot));
		// open menus on the old inventory close, see LuggageInventory#stillValid
		inventory = special;
	}

	@Override
	public boolean isPowered() {
		return isSpecial();
	}

	public SimpleContainer getInventory() {
		return inventory;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		lastSound++;
	}

	public boolean canCollect(ItemStack stack) {
		return inventory.canAddItem(stack);
	}

	public void collect(ItemEntity itemEntity) {
		final ItemStack stack = itemEntity.getItem();
		final ItemStack leftover = inventory.addItem(stack.copy());
		if (leftover.getCount() >= stack.getCount()) return;

		if (lastSound > 15) {
			final boolean isFood = stack.getUseAnimation() == UseAnim.EAT;
			playSound(isFood? OBSounds.LUGGAGE_EAT_FOOD.get() : OBSounds.LUGGAGE_EAT_ITEM.get(), 0.5f, 1.0f + random.nextFloat() * 0.2f);
			lastSound = 0;
		}

		if (leftover.isEmpty()) itemEntity.discard();
		else itemEntity.setItem(leftover);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || player.getItemInHand(hand).is(Items.NAME_TAG)) return InteractionResult.PASS;

		final Level level = level();
		if (player.getItemInHand(hand).is(Tags.Items.RODS_WOODEN)) {
			// stay and follow like a sitting dog; not in legacy
			if (!player.getUUID().equals(getOwnerUUID())) return InteractionResult.PASS;
			if (!level.isClientSide) {
				setOrderedToSit(!isOrderedToSit());
				navigation.stop();
				player.displayClientMessage(Component.translatable(isOrderedToSit()? "openblocks.luggage.waiting" : "openblocks.luggage.following"), true);
			}
		} else if (player.isSecondaryUseActive()) {
			if (level.isClientSide) {
				spawnPickupParticles();
			} else if (player.getInventory().add(toItem())) {
				discard();
				playSound(SoundEvents.ITEM_PICKUP, 0.5f, random.nextFloat() * 0.1f + 0.9f);
			}
		} else if (!level.isClientSide) {
			playSound(SoundEvents.CHEST_OPEN, 0.5f, random.nextFloat() * 0.1f + 0.9f);
			final LuggageInventory menuInventory = inventory;
			player.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) -> isSpecial()
					? ChestMenu.sixRows(containerId, playerInventory, menuInventory)
					: ChestMenu.threeRows(containerId, playerInventory, menuInventory), getMenuTitle()));
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private Component getMenuTitle() {
		final Component customName = getCustomName();
		return customName != null? customName : Component.translatable("openblocks.gui.luggage");
	}

	private void spawnPickupParticles() {
		final double y = getY() + getBbHeight();
		for (int i = 0; i < 50; i++) {
			final double vx = random.nextGaussian() * 0.02;
			final double vz = random.nextGaussian() * 0.02;
			final double x = getX() - getBbWidth() / 2 + getBbWidth() * random.nextFloat();
			final double z = getZ() - getBbWidth() / 2 + getBbWidth() * random.nextFloat();
			level().addParticle(ParticleTypes.PORTAL, x, y, z, vx, -1, vz);
		}
	}

	/**
	 * The item that keeps contents, size and name; inverse of {@link #restoreFromItem}.
	 */
	public ItemStack toItem() {
		final ItemStack stack = item.isEmpty()? new ItemStack(OBItems.LUGGAGE.get()) : item.copy();
		stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory.getItems()));
		if (isSpecial()) stack.set(OBComponents.LUGGAGE_SPECIAL.get(), Unit.INSTANCE);
		if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
		else stack.remove(DataComponents.CUSTOM_NAME);
		return stack;
	}

	public void restoreFromItem(ItemStack stack) {
		if (stack.has(OBComponents.LUGGAGE_SPECIAL.get())) setSpecial();
		stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(inventory.getItems());
		setCustomName(stack.get(DataComponents.CUSTOM_NAME));

		item = stack.copyWithCount(1);
		item.remove(DataComponents.CONTAINER);
	}

	@Override
	public ItemStack getPickResult() {
		return toItem();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(OBSounds.LUGGAGE_WALK.get(), 0.3f, 0.7f + random.nextFloat() * 0.5f);
	}

	@Override
	public void thunderHit(ServerLevel level, LightningBolt lightning) {
		// no fire or damage, like legacy
		setSpecial();
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		// legacy was invulnerable to everything; /kill still works
		return !source.is(DamageTypes.GENERIC_KILL);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		for (ItemStack stack : inventory.removeAllItems())
			spawnAtLocation(stack);
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Nullable
	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
		return null;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean(TAG_SPECIAL, isSpecial());
		ContainerHelper.saveAllItems(tag, inventory.getItems(), registryAccess());
		if (!item.isEmpty()) tag.put(TAG_ITEM, item.save(registryAccess()));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.getBoolean(TAG_SPECIAL)) setSpecial();
		ContainerHelper.loadAllItems(tag, inventory.getItems(), registryAccess());
		item = ItemStack.parseOptional(registryAccess(), tag.getCompound(TAG_ITEM));
	}

	private class LuggageInventory extends SimpleContainer {

		LuggageInventory(int size) {
			super(size);
		}

		@Override
		public boolean stillValid(Player player) {
			return inventory == this && isAlive() && player.distanceToSqr(Luggage.this) < 64;
		}
	}
}
