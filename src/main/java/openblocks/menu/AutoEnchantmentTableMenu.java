package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.AutoEnchantmentTableBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerAutoEnchantmentTable}. Power limit, available power and option are synced via {@link ContainerData},
 * the tank and side configuration via the block entity update packet.
 */
public class AutoEnchantmentTableMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 84;
	// the same icon as the vanilla table, whose constant is private
	private static final ResourceLocation EMPTY_SLOT_LAPIS_LAZULI = ResourceLocation.withDefaultNamespace("item/empty_slot_lapis_lazuli");

	private final AutoEnchantmentTableBlockEntity table;
	private final ContainerData data;
	private final ContainerLevelAccess access;
	/**
	 * Button ids below this belong to the side configuration.
	 */
	private final int optionButtons;
	private final int limitButtons;

	public AutoEnchantmentTableMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (AutoEnchantmentTableBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
	}

	public AutoEnchantmentTableMenu(int containerId, Inventory playerInventory, AutoEnchantmentTableBlockEntity table, ContainerData data) {
		super(OBMenus.AUTO_ENCHANTMENT_TABLE.get(), containerId);
		this.table = table;
		this.data = data;
		this.access = ContainerLevelAccess.create(table.getLevel(), table.getBlockPos());
		this.optionButtons = table.getConfig().getButtonCount();
		this.limitButtons = optionButtons + 3;

		addSlot(new SlotItemHandler(table.getInventory(), AutoEnchantmentTableBlockEntity.SLOT_TOOL, 15, 22));
		addSlot(new SlotItemHandler(table.getInventory(), AutoEnchantmentTableBlockEntity.SLOT_LAPIS, 15, 46)
				.setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_LAPIS_LAZULI));
		addSlot(new SlotItemHandler(table.getInventory(), AutoEnchantmentTableBlockEntity.SLOT_OUTPUT, 130, 34));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
		addDataSlots(data);
	}

	public AutoEnchantmentTableBlockEntity getTable() {
		return table;
	}

	public int getPowerLimit() {
		return data.get(AutoEnchantmentTableBlockEntity.DATA_POWER_LIMIT);
	}

	public int getAvailablePower() {
		return data.get(AutoEnchantmentTableBlockEntity.DATA_AVAILABLE_POWER);
	}

	public int getOption() {
		return data.get(AutoEnchantmentTableBlockEntity.DATA_OPTION);
	}

	/**
	 * Button ids for the vanilla container button packet; replace the legacy RPC.
	 */
	public int optionButtonId(int option) {
		return optionButtons + option;
	}

	public int limitButtonId(int limit) {
		return limitButtons + limit;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (table.getConfig().handleButton(id)) return true;
		if (id >= optionButtons && id < limitButtons) table.setOption(id - optionButtons);
		else if (id >= limitButtons) table.setPowerLimit(id - limitButtons);
		else return false;
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
	}
}
