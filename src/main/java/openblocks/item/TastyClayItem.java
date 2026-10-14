package openblocks.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.common.BrickDrops;

/**
 * Port of legacy {@code ItemTastyClay}: food that is always edible and fills the bowels for brick drops.
 */
public class TastyClayItem extends Item {

	public TastyClayItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide && entity instanceof Player player) BrickDrops.onEatTastyClay(player);
		return super.finishUsingItem(stack, level, entity);
	}
}
