package openblocks.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.common.HangGliderHandler;

/**
 * Right-click puts the glider on the back (or takes it off); it opens as soon as the player is in the air.
 */
public class HangGliderItem extends Item {

	public HangGliderItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide) HangGliderHandler.toggle(player, hand);
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}
}
