package openblocks.menu;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import openblocks.block.entity.GuideBlockEntity;
import openblocks.registry.OBMenus;
import openblocks.util.PaintColors;

/**
 * Settings of a building guide; new, legacy only had the touch buttons. Has no slots, the screen reads the synced block
 * entity and sends its buttons as menu button clicks.
 */
public class GuideMenu extends AbstractContainerMenu {

	public static final int BUTTON_NEXT_SHAPE = 0;
	public static final int BUTTON_PREVIOUS_SHAPE = 1;
	public static final int BUTTON_ROTATE_CW = 2;
	public static final int BUTTON_ROTATE_CCW = 3;
	public static final int BUTTON_REDSTONE_MODE = 4;
	// + dye color id
	public static final int BUTTON_COLOR = 10;
	// + half axis * 4 + one of the SIZE_STEPS
	public static final int BUTTON_SIZE = 100;
	public static final int[] SIZE_STEPS = { -1, +1, -10, +10 };

	private final GuideBlockEntity guide;
	private final ContainerLevelAccess access;

	public GuideMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, (GuideBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public GuideMenu(int containerId, GuideBlockEntity guide) {
		super(OBMenus.GUIDE.get(), containerId);
		this.guide = guide;
		this.access = ContainerLevelAccess.create(guide.getLevel(), guide.getBlockPos());
	}

	public GuideBlockEntity getGuide() {
		return guide;
	}

	public static int sizeButton(Direction halfAxis, int step) {
		return BUTTON_SIZE + halfAxis.ordinal() * SIZE_STEPS.length + step;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id >= BUTTON_SIZE && id < BUTTON_SIZE + Direction.values().length * SIZE_STEPS.length) {
			final int index = id - BUTTON_SIZE;
			guide.adjustSize(Direction.values()[index / SIZE_STEPS.length], SIZE_STEPS[index % SIZE_STEPS.length]);
			return true;
		}
		if (id >= BUTTON_COLOR && id < BUTTON_COLOR + DyeColor.values().length) {
			guide.setSettings(guide.getSettings().withColor(PaintColors.rgb(DyeColor.byId(id - BUTTON_COLOR))));
			return true;
		}
		switch (id) {
			case BUTTON_NEXT_SHAPE -> guide.setSettings(guide.getSettings().withShape(guide.getSettings().shape().next()));
			case BUTTON_PREVIOUS_SHAPE -> guide.setSettings(guide.getSettings().withShape(guide.getSettings().shape().previous()));
			case BUTTON_ROTATE_CW -> guide.rotateClockwise();
			case BUTTON_ROTATE_CCW -> guide.rotateCounterClockwise();
			case BUTTON_REDSTONE_MODE -> guide.setSettings(guide.getSettings().withRedstoneMode(guide.getSettings().redstoneMode().next()));
			default -> {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, guide.getBlockState().getBlock());
	}
}
