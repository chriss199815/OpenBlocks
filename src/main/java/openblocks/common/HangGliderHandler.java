package openblocks.common;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import openblocks.OBConfig;
import openblocks.item.HangGliderItem;
import openblocks.registry.OBAttachments;

/**
 * Port of the physics from legacy {@code EntityHangGlider}. Runs on both sides: the client moves its own player, the server only needs
 * the reset fall distance.
 */
public class HangGliderHandler {

	private static final int THERMAL_HEIGHT_MIN = 70;
	private static final int THERMAL_HEIGHT_OPT = 110;
	private static final int THERMAL_HEIGHT_MAX = 136;
	private static final int THERMAL_STRONG_BONUS_HEIGHT = 100;

	private static final double VSPEED_NORMAL = -0.052;
	private static final double VSPEED_FAST = -0.176;
	public static final double VSPEED_MIN = -0.32;
	public static final double VSPEED_MAX = 0.4;

	private static final double HSPEED_NORMAL = 0.03;
	private static final double HSPEED_FAST = 0.1;

	private static int noiseDay = -1;
	private static PerlinSimplexNoise noise;

	@Nullable
	public static InteractionHand getGliderHand(Player player) {
		return player.getExistingDataOrNull(OBAttachments.HANG_GLIDER);
	}

	public static boolean hasGlider(Player player) {
		return getGliderHand(player) != null;
	}

	/**
	 * Glider is on the back and the player is in the air.
	 */
	public static boolean isDeployed(Player player) {
		return hasGlider(player) && !player.onGround() && !player.isInWater() && !player.isSleeping();
	}

	/**
	 * True for the exact stack that is on the player's back, which then isn't drawn in the hand.
	 */
	public static boolean isDeployedStack(Player player, ItemStack stack) {
		final InteractionHand hand = getGliderHand(player);
		return hand != null && player.getItemInHand(hand) == stack;
	}

	public static void toggle(Player player, InteractionHand hand) {
		final InteractionHand current = getGliderHand(player);
		if (current == null) player.setData(OBAttachments.HANG_GLIDER, hand);
		// a glider deployed from the other hand is left alone, like legacy
		else if (current == hand) player.removeData(OBAttachments.HANG_GLIDER);
	}

	private static boolean isValid(Player player, InteractionHand hand) {
		return player.isAlive()
				&& player.getItemInHand(hand).getItem() instanceof HangGliderItem
				&& !player.isFallFlying()
				&& !player.isSpectator();
	}

	public static void onPlayerTick(PlayerTickEvent.Post event) {
		tick(event.getEntity());
	}

	public static void tick(Player player) {
		final InteractionHand hand = getGliderHand(player);
		if (hand == null) return;

		if (!isValid(player, hand)) {
			// removal is synced to clients
			if (!player.level().isClientSide) player.removeData(OBAttachments.HANG_GLIDER);
			return;
		}

		if (!isDeployed(player)) return;

		// remote players are moved by their own client
		if (player.level().isClientSide && !player.isLocalPlayer()) return;

		final double thermal = OBConfig.HANG_GLIDER.enableThermal.getAsBoolean()? getThermal(player) : 0;
		final double lift = thermal >= 0? VSPEED_MAX : -VSPEED_MIN;
		final boolean fast = player.isShiftKeyDown();
		final double verticalSpeed = Math.max((fast? VSPEED_FAST : VSPEED_NORMAL) + thermal * lift, VSPEED_MIN);
		final double horizontalSpeed = fast? HSPEED_FAST : HSPEED_NORMAL;

		final Vec3 motion = player.getDeltaMovement();
		// only once the player sinks faster than the glider would, so jumping still works
		if (motion.y < verticalSpeed) {
			final double yaw = Math.toRadians(player.getYHeadRot() + 90);
			player.setDeltaMovement(motion.x + Math.cos(yaw) * horizontalSpeed, verticalSpeed, motion.z + Math.sin(yaw) * horizontalSpeed);
			player.resetFallDistance();
		}
	}

	/**
	 * Thermal lift (positive) or sink (negative) at the player's position: only in the overworld, between y 70 and 136
	 * (more for strong thermals), during the day and weakened by rain.
	 */
	private static double getThermal(Player player) {
		final Level level = player.level();
		if (level.dimension() != Level.OVERWORLD) return 0;

		double value = getNoise().getValue(player.getX() / 20, player.getZ() / 20, false) / 4;
		final boolean strong = value > 0.7;
		final int bonus = strong? THERMAL_STRONG_BONUS_HEIGHT : 0;

		value *= Math.min(Math.max(player.getY() - THERMAL_HEIGHT_MIN, 0) / (THERMAL_HEIGHT_OPT - THERMAL_HEIGHT_MIN), 1);
		value *= Math.min(Math.max(THERMAL_HEIGHT_MAX + bonus - player.getY(), 0) / (THERMAL_HEIGHT_MAX - THERMAL_HEIGHT_OPT + bonus / 4.0), 1);

		final int dayTime = (int)(level.getDayTime() % 24000);
		value *= Math.min(dayTime / 1000.0, 1);
		value *= Math.min(Math.max(12000 - dayTime, 0) / 1000.0, 1);

		if (level.isRaining() && !strong) {
			final BlockPos pos = player.blockPosition();
			return level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.NONE? -0.5 : 0;
		}

		return value;
	}

	/**
	 * Like legacy, the thermal map changes every real-world day; both sides compute the same map.
	 */
	private static synchronized PerlinSimplexNoise getNoise() {
		final int day = LocalDate.now().getDayOfYear();
		if (noise == null || day != noiseDay) {
			noise = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(day)), List.of(0, 1));
			noiseDay = day;
		}
		return noise;
	}

	/**
	 * Legacy removed the glider entity when changing dimension.
	 */
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		event.getEntity().removeData(OBAttachments.HANG_GLIDER);
	}
}
