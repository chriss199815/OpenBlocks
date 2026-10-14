package openblocks.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import openblocks.entity.Luggage;
import openblocks.registry.OBComponents;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code ItemLuggage}: right-click places the luggage two blocks in front of the player, with the
 * contents, size and name stored on the item.
 */
public class LuggageItem extends Item {

	public LuggageItem(Properties properties) {
		super(properties);
	}

	public static boolean isSpecial(ItemStack stack) {
		return stack.has(OBComponents.LUGGAGE_SPECIAL.get());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isSpecial(stack) || super.isFoil(stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

		if (!level.isClientSide) {
			final Luggage luggage = OBEntities.LUGGAGE.get().create(level);
			if (luggage != null) {
				final Vec3 pos = player.position().add(player.getLookAngle().scale(2));
				luggage.moveTo(pos.x, player.getY(), pos.z, 0, 0);
				luggage.setOwnerUUID(player.getUUID());
				luggage.restoreFromItem(stack);
				level.addFreshEntity(luggage);
				stack.shrink(1);
			}
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	/**
	 * Port of legacy {@code LuggageDropHandler}: dropped luggage with contents neither despawns nor burns.
	 */
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof ItemEntity item && item.getItem().getItem() instanceof LuggageItem && item.getItem().has(DataComponents.CONTAINER)) {
			item.setInvulnerable(true);
			item.setUnlimitedLifetime();
		}
	}
}
