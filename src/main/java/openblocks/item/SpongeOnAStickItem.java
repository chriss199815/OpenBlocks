package openblocks.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import openblocks.OBConfig;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code ItemSpongeOnAStick}: removes liquid blocks around the clicked block (or around the player, when
 * clicking into the air), one durability point per use. Soaking up lava burns the sponge and sets the player on fire.
 * Unlike legacy, flowing liquid left at the edge is updated, so it doesn't stay behind without its source.
 */
public class SpongeOnAStickItem extends Item {

	public static final int DEFAULT_DURABILITY = 256;

	public SpongeOnAStickItem(Properties properties) {
		super(properties);
	}

	/**
	 * Items are registered before the common config is loaded, so the configured durability replaces the default afterwards.
	 */
	public static void setDurability(ModifyDefaultComponentsEvent event) {
		// not loaded during data generation
		if (!OBConfig.SPEC.isLoaded()) return;
		event.modify(OBItems.SPONGE_ON_A_STICK.get(), builder -> builder.set(DataComponents.MAX_DAMAGE, OBConfig.SPONGE.stickUseCount.getAsInt()));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Player player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;
		return soakUp(context.getLevel(), context.getClickedPos(), player, context.getHand())? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : InteractionResult.FAIL;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		return soakUp(level, player.blockPosition(), player, hand)? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : InteractionResultHolder.fail(stack);
	}

	private static boolean soakUp(Level level, BlockPos pos, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		final int flags = OBConfig.SPONGE.stickBlockUpdate.getAsBoolean()? Block.UPDATE_ALL : Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		final int range = OBConfig.SPONGE.stickRange.getAsInt();
		int damage = stack.getDamageValue();
		boolean absorbedAnything = false;
		boolean hitLava = false;

		for (BlockPos target : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
			final BlockState state = level.getBlockState(target);
			if (!(state.getBlock() instanceof LiquidBlock)) continue;

			absorbedAnything = true;
			hitLava |= state.getFluidState().is(FluidTags.LAVA);
			if (!level.isClientSide) level.setBlock(target, Blocks.AIR.defaultBlockState(), flags);
			if (++damage >= stack.getMaxDamage()) break;
		}

		if (!absorbedAnything) return false;
		if (level.isClientSide) return true;

		if (!OBConfig.SPONGE.stickBlockUpdate.getAsBoolean()) updateFlowingBorder(level, pos, range);

		if (hitLava) {
			stack.shrink(1);
			player.igniteForSeconds(6);
		} else {
			stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		}
		return true;
	}

	/**
	 * Without block updates, flowing liquid just outside the cleared cube would stay forever once its source is gone.
	 * Ticking it lets it dry up, or flow back in from sources that are left (sources themselves stay in place, like legacy).
	 */
	private static void updateFlowingBorder(Level level, BlockPos pos, int range) {
		final int border = range + 1;
		for (BlockPos target : BlockPos.betweenClosed(pos.offset(-border, -border, -border), pos.offset(border, border, border))) {
			if (Math.abs(target.getX() - pos.getX()) < border && Math.abs(target.getY() - pos.getY()) < border && Math.abs(target.getZ() - pos.getZ()) < border) continue;
			final FluidState fluid = level.getFluidState(target);
			if (!fluid.isEmpty() && !fluid.isSource())
				level.scheduleTick(target, fluid.getType(), fluid.getType().getTickDelay(level));
		}
	}
}
