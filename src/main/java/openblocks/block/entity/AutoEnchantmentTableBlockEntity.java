package openblocks.block.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.common.AutoSlotConfig;
import openblocks.common.FluidXpUtils;
import openblocks.common.MachineTransfer;
import openblocks.common.SidedItemHandler;
import openblocks.menu.AutoEnchantmentTableMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.util.XpUtils;

/**
 * Port of legacy {@code TileEntityAutoEnchantmentTable}: enchants items like the vanilla table (same bookshelves, costs and enchantments),
 * at one of the three vanilla options chosen in the GUI, paid with lapis and XP from its tank.
 */
public class AutoEnchantmentTableBlockEntity extends BlockEntity implements MenuProvider {

	public enum AutoSlot {
		/** Item to enchant; auto extract pulls it from neighbours. */
		TOOL,
		/** Lapis; auto extract pulls it from neighbours. */
		LAPIS,
		/** Result; auto eject pushes it to neighbours. */
		OUTPUT,
		/** XP fluid; auto drink pulls it from neighbours. */
		XP
	}

	public static final int SLOT_TOOL = 0;
	public static final int SLOT_LAPIS = 1;
	public static final int SLOT_OUTPUT = 2;

	public static final int MAX_STORED_LEVELS = 30;
	/**
	 * The vanilla maximum: 15 bookshelves.
	 */
	public static final int MAX_POWER = 30;

	public static final int DATA_POWER_LIMIT = 0;
	public static final int DATA_AVAILABLE_POWER = 1;
	public static final int DATA_OPTION = 2;

	private static final int PULL_PER_TICK = 80;
	private static final int POWER_CHECK_PERIOD = 20;

