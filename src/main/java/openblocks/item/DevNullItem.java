package openblocks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import openblocks.OBConfig;
import openblocks.menu.DevNullMenu;

/**
 * Port of legacy {@code ItemDevNull}: a single slot (stored as {@link DataComponents#CONTAINER}) that swallows
 * matching items on pickup and voids them once full. Placing a block from the slot works like holding it.
 * /dev/nulls can be nested up to {@link #STACK_LIMIT} deep.
 */
public class DevNullItem extends Item {

	public static final int STACK_LIMIT = 5;

	private static final String BOX_START = "╔";
	private static final String BOX_MIDDLE = "║";
	private static final String BOX_END = "╚";

	/**
	 * The innermost item that is not a /dev/null, and how many /dev/nulls wrap it. Past {@link #STACK_LIMIT} the stack is empty.
	 */
	public record Contents(ItemStack stack, int depth) {}

	public DevNullItem(Properties properties) {
		super(properties);
	}

	public static ItemStack getContained(ItemStack container) {
		final ItemContainerContents contents = container.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		return contents.getSlots() > 0? contents.getStackInSlot(0) : ItemStack.EMPTY;
	}

	public static void setContained(ItemStack container, ItemStack contained) {
		container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contained)));
	}

	public static Contents getContents(ItemStack container) {
		ItemStack stack = container;
		int depth = 0;
		while (depth < STACK_LIMIT) {
			if (stack.isEmpty() || !(stack.getItem() instanceof DevNullItem)) return new Contents(stack, depth);
			stack = getContained(stack);
			depth++;
		}
		return new Contents(ItemStack.EMPTY, depth);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

		if (!level.isClientSide && (!OBConfig.DEV_NULL.sneakClickToGui.get() || player.isShiftKeyDown())) {
			final int slot = player.getInventory().selected;
			player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> new DevNullMenu(containerId, inventory, slot), stack.getHoverName()),
					buffer -> buffer.writeVarInt(slot));
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	/**
	 * Places a contained block as if the player held it.
	 */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Player player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;

		final InteractionHand hand = context.getHand();
		final ItemStack container = player.getItemInHand(hand);
		final ItemStack contained = getContained(container);
		if (!(contained.getItem() instanceof BlockItem)) return InteractionResult.PASS;

		player.setItemInHand(hand, contained);
		final BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
		final InteractionResult result = contained.useOn(new UseOnContext(player, hand, hit));
		setContained(container, player.getItemInHand(hand));
		player.setItemInHand(hand, container);
		return result;
	}

	/**
	 * Fills /dev/nulls holding the same item; once one matches, whatever does not fit is voided.
	 */
	public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
		final ItemEntity itemEntity = event.getItemEntity();
		final Player player = event.getPlayer();
		final ItemStack picked = itemEntity.getItem();
		if (picked.isEmpty() || itemEntity.hasPickUpDelay() || itemEntity.getTarget() != null && !itemEntity.getTarget().equals(player.getUUID())) return;

		final int count = picked.getCount();
		boolean foundMatchingContainer = false;
		final Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize() && !picked.isEmpty(); i++) {
			final ItemStack container = inventory.getItem(i);
			if (!(container.getItem() instanceof DevNullItem)) continue;

			final ItemStack contained = getContained(container);
			if (contained.isEmpty() || !ItemStack.isSameItemSameComponents(picked, contained)) continue;

			final int total = contained.getCount() + picked.getCount();
			final int kept = Math.min(contained.getMaxStackSize(), total);
			contained.setCount(kept);
			picked.setCount(total - kept);
			setContained(container, contained);
			foundMatchingContainer = true;
		}

		if (foundMatchingContainer) {
			// pickup animation and sound, like the legacy pickup of the emptied stack
			player.take(itemEntity, count);
			picked.setCount(0);
			itemEntity.discard();
			event.setCanPickup(TriState.FALSE);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		final Contents contents = getContents(stack);
		final ItemStack contained = contents.stack();
		if (!contained.isEmpty()) {
			final List<Component> innerTooltip = contained.getTooltipLines(context, null, flag);
			if (innerTooltip.isEmpty()) {
				tooltip.add(Component.literal(contained.getCount() + " * ").append(contained.getHoverName()));
			} else {
				tooltip.add(Component.literal(BOX_START));
				tooltip.add(Component.literal(BOX_MIDDLE + contained.getCount() + " * ").append(innerTooltip.get(0)));
				for (Component line : innerTooltip.subList(1, innerTooltip.size()))
					tooltip.add(Component.literal(BOX_MIDDLE).append(line));
				tooltip.add(Component.literal(BOX_END));
			}
		} else if (contents.depth() >= STACK_LIMIT) {
			tooltip.add(Component.literal(BOX_START));
			tooltip.add(Component.literal(BOX_MIDDLE).append(Component.literal("WHOOPS").withStyle(ChatFormatting.OBFUSCATED)));
			tooltip.add(Component.literal(BOX_END));
		}
	}
}
