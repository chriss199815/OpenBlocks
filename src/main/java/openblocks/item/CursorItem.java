package openblocks.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import openblocks.OBConfig;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code ItemCursor}: clicking a block links the cursor to it, clicking the air clicks the linked block
 * remotely. Unlike legacy, which charged XP growing with the squared distance, every remote click costs one point of
 * durability.
 */
public class CursorItem extends Item {

	public static final int DURABILITY = 100;

	public CursorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Level level = context.getLevel();
		context.getItemInHand().set(OBComponents.CURSOR_TARGET.get(), new CursorTarget(GlobalPos.of(level.dimension(), context.getClickedPos()), context.getClickedFace()));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

		final CursorTarget target = stack.get(OBComponents.CURSOR_TARGET.get());
		if (!level.isClientSide && target != null) {
			final BlockPos pos = target.pos().pos();
			if (target.pos().dimension() == level.dimension() && level.isLoaded(pos)) clickBlock(level, player, hand, stack, target);
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	private static void clickBlock(Level level, Player player, InteractionHand hand, ItemStack stack, CursorTarget target) {
		final BlockPos pos = target.pos().pos();
		final BlockState state = level.getBlockState(pos);
		if (state.isAir()) return;

		if (!isInReach(player, pos)) return;

		final Vec3 hitLocation = Vec3.atCenterOf(pos).relative(target.side(), 0.5);
		final BlockHitResult hit = new BlockHitResult(hitLocation, target.side(), pos, false);
		// the cursor itself has no use on blocks, so only the block reacts, as if clicked with an empty hand
		final ItemInteractionResult itemResult = state.useItemOn(stack, level, player, hand, hit);
		final boolean used = itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
				? state.useWithoutItem(level, player, hit).consumesAction()
				: itemResult.consumesAction();
		if (used) {
			player.swing(hand, true);
			// free in creative mode
			stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		}
	}

	public static boolean isInReach(Player player, BlockPos pos) {
		// like legacy, the limit is compared with the squared distance
		return player.distanceToSqr(Vec3.atCenterOf(pos)) < OBConfig.CURSOR.maxDistance.get();
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
		return repairCandidate.is(ItemTags.WOOL);
	}
}
