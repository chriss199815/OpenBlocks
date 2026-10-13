package openblocks.block.entity;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import openblocks.OBConfig;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code TileEntityGrave}: holds the dead player's items, the name shown on the stone and the death message.
 */
public class GraveBlockEntity extends BlockEntity {

	private static final String TAG_USERNAME = "Username";
	private static final String TAG_MESSAGE = "Message";

	private static final int THUNDER_TICKS = 35 * 20;

	private NonNullList<ItemStack> loot = NonNullList.create();

	private String username = "";

	@Nullable
	private Component deathMessage;

	public GraveBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.GRAVE.get(), pos, state);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public void setDeathMessage(Component message) {
		this.deathMessage = message.copy();
		setChanged();
	}

	public List<ItemStack> getLoot() {
		return loot;
	}

	public void setLoot(List<ItemStack> stacks) {
		loot = NonNullList.create();
		for (ItemStack stack : stacks)
			if (!stack.isEmpty()) loot.add(stack.copy());
		setChanged();
	}

	public void dropLoot() {
		for (ItemStack stack : loot)
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
		loot = NonNullList.create();
		setChanged();
	}

	public void showDeathMessage(Player player) {
		if (deathMessage != null) player.sendSystemMessage(deathMessage);
	}

	/**
	 * Digging up the grave with a shovel releases the items; rarely this angers the weather.
	 */
	public boolean rob(Player player) {
		if (loot.isEmpty()) return false;

		dropLoot();
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, worldPosition, Block.getId(Blocks.DIRT.defaultBlockState()));

		if (level instanceof ServerLevel serverLevel && level.random.nextDouble() < OBConfig.GRAVES.specialActionFrequency.get()) {
			level.playSound(null, player.blockPosition(), OBSounds.GRAVE_ROB.get(), SoundSource.BLOCKS, 1, 1);
			serverLevel.setWeatherParameters(0, THUNDER_TICKS, true, true);
		}
		return true;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, GraveBlockEntity grave) {
		if (!OBConfig.GRAVES.spawnSkeletons.getAsBoolean() || level.getDifficulty() == Difficulty.PEACEFUL) return;
		if (level.random.nextDouble() >= OBConfig.GRAVES.skeletonSpawnRate.get()) return;

		final List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(7), mob -> mob instanceof Enemy);
		if (mobs.size() >= 5) return;

		final EntityType<? extends Mob> type = level.random.nextDouble() < 0.5? EntityType.SKELETON : EntityType.BAT;
		final Mob mob = type.create(level);
		if (mob == null) return;

		mob.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		if (mob.checkSpawnRules(level, MobSpawnType.EVENT) && mob.checkSpawnObstruction(level)) {
			if (level instanceof ServerLevel serverLevel) mob.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
			level.addFreshEntity(mob);
		}
	}

	/**
	 * Legacy: a grave placed by a creative player gets a copy of their inventory.
	 */
	public void onPlacedBy(LivingEntity placer, ItemStack stack) {
		if (!(placer instanceof Player player)) return;
		final Component customName = stack.get(DataComponents.CUSTOM_NAME);
		setUsername(customName != null? customName.getString() : player.getGameProfile().getName());
		if (player.isCreative()) {
			final NonNullList<ItemStack> copy = NonNullList.create();
			for (int i = 0; i < player.getInventory().getContainerSize(); i++)
				copy.add(player.getInventory().getItem(i));
			setLoot(copy);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		username = tag.getString(TAG_USERNAME);
		deathMessage = tag.contains(TAG_MESSAGE)? Component.Serializer.fromJson(tag.getString(TAG_MESSAGE), registries) : null;
		// update packets only carry the username
		if (tag.contains("Items")) {
			final NonNullList<ItemStack> items = NonNullList.withSize(tag.getList("Items", 10).size(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(tag, items, registries);
			setLootFrom(items);
		}
	}

	private void setLootFrom(NonNullList<ItemStack> items) {
		loot = NonNullList.create();
		for (ItemStack stack : items)
			if (!stack.isEmpty()) loot.add(stack);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString(TAG_USERNAME, username);
		if (deathMessage != null) tag.putString(TAG_MESSAGE, Component.Serializer.toJson(deathMessage, registries));
		ContainerHelper.saveAllItems(tag, loot, true, registries);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		tag.putString(TAG_USERNAME, username);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
