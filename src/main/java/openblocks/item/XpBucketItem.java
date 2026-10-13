package openblocks.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import openblocks.common.FluidXpUtils;

/**
 * Bucket of liquid XP. Right-clicking a block places the fluid as usual; right-clicking into the air turns the contents back into XP.
 */
public class XpBucketItem extends BucketItem {

	public XpBucketItem(Fluid content, Properties properties) {
		super(content, properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
		// PASS means no block was targeted
		if (result.getResult() != InteractionResult.PASS) return result;

		final ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide) player.giveExperiencePoints(FluidXpUtils.fluidToXp(FluidType.BUCKET_VOLUME));
		level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 0.8f + level.random.nextFloat() * 0.4f);
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player)), level.isClientSide);
	}
}