	private final ItemStackHandler inventory = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return switch (slot) {
				case SLOT_TOOL -> stack.isEnchantable();
				case SLOT_LAPIS -> stack.is(Tags.Items.GEMS_LAPIS);
				default -> false;
			};
		}

		@Override
		public int getSlotLimit(int slot) {
			// like the vanilla table, one item at a time
			return slot == SLOT_TOOL? 1 : super.getSlotLimit(slot);
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

	// unlike legacy (nothing automatic until configured), auto drink is on by default, like the other XP machines
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

	private final RandomSource random = RandomSource.create();

	/**
	 * Like the player's enchantment seed: fixes the costs and enchantments until something is enchanted.
	 */
	private int seed = random.nextInt();
	private int powerLimit = MAX_POWER;
	private int availablePower;
	/**
	 * 0 to 2: which of the three vanilla options to use.
	 */
	private int option;
	private int powerCheckCountdown;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_POWER_LIMIT -> powerLimit;
				case DATA_AVAILABLE_POWER -> availablePower;
				default -> option;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return 3;
		}
	};

	/**
	 * Only on the client: vanilla book animation state, drawn by the vanilla renderer.
	 */
	@Nullable
	private EnchantingTableBlockEntity book;

	public AutoEnchantmentTableBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.AUTO_ENCHANTMENT_TABLE.get(), pos, state);
		final Map<Integer, AutoSlot> inputs = Map.of(SLOT_TOOL, AutoSlot.TOOL, SLOT_LAPIS, AutoSlot.LAPIS);
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

	public EnchantingTableBlockEntity getBook() {
		if (book == null) book = new EnchantingTableBlockEntity(worldPosition, Blocks.ENCHANTING_TABLE.defaultBlockState());
		return book;
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		if (side == null) return itemHandlers[6];
		final boolean open = config.isOpen(AutoSlot.TOOL, side) || config.isOpen(AutoSlot.LAPIS, side) || config.isOpen(AutoSlot.OUTPUT, side);
		return open? itemHandlers[side.get3DDataValue()] : null;
	}

	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side == null || config.isOpen(AutoSlot.XP, side)? fluidHandler : null;
	}

	public void setPowerLimit(int powerLimit) {
		this.powerLimit = Mth.clamp(powerLimit, 1, MAX_POWER);
		setChanged();
	}

	public void setOption(int option) {
		this.option = Mth.clamp(option, 0, 2);
		setChanged();
	}

	private void onConfigChanged() {
		setChanged();
		if (level != null) level.invalidateCapabilities(worldPosition);
		sendUpdate();
	}

	private void sendUpdate() {
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, AutoEnchantmentTableBlockEntity table) {
		EnchantingTableBlockEntity.bookAnimationTick(level, pos, state, table.getBook());
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AutoEnchantmentTableBlockEntity table) {
		final AutoSlotConfig<AutoSlot> config = table.config;
		if (config.isAuto(AutoSlot.XP) && table.tank.getSpace() > 0)
			MachineTransfer.pullFluid(level, pos, config.getSides(AutoSlot.XP), table.tank, PULL_PER_TICK);

		if (table.powerCheckCountdown-- <= 0) {
			table.powerCheckCountdown = POWER_CHECK_PERIOD;
			table.availablePower = getPower(level, pos);
		}

		if (config.isAuto(AutoSlot.OUTPUT))
			MachineTransfer.pushItem(level, pos, config.getSides(AutoSlot.OUTPUT), table.inventory, SLOT_OUTPUT);
		if (config.isAuto(AutoSlot.TOOL))
			MachineTransfer.pullItem(level, pos, config.getSides(AutoSlot.TOOL), table.inventory, SLOT_TOOL);
		if (config.isAuto(AutoSlot.LAPIS))
			MachineTransfer.pullItem(level, pos, config.getSides(AutoSlot.LAPIS), table.inventory, SLOT_LAPIS);

		table.tryEnchant(level, pos);
	}

	/**
	 * Bookshelf power, the same as for a vanilla table at this position.
	 */
	private static int getPower(Level level, BlockPos pos) {
		float power = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS)
			if (EnchantingTableBlock.isValidBookShelf(level, pos, offset))
				power += level.getBlockState(pos.offset(offset)).getEnchantPowerBonus(level, pos.offset(offset));
		return (int)power;
	}

	/**
	 * Mirrors {@code EnchantmentMenu.slotsChanged} and {@code clickMenuButton}, with the bookshelf power capped by the limit set in the GUI.
	 */
	private void tryEnchant(Level level, BlockPos pos) {
		final ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
		if (tool.isEmpty() || !tool.isEnchantable() || !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;

		final int lapisCost = option + 1;
		if (inventory.getStackInSlot(SLOT_LAPIS).getCount() < lapisCost) return;

		final int power = Math.min(availablePower, powerLimit);
		// costs of later options depend on the earlier ones, since they share the random sequence
		random.setSeed(seed);
		int levelRequirement = 0;
		for (int i = 0; i <= option; i++) {
			levelRequirement = EnchantmentHelper.getEnchantmentCost(random, i, power, tool);
			if (levelRequirement < i + 1) levelRequirement = 0;
			levelRequirement = EventHooks.onEnchantmentLevelSet(level, pos, i, power, tool, levelRequirement);
		}
		if (levelRequirement <= 0) return;

		// like a player at the required level who pays the option's levels
		final int availableXp = FluidXpUtils.fluidToXp(tank.getFluidAmount());
		if (XpUtils.getLevelForExperience(availableXp) < levelRequirement) return;
		final int xpCost = XpUtils.getExperienceForLevel(levelRequirement) - XpUtils.getExperienceForLevel(levelRequirement - lapisCost);

		final List<EnchantmentInstance> enchantments = getEnchantmentList(level, tool, levelRequirement);
		if (enchantments.isEmpty()) return;

		inventory.setStackInSlot(SLOT_OUTPUT, tool.getItem().applyEnchantments(tool, enchantments));
		inventory.setStackInSlot(SLOT_TOOL, ItemStack.EMPTY);
		inventory.extractItem(SLOT_LAPIS, lapisCost, false);
		tank.drain(FluidXpUtils.xpToFluid(xpCost), IFluidHandler.FluidAction.EXECUTE);
		seed = level.random.nextInt();
		level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.1f + 0.9f);
	}

	private List<EnchantmentInstance> getEnchantmentList(Level level, ItemStack stack, int cost) {
		random.setSeed(seed + option);
		final Optional<HolderSet.Named<Enchantment>> enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
		if (enchantments.isEmpty()) return List.of();
		final List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(random, stack, cost, enchantments.get().stream());
		if (stack.is(Items.BOOK) && list.size() > 1) list.remove(random.nextInt(list.size()));
		return list;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.autoenchantmenttable");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new AutoEnchantmentTableMenu(containerId, playerInventory, this, data);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag.getCompound("tank"));
		// update packets don't carry the inventory; the open menu uses it on the client, so it must not be cleared
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		if (tag.contains("seed")) seed = tag.getInt("seed");
		if (tag.contains("power_limit")) powerLimit = Mth.clamp(tag.getInt("power_limit"), 1, MAX_POWER);
		option = Mth.clamp(tag.getInt("option"), 0, 2);
		config.load(tag);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		tag.putInt("seed", seed);
		tag.putInt("power_limit", powerLimit);
		tag.putInt("option", option);
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
