package openblocks.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.block.entity.TrophyBlockEntity;

/**
 * Ports of the legacy {@code openblocks.trophy} behaviours.
 */
public final class TrophyBehaviors {

	private TrophyBehaviors() {}

	/**
	 * Legacy {@code ItemDropBehavior}: drops an item at the player, then waits {@code cooldown} ticks.
	 */
	public static TrophyBehavior dropItem(int cooldown, Item item) {
		return dropItem(cooldown, item, null);
	}

	public static TrophyBehavior dropItem(int cooldown, Item item, SoundEvent sound) {
		return (trophy, player) -> {
			if (sound != null) {
				final Level level = player.level();
				player.playSound(sound, 1.0f, (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1.0f);
			}
			player.spawnAtLocation(new ItemStack(item));
			return cooldown;
		};
	}

	public static int blaze(TrophyBlockEntity trophy, Player player) {
		player.igniteForSeconds(4);
		return 0;
	}

	public static int caveSpider(TrophyBlockEntity trophy, Player player) {
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 3));
		return 0;
	}

	/**
	 * Like legacy, the explosion hurts, but doesn't break blocks.
	 */
	public static int creeper(TrophyBlockEntity trophy, Player player) {
		final BlockPos pos = trophy.getBlockPos();
		trophy.getLevel().explode(player, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 2, Level.ExplosionInteraction.NONE);
		return 0;
	}

	/**
	 * Throws an ender pearl for the player in a random direction. Unlike legacy (10 ticks), waits 4 minutes, so it can't
	 * be used as a free teleporter.
	 */
	public static int enderman(TrophyBlockEntity trophy, Player player) {
		final Level level = trophy.getLevel();
		final BlockPos pos = trophy.getBlockPos();
		final ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
		pearl.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		pearl.setDeltaMovement(level.random.nextGaussian(), 1, level.random.nextGaussian());
		level.addFreshEntity(pearl);
		return 4 * 60 * 20;
	}

	/**
	 * The totem of undying animation, only for the player.
	 */
	public static int evoker(TrophyBlockEntity trophy, Player player) {
		if (player instanceof ServerPlayer serverPlayer)
			serverPlayer.connection.send(new ClientboundEntityEventPacket(player, (byte)35));
		return 100;
	}

	/**
	 * The elder guardian curse animation, only for the player.
	 */
	public static int guardian(TrophyBlockEntity trophy, Player player) {
		if (player instanceof ServerPlayer serverPlayer)
			serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 0.0f));
		return 100;
	}

	/**
	 * Spits at the player.
	 */
	public static int llama(TrophyBlockEntity trophy, Player player) {
		final Level level = trophy.getLevel();
		final BlockPos pos = trophy.getBlockPos();
		final LlamaSpit spit = new LlamaSpit(EntityType.LLAMA_SPIT, level);
		spit.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

		final double dx = player.getX() - spit.getX();
		final double dy = player.getY(0.3333) - spit.getY();
		final double dz = player.getZ() - spit.getZ();
		final double horizontal = Math.sqrt(dx * dx + dz * dz) * 0.2;
		spit.shoot(dx, dy + horizontal, dz, 1.5f, 10.0f);
		level.playSound(null, pos, SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, 1.0f, 1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f);
		level.addFreshEntity(spit);
		return 0;
	}

	/**
	 * Brown mushrooms on the free spots around the trophy.
	 */
	public static int mooshroom(TrophyBlockEntity trophy, Player player) {
		placeAround(trophy, Blocks.BROWN_MUSHROOM.defaultBlockState());
		return 100;
	}

	public static int shulker(TrophyBlockEntity trophy, Player player) {
		player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 1));
		return 100;
	}

	/**
	 * Shoots a harmless arrow almost straight up, which can't be picked up.
	 */
	public static int skeleton(TrophyBlockEntity trophy, Player player) {
		final Level level = trophy.getLevel();
		final BlockPos pos = trophy.getBlockPos();
		final Arrow arrow = new Arrow(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, new ItemStack(Items.ARROW), null);
		arrow.setBaseDamage(0.1);
		arrow.shoot(level.random.nextInt(10) - 5, 40, level.random.nextInt(10) - 5, 1.0f, 6.0f);
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0f, 1.0f / (level.random.nextFloat() * 0.4f + 1.2f) + 0.5f);
		level.addFreshEntity(arrow);
		return 0;
	}

	/**
	 * Snow layers of random height on the free spots around the trophy.
	 */
	public static int snowGolem(TrophyBlockEntity trophy, Player player) {
		final Level level = trophy.getLevel();
		final BlockPos base = trophy.getBlockPos();
		for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1)))
			place(level, pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Mth.nextInt(level.random, 1, SnowLayerBlock.MAX_HEIGHT)));
		return 10;
	}

	/**
	 * Water on top of the trophy, or a hiss where water evaporates.
	 */
	public static int squid(TrophyBlockEntity trophy, Player player) {
		final Level level = trophy.getLevel();
		final BlockPos above = trophy.getBlockPos().above();
		if (level.dimensionType().ultraWarm()) level.playSound(null, above, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f);
		else if (level.isEmptyBlock(above)) level.setBlockAndUpdate(above, Blocks.WATER.defaultBlockState());
		return 10;
	}

	public static int witch(TrophyBlockEntity trophy, Player player) {
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 1));
		player.sendSystemMessage(Component.translatable("openblocks.misc.get_witched"));
		return 0;
	}

	private static void placeAround(TrophyBlockEntity trophy, BlockState state) {
		final BlockPos base = trophy.getBlockPos();
		for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1)))
			place(trophy.getLevel(), pos, state);
	}

	private static void place(Level level, BlockPos pos, BlockState state) {
		if (level.isEmptyBlock(pos) && state.canSurvive(level, pos)) level.setBlockAndUpdate(pos, state);
	}
}
