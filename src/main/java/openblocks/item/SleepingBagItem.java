package openblocks.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.common.SleepingBagHandler;

/**
 * Right-click lets the player sleep where they stand. Unlike legacy it isn't worn in the chest slot, so armor stays on.
 */
public class SleepingBagItem extends Item {

	public SleepingBagItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
		if (player instanceof ServerPlayer serverPlayer) SleepingBagHandler.trySleep(serverPlayer);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
