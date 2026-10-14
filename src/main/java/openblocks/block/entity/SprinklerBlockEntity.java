package openblocks.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.OBConfig;
import openblocks.block.SprinklerBlock;
import openblocks.common.OBFakePlayer;
import openblocks.menu.SprinklerMenu;
import openblocks.particle.LiquidSprayOptions;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntitySprinkler}: uses water from its tank (filled from below or through the fluid capability) to bone meal
 * random blocks around it, faster with bone meal in its inventory, and keeps farmland in range moist while it runs.
 */
public class SprinklerBlockEntity extends BlockEntity implements MenuProvider {

	public static final int SLOTS = 9;

	// legacy particle spacing along the nozzle and sideways scatter
	private static final double OUTLET_SPACING = 0.2;
	private static final double SPRAY_SIDE_SCATTER = Math.toRadians(25);
	public static final float MAX_NOZZLE_ANGLE = 25;

	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.is(Items.BONE_MEAL);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private final FluidTank tank = new FluidTank(OBConfig.SPRINKLER.internalTankCapacity.get(), fluid -> fluid.is(Fluids.WATER)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};

	private boolean enabled;
	private boolean hasBonemeal;
	private int ticks;
	@Nullable
	private AABBTicket waterTicket;

	public SprinklerBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.SPRINKLER.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public IFluidHandler getTank() {
		return tank;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SprinklerBlockEntity sprinkler) {
		sprinkler.serverTick();
	}

	private void serverTick() {
		if (tank.isEmpty()) {
			final IFluidHandler below = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP);
			if (below != null) FluidUtil.tryFluidTransfer(tank, below, tank.getCapacity(), true);
		}

		if (ticks % OBConfig.SPRINKLER.waterConsumeRate.get() == 0)
			setEnabled(!tank.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty());

		// unlike legacy, bone meal is only used up while the sprinkler runs
		if (enabled && ticks % OBConfig.SPRINKLER.bonemealConsumeRate.get() == 0) hasBonemeal = consumeBonemeal();

		ticks++;
		if (enabled) attemptFertilize();
	}

	private boolean consumeBonemeal() {
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			if (!inventory.extractItem(slot, 1, false).isEmpty()) return true;
		return false;
	}

	private void attemptFertilize() {
		final int chance = hasBonemeal? OBConfig.SPRINKLER.bonemealFertilizeChance.get() : OBConfig.SPRINKLER.fertilizeChance.get();
		if (level.random.nextInt(chance) != 0) return;

		final int range = OBConfig.SPRINKLER.effectiveRange.get();
		final int x = level.random.nextInt(2 * range + 1) - range;
		final int z = level.random.nextInt(2 * range + 1) - range;
		for (int y = -1; y <= 1; y++) {
			final BlockPos target = worldPosition.offset(x, y, z);
			if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, target, OBFakePlayer.get((ServerLevel)level))) {
				level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, 15);
				break;
			}
		}
	}

	private void setEnabled(boolean enabled) {
		updateWaterTicket(enabled);
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/**
	 * Replaces the legacy water material: farmland next to or one block below the sprinkler stays moist while it has water.
	 */
	private void updateWaterTicket(boolean enabled) {
		if (enabled) {
			if (waterTicket == null) {
				final int range = OBConfig.SPRINKLER.effectiveRange.get();
				waterTicket = FarmlandWaterManager.addAABBTicket(level, new AABB(worldPosition).inflate(range, 0, range).expandTowards(0, -1, 0));
			} else if (!waterTicket.isValid()) {
				waterTicket.validate();
			}
		} else if (waterTicket != null) {
			waterTicket.invalidate();
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (waterTicket != null) waterTicket.invalidate();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (waterTicket != null) waterTicket.invalidate();
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, SprinklerBlockEntity sprinkler) {
		sprinkler.ticks++;
		if (sprinkler.enabled) sprinkler.sprayParticles();
	}

	/**
	 * Swings from -1 to 1 while the sprinkler runs.
	 */
	public float getSprayDirection(float partialTick) {
		return enabled? Mth.sin((ticks + partialTick) * 0.02f) : 0;
	}

	private void sprayParticles() {
		final boolean alongX = getBlockState().getValue(SprinklerBlock.AXIS) == Direction.Axis.X;
		// nozzle direction and the direction it sprays to at full swing, see SprinklerRenderer
		final Vec3 nozzle = alongX? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
		final Vec3 forward = alongX? new Vec3(0, 0, -1) : new Vec3(1, 0, 0);
		final Vec3 center = Vec3.atBottomCenterOf(worldPosition).add(0, 0.2, 0);
		final double forwardVelocity = Math.sin(Math.toRadians(getSprayDirection(0) * MAX_NOZZLE_ANGLE)) / 2;
		final LiquidSprayOptions particle = new LiquidSprayOptions(new FluidStack(Fluids.WATER, 1), 0.3f, 0.7f);

		// the particle engine thins out particles according to the client's particle setting
		for (double outlet = -0.5; outlet <= 0.5; outlet += OUTLET_SPACING) {
			final double sideVelocity = Math.sin(SPRAY_SIDE_SCATTER * (level.random.nextDouble() - 0.5));
			final Vec3 position = center.add(nozzle.scale(outlet * 0.6));
			final Vec3 velocity = forward.scale(forwardVelocity).add(nozzle.scale(sideVelocity)).add(0, 0.35, 0);
			level.addParticle(particle, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
		}
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.sprinkler");
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new SprinklerMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		tank.readFromNBT(registries, tag.getCompound("tank"));
		enabled = tag.getBoolean("enabled");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putBoolean("enabled", enabled);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		tag.putBoolean("enabled", enabled);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
