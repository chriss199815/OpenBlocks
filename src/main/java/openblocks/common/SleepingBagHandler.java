package openblocks.common;

import com.mojang.datafixers.util.Either;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import openblocks.registry.OBAttachments;

/**
 * Port of legacy {@code ItemSleepingBag} sleep logic. Vanilla only applies its sleep checks to real beds, so they are repeated here;
 * the spawn point is never changed, since vanilla only sets it for blocks with a facing.
 */
public class SleepingBagHandler {

	public static boolean isInSleepingBag(Player player) {
		return player.hasData(OBAttachments.SLEEPING_BAG);
	}

	public static void trySleep(ServerPlayer player) {
		final Level level = player.level();
		final BlockPos pos = player.blockPosition();

		if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !isSolidEnough(level, pos.below())) {
			player.displayClientMessage(Component.translatable("openblocks.misc.oh_no_ground"), true);
			return;
		}

		final Player.BedSleepingProblem problem = checkProblem(player, pos);
		if (problem != null) {
			if (problem.getMessage() != null) player.displayClientMessage(problem.getMessage(), true);
			return;
		}

		player.setData(OBAttachments.SLEEPING_BAG, player.getYRot());
		final Either<Player.BedSleepingProblem, Unit> result = player.startSleepInBed(pos);
		result.ifLeft(p -> {
			player.removeData(OBAttachments.SLEEPING_BAG);
			if (p.getMessage() != null) player.displayClientMessage(p.getMessage(), true);
		});
	}

	/**
	 * Same rules as {@code ServerPlayer.startSleepInBed} for beds.
	 */
	private static Player.BedSleepingProblem checkProblem(ServerPlayer player, BlockPos pos) {
		final Level level = player.level();
		if (player.isSleeping() || !player.isAlive()) return Player.BedSleepingProblem.OTHER_PROBLEM;
		if (!level.dimensionType().natural()) return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
		if (level.isDay()) return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;

		if (!player.isCreative()) {
			final List<Monster> monsters = level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(8, 5, 8), monster -> monster.isPreventingPlayerRest(player));
			if (!monsters.isEmpty()) return Player.BedSleepingProblem.NOT_SAFE;
		}

		return null;
	}

	private static boolean isSolidEnough(Level level, BlockPos pos) {
		final var shape = level.getBlockState(pos).getCollisionShape(level, pos);
		if (shape.isEmpty()) return false;
		final AABB box = shape.bounds();
		return box.getXsize() >= 0.5 && box.getYsize() >= 0.5 && box.getZsize() >= 0.5;
	}

	/**
	 * Without a bed vanilla would wake the player immediately; daytime still wakes them.
	 */
	public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
		if (event.getEntity() instanceof Player player && isInSleepingBag(player) && event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_HERE)
			event.setContinueSleeping(true);
	}

	public static void onWakeUp(PlayerWakeUpEvent event) {
		if (!event.getEntity().level().isClientSide) event.getEntity().removeData(OBAttachments.SLEEPING_BAG);
	}
}
