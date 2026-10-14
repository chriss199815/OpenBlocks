package openblocks.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import openblocks.OBConfig;
import openblocks.registry.OBAttachments;
import openblocks.registry.OBSounds;
import openblocks.registry.OBStats;
import openblocks.registry.OBTriggers;

/**
 * Port of legacy {@code BrickManager}: eating tasty clay fills the player's bowels, the "Be silly" key drops one brick
 * behind them (with a sound); on death all of them come out (at most 16). Only without
 * {@code tomfoolery.weAreSeriousPeople}, like legacy; the bowels are a player attachment instead of a capability.
 */
public final class BrickDrops {

	private static final int MAX_DEATH_DROPS = 16;

	private BrickDrops() {}

	public static boolean isEnabled() {
		return !OBConfig.TOMFOOLERY.weAreSeriousPeople.getAsBoolean();
	}

	public static void registerStat(FMLCommonSetupEvent event) {
		// creates the stat with its formatter, custom stats are only names in their registry
		event.enqueueWork(() -> Stats.CUSTOM.get(OBStats.BRICKS_DROPPED.get(), StatFormatter.DEFAULT));
	}

	public static void onEatTastyClay(Player player) {
		if (isEnabled()) player.setData(OBAttachments.BOWELS, player.getData(OBAttachments.BOWELS) + 1);
	}

	public static void dropBrick(ServerPlayer player) {
		if (!isEnabled() || !player.isAlive()) return;

		player.level().playSound(null, player.blockPosition(), OBSounds.PLAYER_WHOOPS.get(), SoundSource.PLAYERS, 1, 1);

		if (!player.getAbilities().instabuild) {
			final int bricks = player.getData(OBAttachments.BOWELS);
			if (bricks <= 0) return;
			player.setData(OBAttachments.BOWELS, bricks - 1);
		}

		final ItemEntity brick = createBrick(player);
		brick.setDefaultPickUpDelay();
		player.level().addFreshEntity(brick);
		OBTriggers.BRICK_DROPPED.get().trigger(player);
		player.awardStat(OBStats.BRICKS_DROPPED.get());
	}

	public static void onLivingDrops(LivingDropsEvent event) {
		if (!isEnabled() || !(event.getEntity() instanceof ServerPlayer player)) return;

		final int bricks = Math.min(player.getData(OBAttachments.BOWELS), MAX_DEATH_DROPS);
		for (int i = 0; i < bricks; i++)
			event.getDrops().add(createBrick(player));
		player.removeData(OBAttachments.BOWELS);
	}

	// out of the back of the entity
	private static ItemEntity createBrick(Entity dropper) {
		final double rotation = Math.toRadians(dropper.getYRot()) - Math.PI / 2;
		final double dx = Math.cos(rotation);
		final double dz = Math.sin(rotation);
		return new ItemEntity(dropper.level(), dropper.getX() + 0.75 * dx, dropper.getY() + 0.5, dropper.getZ() + 0.75 * dz,
				new ItemStack(Items.BRICK), 0.5 * dx, 0.2, 0.5 * dz);
	}
}
