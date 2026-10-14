package openblocks.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import openblocks.OBConfig;
import openblocks.item.TrophyItem;
import openblocks.registry.OBBlocks;

/**
 * Port of legacy {@code TrophyHandler.onLivingDrops}: mobs killed by a player sometimes drop their trophy.
 */
public class TrophyDropHandler {

	public static void onLivingDrops(LivingDropsEvent event) {
		final LivingEntity entity = event.getEntity();
		final Level level = entity.level();
		if (level.isClientSide || !event.isRecentlyHit() || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

		final TrophyType type = TrophyType.forEntity(entity.getType());
		if (type == null) return;

		final int looting = event.getSource().getEntity() instanceof LivingEntity killer
				? EnchantmentHelper.getEnchantmentLevel(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING), killer)
				: 0;
		if (!shouldDrop(level.random, looting, OBConfig.TROPHY.dropChance.get())) return;

		final ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), TrophyItem.create(OBBlocks.TROPHY.get(), type));
		drop.setDefaultPickUpDelay();
		event.getDrops().add(drop);
	}

	/**
	 * The default legacy {@code trophyDropChanceFormula}: {@code (looting + rand()/4) * chance - rand() > 0}.
	 */
	public static boolean shouldDrop(RandomSource random, int looting, double chance) {
		final double bias = random.nextDouble() / 4;
		final double selection = random.nextDouble();
		return (looting + bias) * chance - selection > 0;
	}
}
