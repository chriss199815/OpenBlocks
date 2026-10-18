package openblocks.common;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import openblocks.OBConfig;
import openblocks.registry.OBEnchantments;

/**
 * Port of legacy {@code ExplosiveEnchantmentsHandler} ("Unstable"). Boots: a fall of more than 4 blocks (not while
 * sneaking) becomes an explosion that throws the player up again, without fall damage. Other armor: when hit by an
 * arrow or a mob or player attack, an explosion around the player. Each explosion uses gunpowder from the inventory (1,
 * 2 or 4 by level) and damages the armor piece; the player is not hurt by it.
 * <p>
 * Legacy pressed the client's jump key to make the player jump; here the server sets the motion.
 */
public class ExplosiveEnchantmentHandler {

	// legacy multiplied the horizontal motion by this much more than the vertical one
	private static final double HORIZONTAL_FACTOR = 5;

	// vanilla jump motion
	private static final double JUMP_MOTION = 0.42;

	private static final List<EquipmentSlot> UPPER_ARMOR = List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD);

	private record Tier(double multiplier, double maxHeightBonus, float jumpExplosionPower, float armorExplosionPower, boolean destructive, int gunpowderNeeded) {

		double calculateMultiplier(float height) {
			return multiplier + Math.min(Math.sqrt(height), maxHeightBonus);
		}

		boolean isDestructive() {
			return destructive && OBConfig.ENCHANTMENTS.explosiveEnchantGrief.get();
		}
	}

	private static final Tier[] TIERS = {
			new Tier(0.10, 5, 5, 1, false, 1),
			new Tier(0.75, 7.5, 10, 2, false, 2),
			new Tier(1.00, 10, 5, 4, true, 4)
	};

	private static void explode(Player player, float power, boolean destructive) {
		player.level().explode(player, player.getX(), player.getBoundingBox().minY, player.getZ(), power,
				destructive? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
	}

	@Nullable
	private static Tier tryUseEnchantment(Player player, EquipmentSlot slot) {
		final ItemStack armor = player.getItemBySlot(slot);
		if (armor.isEmpty() || !(armor.getItem() instanceof ArmorItem)) return null;

		final int explosiveLevel = OBEnchantments.getLevel(player.level(), OBEnchantments.EXPLOSIVE, armor);
		if (explosiveLevel <= 0 || explosiveLevel > TIERS.length) return null;
		final Tier level = TIERS[explosiveLevel - 1];

		for (ItemStack stack : player.getInventory().items) {
			if (stack.is(Items.GUNPOWDER) && stack.getCount() >= level.gunpowderNeeded()) {
				if (!player.getAbilities().instabuild) {
					armor.hurtAndBreak(1, player, slot);
					stack.shrink(level.gunpowderNeeded());
				}
				return level;
			}
		}

		return null;
	}

	public static void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
		if (event.getDistance() <= 4 || player.isShiftKeyDown()) return;

		final Tier level = tryUseEnchantment(player, EquipmentSlot.FEET);
		if (level == null) return;

		explode(player, level.jumpExplosionPower(), level.isDestructive());
		final double multiplier = level.calculateMultiplier(event.getDistance());
		final Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x * multiplier * HORIZONTAL_FACTOR, JUMP_MOTION * multiplier, motion.z * multiplier * HORIZONTAL_FACTOR);
		player.hurtMarked = true;
		player.resetFallDistance();
		event.setCanceled(true);
	}

	// legacy: damage types "arrow", "player" and "mob" from an entity
	private static boolean checkSource(DamageSource source) {
		return source.getEntity() != null && (source.is(DamageTypes.ARROW) || source.is(DamageTypes.PLAYER_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
	}

	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !checkSource(event.getSource())) return;

		for (EquipmentSlot slot : UPPER_ARMOR) {
			final Tier level = tryUseEnchantment(player, slot);
			if (level != null) {
				explode(player, level.armorExplosionPower(), level.isDestructive());
				return;
			}
		}
	}
}
