package openblocks.common.flimflam;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import openblocks.OBConfig;
import openblocks.api.FlimFlamRegistry;
import openblocks.registry.OBEnchantments;
import org.slf4j.Logger;

/**
 * Port of legacy {@code FlimFlamEnchantmentsHandler}. When a player hits another, the flim flam levels of the
 * attacker's weapon are compared with a third of the victim's flim flam armor; whoever is worse off loses luck (a d20
 * per level of difference). Players with bad luck get a random flim flam from time to time, which costs luck and so
 * makes it better again.
 */
public class FlimFlamHandler {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final int LUCK_MARGIN = -30;

	// 15 s
	public static final int EFFECT_DELAY = 20 * 15;

	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof Player target) || target.level().isClientSide) return;
		if (!(event.getSource().getEntity() instanceof Player source) || source == target) return;

		final int sourceFlimFlam = OBEnchantments.getLevel(source.level(), OBEnchantments.FLIM_FLAM, source.getMainHandItem());
		final int targetFlimFlam = OBEnchantments.getArmorLevels(target, OBEnchantments.FLIM_FLAM);

		// armor is less effective, since we can have more levels
		final int flimFlamDiff = targetFlimFlam / 3 - sourceFlimFlam;
		if (flimFlamDiff == 0) return;

		// a positive difference means the target is better protected
		final Player flimFlamTarget = flimFlamDiff > 0? source : target;
		final LuckState luck = LuckState.of(flimFlamTarget);
		final RandomSource random = flimFlamTarget.getRandom();
		for (int i = 0; i < Math.abs(flimFlamDiff); i++) {
			final int roll = random.nextInt(20) + 1;
			// critical
			if (roll == 20) luck.forceNext = true;
			luck.luck -= roll;
		}

		if (luck.luck < LUCK_MARGIN) luck.forceNext = true;
	}

	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (event.getEntity() instanceof ServerPlayer player) deliverKarma(player);
	}

	private static void deliverKarma(ServerPlayer player) {
		if (!player.isAlive()) return;
		final LuckState luck = LuckState.of(player);
		if (!canFlimFlam(luck, player.getRandom())) return;

		final List<FlimFlamRegistry.Entry> selected = new ArrayList<>();
		int totalWeight = 0;
		for (FlimFlamRegistry.Entry entry : FlimFlamRegistry.getFlimFlams())
			if (entry.canApply(luck.luck) && isAllowed(entry)) {
				selected.add(entry);
				totalWeight += entry.weight();
			}

		Collections.shuffle(selected);

		while (!selected.isEmpty()) {
			final int selectedWeight = player.getRandom().nextInt(totalWeight);
			int currentWeight = 0;
			final Iterator<FlimFlamRegistry.Entry> it = selected.iterator();
			while (it.hasNext()) {
				final FlimFlamRegistry.Entry entry = it.next();
				currentWeight += entry.weight();
				if (selectedWeight <= currentWeight) {
					if (execute(player, entry)) {
						luck.luck -= entry.cost();
						LOGGER.debug("Player {} flim-flammed with {}, current luck: {}", player.getName().getString(), entry.name(), luck.luck);
						if (!entry.silent()) player.sendSystemMessage(Component.translatable("openblocks.flim_flammed"));
						return;
					}
					totalWeight -= entry.weight();
					it.remove();
					break;
				}
			}
		}
	}

	public static boolean execute(ServerPlayer player, FlimFlamRegistry.Entry entry) {
		try {
			return entry.action().execute(player);
		} catch (RuntimeException e) {
			LOGGER.warn("Error during flimflam '{}' execution", entry.name(), e);
			return false;
		}
	}

	private static boolean canFlimFlam(LuckState luck, RandomSource random) {
		if (luck.forceNext) {
			luck.forceNext = false;
			luck.cooldown = EFFECT_DELAY;
			return true;
		}

		if (luck.luck > -LUCK_MARGIN || luck.cooldown-- > 0) return false;
		luck.cooldown = EFFECT_DELAY;
		final double probability = 0.75 * 2.0 * Math.abs(Math.atan(luck.luck / 250.0) / Math.PI);
		return random.nextDouble() < probability;
	}

	/**
	 * Port of legacy {@code FlimFlamChecker}: config {@code safeOnly} and the black/white list.
	 */
	public static boolean isAllowed(FlimFlamRegistry.Entry entry) {
		if (OBConfig.TOMFOOLERY.safeFlimFlams.get() && !entry.safe()) return false;
		final boolean onList = OBConfig.TOMFOOLERY.flimFlamList.get().contains(entry.name());
		return OBConfig.TOMFOOLERY.flimFlamWhitelist.get() == onList;
	}
}
