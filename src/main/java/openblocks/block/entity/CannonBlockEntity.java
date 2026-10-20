package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import openblocks.common.CannonBallistics;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code TileEntityCannon}: once a second while powered, takes a stack out of a neighbouring inventory and
 * shoots it. The pointer sets the target, which becomes the shot velocity; the client turns the barrel towards it.
 */
public class CannonBlockEntity extends BlockEntity {

	/**
	 * Where shots start and the trajectory line is drawn from, relative to the block.
	 */
	public static final Vec3 ORIGIN = new Vec3(0.5, 0.5, 0.5);

	private static final int FIRE_INTERVAL = 20;
	private static final int EVENT_FIRED = 1;

	private static final double YAW_CHANGE_SPEED = 3;
	private static final double PITCH_CHANGE_SPEED = 20;

	// legacy default: pitch 45, yaw 0, speed 1.4, which shoots south
	private static final Vec3 DEFAULT_VELOCITY = new Vec3(0, Math.sin(Math.toRadians(45)) * 1.4, Math.cos(Math.toRadians(45)) * 1.4);

	private static final String TAG_VELOCITY_X = "VelocityX";
	private static final String TAG_VELOCITY_Y = "VelocityY";
	private static final String TAG_VELOCITY_Z = "VelocityZ";

	private Vec3 velocity = DEFAULT_VELOCITY;

	// client only, for the model
	private double currentYaw = Double.NaN;
	private double currentPitch;
	private int ticksSinceLastFire = Integer.MAX_VALUE;

	public CannonBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.CANNON.get(), pos, state);
	}

	public Vec3 getVelocity() {
		return velocity;
	}

	/**
	 * Aims at the top centre of the given block.
	 */
	public void setTarget(BlockPos target) {
		velocity = CannonBallistics.velocityTo(Vec3.atBottomCenterOf(target).add(0, 1, 0).subtract(Vec3.atLowerCornerOf(worldPosition).add(ORIGIN)));
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/**
	 * Aims by angles, like legacy {@code setYaw}/{@code setPitch}/{@code setSpeed} (computers): yaw 0 shoots south, 90 west;
	 * pitch is the angle above the horizon.
	 */
	public void setAim(double yaw, double pitch, double speed) {
		final double yawRad = Math.toRadians(yaw + 90);
		final double pitchRad = Math.toRadians(pitch);
		velocity = new Vec3(Math.cos(pitchRad) * Math.cos(yawRad), Math.sin(pitchRad), Math.cos(pitchRad) * Math.sin(yawRad)).scale(speed);
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public double getAimYaw() {
		return Mth.wrapDegrees(targetYaw());
	}

	public double getAimPitch() {
		return targetPitch();
	}

	public double getSpeed() {
		return velocity.length();
	}

	public double getYaw() {
		return currentYaw;
	}

	public double getPitch() {
		return currentPitch;
	}

	public int getTicksSinceLastFire() {
		return ticksSinceLastFire;
	}

	private double targetYaw() {
		return Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90;
	}

	private double targetPitch() {
		return Math.toDegrees(Math.asin(velocity.normalize().y));
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, CannonBlockEntity cannon) {
		final double targetYaw = Mth.wrapDegrees(cannon.targetYaw());
		if (Double.isNaN(cannon.currentYaw)) {
			cannon.currentYaw = targetYaw;
			cannon.currentPitch = cannon.targetPitch();
		}

		cannon.currentPitch -= (cannon.currentPitch - cannon.targetPitch()) / PITCH_CHANGE_SPEED;
		final double yawDistance = Mth.wrapDegrees(targetYaw - cannon.currentYaw);
		if (Math.abs(yawDistance) < YAW_CHANGE_SPEED) cannon.currentYaw = targetYaw;
		else cannon.currentYaw = Mth.wrapDegrees(cannon.currentYaw + YAW_CHANGE_SPEED * Math.signum(yawDistance));

		if (cannon.ticksSinceLastFire < 100) cannon.ticksSinceLastFire++;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CannonBlockEntity cannon) {
		if (level.getGameTime() % FIRE_INTERVAL != 0 || !level.hasNeighborSignal(pos)) return;

		final ItemStack stack = cannon.takeStack();
		if (!stack.isEmpty()) cannon.fire(stack);
	}

	private ItemStack takeStack() {
		for (Direction side : Direction.values()) {
			final IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
			if (inventory == null) continue;
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				final ItemStack stack = inventory.extractItem(slot, 64, false);
				if (!stack.isEmpty()) return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private void fire(ItemStack stack) {
		final Vec3 origin = Vec3.atLowerCornerOf(worldPosition).add(ORIGIN);
		final ItemEntity item = new ItemEntity(level, origin.x, origin.y, origin.z, stack, velocity.x, velocity.y, velocity.z);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
		level.playSound(null, worldPosition, OBSounds.CANNON_ACTIVATE.get(), SoundSource.BLOCKS, 0.2f, 1.0f);
		level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIRED, 0);
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id != EVENT_FIRED) return super.triggerEvent(id, param);
		if (level != null && level.isClientSide) {
			ticksSinceLastFire = 0;
			// smoke behind the barrel, like legacy
			final double yaw = Math.toRadians(currentYaw - 90);
			final double x = worldPosition.getX() - 0.5 * Math.cos(yaw);
			final double z = worldPosition.getZ() - 0.5 * Math.sin(yaw);
			for (int i = 0; i < 20; i++)
				level.addParticle(i < 4? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
						x + 0.3 + level.random.nextDouble() * 0.4, worldPosition.getY() + 0.7, z + 0.3 + level.random.nextDouble() * 0.4, 0, 0, 0);
		}
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putDouble(TAG_VELOCITY_X, velocity.x);
		tag.putDouble(TAG_VELOCITY_Y, velocity.y);
		tag.putDouble(TAG_VELOCITY_Z, velocity.z);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		velocity = tag.contains(TAG_VELOCITY_X)? new Vec3(tag.getDouble(TAG_VELOCITY_X), tag.getDouble(TAG_VELOCITY_Y), tag.getDouble(TAG_VELOCITY_Z)) : DEFAULT_VELOCITY;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
