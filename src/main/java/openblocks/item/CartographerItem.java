package openblocks.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.entity.Cartographer;

/**
 * Port of legacy {@code ItemCartographer}: using it releases the cartographer, with the map it carried when it was
 * picked up. Legacy item damage for other assistant types is gone; there only ever was the cartographer.
 */
public class CartographerItem extends Item {

	public CartographerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

		if (!level.isClientSide) level.addFreshEntity(new Cartographer(level, player, stack));
		stack.consume(1, player);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
