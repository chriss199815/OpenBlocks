package openblocks.item;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.common.PedometerState;
import openblocks.registry.OBAttachments;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code ItemPedometer}: right click starts tracking, then shows distances and speeds in chat; sneaking
 * resets it. Tracking runs on the client while the pedometer is in the hotbar, like legacy.
 */
public class PedometerItem extends Item {

	public PedometerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide) {
			final PedometerState state = player.getData(OBAttachments.PEDOMETER);
			if (player.isSecondaryUseActive()) {
				state.reset();
				send(player, "openblocks.misc.pedometer.tracking_reset");
			} else if (state.isRunning()) {
				showData(player, state);
			} else {
				state.start(player);
				send(player, "openblocks.misc.pedometer.tracking_started");
			}
		} else {
			level.playSound(null, player.blockPosition(), OBSounds.PEDOMETER_USE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	private static void send(Player player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), false);
	}

	private static String speed(double metersPerTick) {
		return String.format(Locale.ROOT, "%.2f m/tick", metersPerTick);
	}

	private static String distance(double meters) {
		return String.format(Locale.ROOT, "%.0f m", meters);
	}

	private static void showData(Player player, PedometerState state) {
		final PedometerState.Data result = state.check();
		if (result == null) return;
		player.displayClientMessage(Component.empty(), false);
		send(player, "openblocks.misc.pedometer.start_point", String.format(Locale.ROOT, "%.1f %.1f %.1f", result.startingPoint().x, result.startingPoint().y, result.startingPoint().z));
		send(player, "openblocks.misc.pedometer.speed", speed(result.currentSpeed()));
		send(player, "openblocks.misc.pedometer.avg_speed", speed(result.averageSpeed()));
		send(player, "openblocks.misc.pedometer.total_distance", distance(result.totalDistance()));
		send(player, "openblocks.misc.pedometer.straght_line_distance", distance(result.straightLineDistance()));
		send(player, "openblocks.misc.pedometer.straigh_line_speed", speed(result.straightLineSpeed()));
		send(player, "openblocks.misc.pedometer.last_check_speed", speed(result.lastCheckSpeed()));
		send(player, "openblocks.misc.pedometer.last_check_distance", distance(result.lastCheckDistance()));
		send(player, "openblocks.misc.pedometer.last_check_time", result.lastCheckTime());
		send(player, "openblocks.misc.pedometer.total_time", result.totalTime());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide && slot < 9 && entity instanceof Player player) {
			final PedometerState state = player.getData(OBAttachments.PEDOMETER);
			if (state.isRunning()) state.update(player);
		}
	}
}
