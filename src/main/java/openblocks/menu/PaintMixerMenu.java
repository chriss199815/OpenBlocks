package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.PaintMixerBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerPaintMixer}: paint slot, four dye slots, with the legacy layout.
 * The chosen color is sent as the menu button id (the color itself, 24 bits fit into the packet's var int).
 */
public class PaintMixerMenu extends MachineMenu {

	public static final int PAINT_X = 133;
	public static final int PAINT_Y = 22;
	public static final int DYES_X = 123;
	public static final int DYES_Y = 76;
	public static final int PLAYER_INVENTORY_Y = 120;

	private final PaintMixerBlockEntity mixer;
	private final ContainerLevelAccess access;
	private final ContainerData data;

	public PaintMixerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (PaintMixerBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
				new SimpleContainerData(PaintMixerBlockEntity.DATA_COUNT));
	}

	public PaintMixerMenu(int containerId, Inventory playerInventory, PaintMixerBlockEntity mixer, ContainerData data) {
		super(OBMenus.PAINT_MIXER.get(), containerId);
		this.mixer = mixer;
		this.access = ContainerLevelAccess.create(mixer.getLevel(), mixer.getBlockPos());
		this.data = data;

		addSlot(new SlotItemHandler(mixer.getInventory(), PaintMixerBlockEntity.SLOT_PAINT, PAINT_X, PAINT_Y));
		for (int i = 0; i < 4; i++)
			addSlot(new SlotItemHandler(mixer.getInventory(), PaintMixerBlockEntity.SLOT_CYAN + i, DYES_X + (i % 2) * 20, DYES_Y + (i / 2) * 20));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
		addDataSlots(data);
	}

	public int getProgress() {
		return data.get(PaintMixerBlockEntity.DATA_PROGRESS);
	}

	public int getColor() {
		return (data.get(PaintMixerBlockEntity.DATA_COLOR_HIGH) << 12) | data.get(PaintMixerBlockEntity.DATA_COLOR_LOW);
	}

	public float getInk(int ink) {
		return data.get(PaintMixerBlockEntity.DATA_INK + ink) / 100f;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		mixer.startMixing(id);
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.PAINT_MIXER.get());
	}
}
