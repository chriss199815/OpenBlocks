package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import openblocks.OBConfig;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityFan}: blows entities in a cone, with a force scaled by the redstone signal.
 * Each side moves the entities it simulates: the server mobs and items, the client its own player.
 */
public class FanBlockEntity extends BlockEntity {

	public static final float ROTATION_STEP = 10;

	private static final float BLADE_SPEED_PER_POWER = 45 / 15f;
	private static final double CONE_HALF_APERTURE_COS = Math.cos(1.2 / 2.0);
	// the cone starts behind the fan, so entities right next to it are blown too
	private static final double CONE_APEX_OFFSET = 1.1;

	private float angle;
	private int power;
	private boolean powerChecked;

	private float bladeRotation;
	private float bladeSpeed;

	public FanBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.FAN.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FanBlockEntity fan) {
		// not on load, neighbors may not be available yet
		if (!fan.powerChecked) fan.updateRedstone();
		fan.blow();
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, FanBlockEntity fan) {
		fan.bladeSpeed = fan.power * BLADE_SPEED_PER_POWER;
		fan.bladeRotation = (fan.bladeRotation + fan.bladeSpeed) % 360;
		fan.blow();
	}

	private void blow() {
		final double maxForce = OBConfig.FAN.force.get() * power / 15.0;
		if (maxForce <= 0) return;

		final double range = OBConfig.FAN.range.get();
		final double direction = Math.toRadians(angle - 90);
		final Vec3 axis = new Vec3(Math.cos(direction), 0, Math.sin(direction));
		final Vec3 apex = Vec3.atCenterOf(worldPosition).subtract(axis.scale(CONE_APEX_OFFSET));
		final AABB area = new AABB(worldPosition.getX(), worldPosition.getY() - 2, worldPosition.getZ(),
				worldPosition.getX() + 1, worldPosition.getY() + 3, worldPosition.getZ() + 1).inflate(range);

		for (Entity entity : level.getEntitiesOfClass(Entity.class, area, this::isSimulatedHere)) {
			final Vec3 toEntity = entity.position().subtract(apex);
			final double distance = toEntity.length();
			if (distance == 0 || toEntity.dot(axis) / distance <= CONE_HALF_APERTURE_COS) continue;

			final double force = (1.0 - distance / range) * maxForce;
			if (force <= 0) continue;

			entity.setDeltaMovement(entity.getDeltaMovement().add(force * toEntity.x / distance, 0, force * toEntity.z / distance));
			if (!level.isClientSide) entity.hasImpulse = true;
		}
	}

	private boolean isSimulatedHere(Entity entity) {
		if (entity.isSpectator()) return false;
		if (entity instanceof Player player) return player.isLocalPlayer() && !player.isCreative();
		return entity.isControlledByLocalInstance();
	}

	public void updateRedstone() {
		if (level == null || level.isClientSide) return;
		powerChecked = true;
		final int newPower = OBConfig.FAN.isRedstoneActivated.getAsBoolean()? level.getBestNeighborSignal(worldPosition) : 15;
		if (newPower != power) {
			power = newPower;
			sync();
		}
	}

	public float getAngle() {
		return angle;
	}

	public void setAngle(float angle) {
		this.angle = Mth.wrapDegrees(angle);
		sync();
	}

	public int getPower() {
		return power;
	}

	public float getBladeRotation(float partialTick) {
		return bladeRotation + bladeSpeed * partialTick;
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		angle = tag.getFloat("angle");
		power = tag.getByte("power");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putFloat("angle", angle);
		tag.putByte("power", (byte)power);
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
