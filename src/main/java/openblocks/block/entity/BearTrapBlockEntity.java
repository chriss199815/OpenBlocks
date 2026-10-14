package openblocks.block.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code TileEntityBearTrap}: new traps are shut; once opened by hand, the first mob that walks in is held in place
 * until the trap is opened again. A redstone signal opens the trap and keeps it from closing.
 */
public class BearTrapBlockEntity extends BlockEntity {

	public static final int OPENING_TICKS = 15;
	// the trapped entity may load after the trap, so it is only given up after a while
	private static final int MISSING_ENTITY_TICKS = 40;

	private boolean shut = true;
	private boolean locked;
	@Nullable
	private UUID trappedId;

	private int ticksSinceOpened = OPENING_TICKS;
	private int missingTicks;
	private boolean powerChecked;
	@Nullable
	private Entity trapped;

	public BearTrapBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.BEAR_TRAP.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BearTrapBlockEntity trap) {
		// not on load, neighbors may not be available yet
		if (!trap.powerChecked) trap.updateRedstone();
		trap.ticksSinceOpened++;
		trap.holdEntity();
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, BearTrapBlockEntity trap) {
		trap.ticksSinceOpened++;
	}

	private void holdEntity() {
		if (trappedId == null) return;

		if (trapped == null || !trapped.getUUID().equals(trappedId)) trapped = ((ServerLevel)level).getEntity(trappedId);
		if (trapped == null) {
			if (++missingTicks > MISSING_ENTITY_TICKS) open();
			return;
		}
		missingTicks = 0;

		if (!trapped.isAlive()) {
			open();
			return;
		}

		final Vec3 center = Vec3.atBottomCenterOf(worldPosition);
		trapped.moveTo(center.x, center.y, center.z, trapped.getYRot(), trapped.getXRot());
		trapped.setDeltaMovement(Vec3.ZERO);
	}

	public void onEntityInside(Entity entity) {
		if (!shut && !locked && ticksSinceOpened > OPENING_TICKS && entity instanceof PathfinderMob) close(entity);
	}

	private void close(Entity entity) {
		shut = true;
		trappedId = entity.getUUID();
		trapped = entity;
		missingTicks = 0;
		level.playSound(null, worldPosition, OBSounds.BEARTRAP_CLOSE.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
		sync();
	}

	public void open() {
		if (!shut) return;
		shut = false;
		trappedId = null;
		trapped = null;
		ticksSinceOpened = 0;
		level.playSound(null, worldPosition, OBSounds.BEARTRAP_OPEN.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
		sync();
	}

	public void updateRedstone() {
		if (level == null || level.isClientSide) return;
		powerChecked = true;
		locked = level.hasNeighborSignal(worldPosition);
		if (locked) open();
	}

	public boolean isShut() {
		return shut;
	}

	public boolean isLocked() {
		return locked;
	}

	@Nullable
	public UUID getTrappedId() {
		return trappedId;
	}

	/**
	 * How far the jaws are closed: 1 when shut, falling to 0 while opening.
	 */
	public float getClosedFraction(float partialTick) {
		if (shut) return 1;
		return Math.max(0, 1 - (ticksSinceOpened + partialTick) / OPENING_TICKS);
	}

	public int getComparatorLevel() {
		// legacy formula, which gives 1 for any trapped entity
		return trapped != null? Mth.ceil(trapped.getBoundingBox().getSize() / 16.0) : 0;
	}

	private void sync() {
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		final boolean wasShut = shut;
		shut = tag.getBoolean("shut");
		trappedId = tag.hasUUID("trapped")? tag.getUUID("trapped") : null;
		// update packets carry no animation state, so the client starts it when the trap opens
		if (wasShut && !shut && level != null && level.isClientSide) ticksSinceOpened = 0;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("shut", shut);
		if (trappedId != null) tag.putUUID("trapped", trappedId);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
