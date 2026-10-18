package openblocks.common;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import openblocks.registry.OBEnchantments;
import openblocks.util.XpUtils;

/**
 * Port of legacy {@code LastStandEnchantmentsHandler}: damage that would kill a player wearing last stand armor is
 * paid with experience instead, leaving the player at half a heart. The cost is the default of the legacy formula
 * {@code max(1, 50 * (1 - (hp - dmg)) / ench)}, with the levels of all worn pieces added up (the configurable formula is
 * gone).
 * <p>
 * Unlike legacy, the damage is checked after armor and absorption, so only damage that would really kill counts.
 */
public class LastStandEnchantmentHandler {

	public static void onDamage(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

		final int enchantmentLevels = OBEnchantments.getArmorLevels(player, OBEnchantments.LAST_STAND);
		if (enchantmentLevels <= 0) return;

		final float health = player.getHealth() + player.getAbsorptionAmount();
		final float healthAvailable = health - event.getNewDamage();
		if (healthAvailable >= 1) return;

		final int xpAvailable = XpUtils.getPlayerXp(player);
		final int xpRequired = (int)Math.max(1, 50 * (1 - healthAvailable) / enchantmentLevels);
		if (xpAvailable < xpRequired) return;

		player.giveExperiencePoints(-xpRequired);
		event.setNewDamage(Math.max(0, health - 1));
	}
}
