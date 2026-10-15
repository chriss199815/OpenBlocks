package openblocks.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import openblocks.block.entity.CannonBlockEntity;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code MetaPointer}: sneak-using it on a cannon selects the cannon, sneak-using it on any other block
 * aims the selected cannon at that block. Unlike legacy (10 blocks), it reaches 64 blocks.
 */
public class PointerItem extends Item {

	public static final double RANGE = 64;

	public PointerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);

		if (!level.isClientSide && player.pick(RANGE, 1.0f, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			final BlockPos pos = hit.getBlockPos();
			if (level.getBlockEntity(pos) instanceof CannonBlockEntity) {
				stack.set(OBComponents.POINTER_TARGET.get(), GlobalPos.of(level.dimension(), pos));
				player.sendSystemMessage(Component.translatable("openblocks.misc.selected_cannon"));
			} else {
				final GlobalPos cannonPos = stack.get(OBComponents.POINTER_TARGET.get());
				if (cannonPos != null && cannonPos.dimension() == level.dimension() && level.isLoaded(cannonPos.pos())
						&& level.getBlockEntity(cannonPos.pos()) instanceof CannonBlockEntity cannon) {
					cannon.setTarget(pos);
					player.sendSystemMessage(Component.translatable("openblocks.misc.pointed_cannon", pos.getX(), pos.getY(), pos.getZ()));
				}
			}
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	public static boolean isPointer(ItemStack stack) {
		return stack.getItem() instanceof PointerItem;
	}
}
