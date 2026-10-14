package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.common.TrophyBehavior;
import openblocks.common.TrophyType;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code TileEntityTrophy}: the mob type, shown by {@code TrophyRenderer}, and the cooldown of its click
 * behaviour. Both go into the item when broken (like legacy), so breaking and placing doesn't reset the cooldown.
 */
public class TrophyBlockEntity extends BlockEntity {

	private static final String TAG_TYPE = "type";
	private static final String TAG_COOLDOWN = "cooldown";

	private TrophyType type = TrophyType.DEFAULT;
	private int cooldown;

	public TrophyBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.TROPHY.get(), pos, state);
	}

	public TrophyType getTrophyType() {
		return type;
	}

	public int getCooldown() {
		return cooldown;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TrophyBlockEntity trophy) {
		if (trophy.cooldown > 0) {
			trophy.cooldown--;
			trophy.setChanged();
		}
	}

	/**
	 * Plays the mob's sound, then runs its behaviour unless it is cooling down.
	 */
	public void activate(Player player) {
		final Entity entity = type.getEntityType().create(level);
		if (entity instanceof Mob mob) {
			mob.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
			mob.playAmbientSound();
		}

		final TrophyBehavior behavior = type.getBehavior();
		if (behavior != null && cooldown <= 0) {
			cooldown = behavior.activate(this, player);
			setChanged();
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		type = TrophyType.CODEC.byName(tag.getString(TAG_TYPE), TrophyType.DEFAULT);
		cooldown = tag.getInt(TAG_COOLDOWN);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString(TAG_TYPE, type.getSerializedName());
		tag.putInt(TAG_COOLDOWN, cooldown);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput componentInput) {
		super.applyImplicitComponents(componentInput);
		type = componentInput.getOrDefault(OBComponents.TROPHY, TrophyType.DEFAULT);
		cooldown = componentInput.getOrDefault(OBComponents.TROPHY_COOLDOWN, 0);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(OBComponents.TROPHY, type);
		if (cooldown > 0) components.set(OBComponents.TROPHY_COOLDOWN, cooldown);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove(TAG_TYPE);
		tag.remove(TAG_COOLDOWN);
	}

	// the type is drawn by the renderer
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		tag.putString(TAG_TYPE, type.getSerializedName());
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
