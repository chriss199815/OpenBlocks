package openblocks.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.api.GraveDropsEvent;
import openblocks.api.GraveSpawnEvent;
import openblocks.block.GraveBlock;
import openblocks.block.entity.GraveBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBGameRules;

/**
 * Port of legacy {@code PlayerDeathHandler}: moves a dead player's drops into a grave placed near the death position.
 * Inventory backups ({@code storeContents} / {@code ob_inventory}) come with the commands in wave 4.
 */
public class PlayerDeathHandler {

	private static List<BlockPos> searchOrder = List.of();
	private static int searchOrderSize = -1;

	private interface PlacementChecker {
		boolean canPlace(ServerLevel level, Player player, BlockPos pos);
	}

	private static final PlacementChecker POLITE = (level, player, pos) -> {
		final BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced();
	};

	/**
	 * Preferred over {@link #POLITE}, so graves land on the ground instead of floating on a dirt block in the air.
	 */
	private static final PlacementChecker GROUNDED = (level, player, pos) -> POLITE.canPlace(level, player, pos)
			&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);

	private static final PlacementChecker BRUTAL = (level, player, pos) -> level.getBlockState(pos).getDestroySpeed(level, pos) >= 0 && level.getBlockEntity(pos) == null;

	public static void onLivingDrops(LivingDropsEvent event) {
		if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;

		if (player instanceof FakePlayer) return;

		if (event.isCanceled()) {
			OpenBlocks.LOG.warn("Drops event for player '{}' cancelled, grave will not be spawned", player.getGameProfile().getName());
			return;
		}

		final List<ItemEntity> drops = new ArrayList<>(event.getDrops());
		if (drops.isEmpty()) return;

		final GameRules gameRules = level.getGameRules();
		if (gameRules.getBoolean(GameRules.RULE_KEEPINVENTORY) || !gameRules.getBoolean(OBGameRules.SPAWN_GRAVES)) return;

		final GraveDropsEvent dropsEvent = new GraveDropsEvent(player);
		drops.forEach(dropsEvent::addItem);
		if (NeoForge.EVENT_BUS.post(dropsEvent).isCanceled()) {
			OpenBlocks.LOG.warn("Grave drops event for player '{}' cancelled, grave will not be spawned", player.getGameProfile().getName());
			return;
		}

		final List<ItemEntity> graveLoot = new ArrayList<>();
		final List<ItemEntity> normalDrops = new ArrayList<>();
		for (GraveDropsEvent.ItemAction entry : dropsEvent.drops) {
			switch (entry.action) {
				case DELETE -> {}
				case DROP -> normalDrops.add(entry.item);
				case STORE -> graveLoot.add(entry.item);
			}
		}

		if (graveLoot.isEmpty()) return;

		if (!tryConsumeGrave(player, graveLoot, normalDrops)) return;

		event.getDrops().clear();
		event.getDrops().addAll(normalDrops);

		// placed on the next tick, like legacy, so other drop handlers have finished
		final MinecraftServer server = level.getServer();
		final GraveSpawner spawner = new GraveSpawner(level, player, graveLoot);
		server.tell(new TickTask(server.getTickCount(), spawner));
	}

	private static boolean tryConsumeGrave(Player player, List<ItemEntity> graveLoot, List<ItemEntity> normalDrops) {
		if (!OBConfig.GRAVES.requiresGraveInInv.getAsBoolean() || player.isCreative()) return true;

		for (List<ItemEntity> list : List.of(graveLoot, normalDrops)) {
			final Iterator<ItemEntity> it = list.iterator();
			while (it.hasNext()) {
				final ItemEntity drop = it.next();
				final ItemStack stack = drop.getItem();
				if (stack.is(OBBlocks.GRAVE.get().asItem())) {
					stack.shrink(1);
					if (stack.isEmpty()) it.remove();
					else drop.setItem(stack);
					return true;
				}
			}
		}

		return false;
	}

	private static List<BlockPos> getSearchOrder(int size) {
		if (searchOrderSize != size) {
			final List<BlockPos> coords = new ArrayList<>();
			for (int x = -size; x <= size; x++)
				for (int y = -size; y <= size; y++)
					for (int z = -size; z <= size; z++)
						coords.add(new BlockPos(x, y, z));

			// first by Manhattan distance, then prefer positions away from the axes
			coords.sort(Comparator.<BlockPos>comparingInt(c -> Math.abs(c.getX()) + Math.abs(c.getY()) + Math.abs(c.getZ()))
					.thenComparing(Comparator.<BlockPos>comparingInt(c -> Math.max(Math.max(Math.abs(c.getX()), Math.abs(c.getY())), Math.abs(c.getZ()))).reversed()));
			searchOrder = List.copyOf(coords);
			searchOrderSize = size;
		}
		return searchOrder;
	}

	private static class GraveSpawner implements Runnable {

		private final ServerLevel level;
		private final Player player;
		private final List<ItemEntity> loot;
		private final BlockPos playerPos;
		private final Component deathMessage;

		GraveSpawner(ServerLevel level, Player player, List<ItemEntity> loot) {
			this.level = level;
			this.player = player;
			this.loot = List.copyOf(loot);
			this.playerPos = player.blockPosition();

			final Component day = Component.literal(String.format("%.1f", level.getGameTime() / 24000.0)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
			this.deathMessage = Component.translatable("openblocks.misc.grave_msg", player.getCombatTracker().getDeathMessage(), day);
		}

		@Override
		public void run() {
			if (!trySpawnGrave()) {
				OpenBlocks.LOG.warn("Failed to spawn grave for player '{}', dropping items", player.getGameProfile().getName());
				for (ItemEntity drop : loot)
					level.addFreshEntity(drop);
			}
		}

		private boolean trySpawnGrave() {
			final GraveSpawnEvent event = new GraveSpawnEvent(player, findLocation(), loot, player.getGameProfile().getName(), deathMessage);
			if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
				OpenBlocks.LOG.warn("Grave event for player '{}' cancelled, no grave will spawn", player.getGameProfile().getName());
				return false;
			}

			final BlockPos location = event.location;
			if (location == null) return false;

			final BlockPos under = location.below();
			if (OBConfig.GRAVES.canSpawnBase.getAsBoolean() && level.isLoaded(under) && level.isEmptyBlock(under) && level.mayInteract(player, under))
				level.setBlockAndUpdate(under, Blocks.DIRT.defaultBlockState());

			level.setBlockAndUpdate(location, GraveBlock.stateFor(level, location));
			if (!(level.getBlockEntity(location) instanceof GraveBlockEntity grave)) {
				OpenBlocks.LOG.warn("Failed to place grave @ {}: invalid block entity", location);
				return false;
			}

			grave.setUsername(event.gravestoneText);
			grave.setDeathMessage(event.clickText);
			grave.setLoot(loot.stream().map(ItemEntity::getItem).toList());

			OpenBlocks.LOG.info("Grave for ({},{}) was spawned at ({}) (player died at ({}))", player.getUUID(), player.getGameProfile().getName(), location, playerPos);
			return true;
		}

		@Nullable
		private BlockPos findLocation() {
			final BlockPos grounded = findLocation(GROUNDED);
			if (grounded != null) return grounded;

			// e.g. over lava or the void: legacy behaviour, a dirt block is placed under the grave
			final BlockPos location = findLocation(POLITE);
			if (location != null || !OBConfig.GRAVES.destructiveGraves.getAsBoolean()) return location;

			OpenBlocks.LOG.warn("Failed to place grave for player '{}', going berserk", player.getGameProfile().getName());
			return findLocation(BRUTAL);
		}

		@Nullable
		private BlockPos findLocation(PlacementChecker checker) {
			// replaces legacy minimalPosY/maximalPosY; preferring grounded spots already keeps graves out of bedrock pockets
			final int minY = level.getMinBuildHeight();
			final int maxY = level.getMaxBuildHeight() - 1;
			final BlockPos searchPos = new BlockPos(playerPos.getX(), Math.min(Math.max(playerPos.getY(), minY), maxY), playerPos.getZ());

			for (BlockPos offset : getSearchOrder(OBConfig.GRAVES.spawnRange.getAsInt() / 2)) {
				final BlockPos pos = searchPos.offset(offset);
				if (pos.getY() < minY || pos.getY() > maxY) continue;
				if (level.isLoaded(pos) && level.mayInteract(player, pos) && checker.canPlace(level, player, pos)) return pos;
			}

			return null;
		}
	}
}
