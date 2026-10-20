package openblocks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.integration.patchouli.OBPatchouli;

/**
 * Port of legacy {@code ItemInfoBook}: opens the manual, which needs Patchouli. Always registered, so the item survives
 * removing Patchouli; then it only says so.
 */
public class InfoBookItem extends Item {

	public InfoBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			if (OBPatchouli.isLoaded()) OBPatchouli.openBook(serverPlayer);
			else player.displayClientMessage(Component.translatable("openblocks.misc.needs_patchouli"), true);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
