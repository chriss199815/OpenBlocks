package openblocks.block.entity;

import java.util.EnumSet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.common.AutoSlotConfig;
import openblocks.common.FluidXpUtils;
import openblocks.common.MachineTransfer;
import openblocks.common.OBFakePlayer;
import openblocks.common.SidedItemHandler;
import openblocks.menu.AutoAnvilMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.util.XpUtils;

/**
 * Port of legacy {@code TileEntityAutoAnvil}: combines the tool and modifier like a vanilla anvil, paid with XP from its tank.
 */
public class AutoAnvilBlockEntity extends BlockEntity implements MenuProvider {

	public enum AutoSlot {
		/** Item to repair or enchant; auto extract pulls it from neighbours. */
		TOOL,
		/** Material or enchanted book; auto extract pulls it from neighbours. */
		MODIFIER,
		/** Result; auto eject pushes it to neighbours. */
		OUTPUT,
		/** XP fluid; auto drink pulls it from neighbours. */
		XP
	}

	public static final int SLOT_TOOL = 0;
	public static final int SLOT_MODIFIER = 1;
	public static final int SLOT_OUTPUT = 2;

	public static final int MAX_STORED_LEVELS = 45;
	public static final int COOLDOWN = 40;

	private static final int PULL_PER_TICK = 100;

	private final ItemStackHandler inventory = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot != SLOT_OUTPUT;
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private final FluidTank tank = new FluidTank(getTankCapacity(), FluidXpUtils::isXpFluid) {
		@Override
		protected void onContentsChanged() {
			setChanged();
			sendUpdate();
		}
	};

	// unlike legacy (nothing automatic until configured), auto drink is on by default, like the XP bottler
	private final AutoSlotConfig<AutoSlot> config = new AutoSlotConfig<>(AutoSlot.class, EnumSet.of(AutoSlot.XP), this::onConfigChanged);

	/**
	 * Index 6 is for the null side.
	 */
	private final IItemHandler[] itemHandlers = new IItemHandler[7];

	/**
	 * Fill-only, like legacy.
	 */
	private final IFluidHandler fluidHandler = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int index) {
			return tank.getFluid();
		}

		@Override
		public int getTankCapacity(int index) {
			return tank.getCapacity();
		}

		@Override
		public boolean isFluidValid(int index, FluidStack stack) {
			return tank.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return tank.fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	};

	private int cooldown;

	public AutoAnvilBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.AUTO_ANVIL.get(), pos, state);
		final Map<Integer, AutoSlot> inputs = Map.of(SLOT_TOOL, AutoSlot.TOOL, SLOT_MODIFIER, AutoSlot.MODIFIER);
		final Map<Integer, AutoSlot> outputs = Map.of(SLOT_OUTPUT, AutoSlot.OUTPUT);
		for (Direction side : Direction.values())
			itemHandlers[side.get3DDataValue()] = new SidedItemHandler<>(inventory, config, inputs, outputs, side);
		itemHandlers[6] = new SidedItemHandler<>(inventory, config, inputs, outputs, null);
	}

	public static int getTankCapacity() {
		return FluidXpUtils.xpToFluid(XpUtils.getExperienceForLevel(MAX_STORED_LEVELS));
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public AutoSlotConfig<AutoSlot> getConfig() {
		return config;
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		if (side == null) return itemHandlers[6];
		final boolean open = config.isOpen(AutoSlot.TOOL, side) || config.isOpen(AutoSlot.MODIFIER, side) || config.isOpen(AutoSlot.OUTPUT, side);
		return open? itemHandlers[side.get3DDataValue()] : null;
	}

	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side == null || config.isOpen(AutoSlot.XP, side)? fluidHandler : null;
	}

	private void onConfigChanged() {
		setChanged();
		if (level != null) level.invalidateCapabilities(worldPosition);
		sendUpdate();
	}

	private void sendUpdate() {
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AutoAnvilBlockEntity anvil) {
		final AutoSlotConfig<AutoSlot> config = anvil.config;
		if (config.isAuto(AutoSlot.XP) && anvil.tank.getSpace() > 0)
			MachineTransfer.pullFluid(level, pos, config.getSides(AutoSlot.XP), anvil.tank, PULL_PER_TICK);
		if (config.isAuto(AutoSlot.OUTPUT))
			MachineTransfer.pushItem(level, pos, config.getSides(AutoSlot.OUTPUT), anvil.inventory, SLOT_OUTPUT);
		// like legacy, the tool only when its slot is empty, modifiers until the stack is full
		if (config.isAuto(AutoSlot.TOOL) && anvil.inventory.getStackInSlot(SLOT_TOOL).isEmpty())
			MachineTransfer.pullItem(level, pos, config.getSides(AutoSlot.TOOL), anvil.inventory, SLOT_TOOL);
		if (config.isAuto(AutoSlot.MODIFIER))
			MachineTransfer.pullItem(level, pos, config.getSides(AutoSlot.MODIFIER), anvil.inventory, SLOT_MODIFIER);

		if (anvil.cooldown-- < 0 && anvil.inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
			anvil.combine((ServerLevel)level);
			anvil.cooldown = COOLDOWN;
		}
	}

	/**
	 * Runs the vanilla anvil logic (including NeoForge's {@code AnvilUpdateEvent}) in a menu opened by a fake player.
	 */
	private void combine(ServerLevel level) {
		final ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
		if (tool.isEmpty()) return;

		final FakePlayer player = OBFakePlayer.get(level);
		final AnvilMenu anvil = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		// unlike legacy, keeps custom names: without a name, the anvil would remove them
		if (tool.has(DataComponents.CUSTOM_NAME)) anvil.setItemName(tool.getHoverName().getString());
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(tool.copy());
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(inventory.getStackInSlot(SLOT_MODIFIER).copy());

		final ItemStack output = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		final int levelCost = anvil.getCost();
		// a vanilla anvil doesn't allow taking free results either
		if (output.isEmpty() || levelCost <= 0) return;

		final int fluidCost = FluidXpUtils.xpToFluid(XpUtils.getExperienceForLevel(levelCost));
		if (tank.drain(fluidCost, IFluidHandler.FluidAction.SIMULATE).getAmount() < fluidCost) return;
		tank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);

		final ItemStack modifier = inventory.getStackInSlot(SLOT_MODIFIER);
		final int modifierCost = anvil.repairItemCountCost;
		inventory.setStackInSlot(SLOT_MODIFIER, modifierCost > 0 && modifier.getCount() > modifierCost? modifier.copyWithCount(modifier.getCount() - modifierCost) : ItemStack.EMPTY);
		inventory.setStackInSlot(SLOT_TOOL, ItemStack.EMPTY);
		inventory.setStackInSlot(SLOT_OUTPUT, output.copy());
		level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3f, 1.0f);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.autoanvil");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new AutoAnvilMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag.getCompound("tank"));
		// update packets don't carry the inventory; the open menu uses it on the client, so it must not be cleared
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		config.load(tag);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		writeUpdateData(tag, registries);
	}

	private void writeUpdateData(CompoundTag tag, HolderLookup.Provider registries) {
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		config.save(tag);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		// the client needs the tank and the side configuration for the GUI
		final CompoundTag tag = new CompoundTag();
		writeUpdateData(tag, registries);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
