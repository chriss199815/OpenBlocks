package openblocks.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import openblocks.OpenBlocks;
import openblocks.item.HeightMapItem;
import openblocks.network.HeightMapPayload;
import openblocks.network.HeightMapUpdatesPayload;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

/**
 * Port of the server side of legacy {@code MapDataManager}: creates and looks up height maps, and tells clients which
 * maps changed, so they ask again for the ones they show (see {@code HeightMapCache}).
 */
public class HeightMaps {

	private static final int UPDATE_INTERVAL = 10;
	private static final String COUNTER_NAME = "openblocks_height_maps";

	private static final Set<Integer> updatedMaps = new HashSet<>();
	private static int ticksToUpdate;

	private static DimensionDataStorage storage(MinecraftServer server) {
		return server.overworld().getDataStorage();
	}

	private static String name(int mapId) {
		return "openblocks_height_map_" + mapId;
	}

	public static int create(MinecraftServer server, int scale) {
		final int mapId = storage(server).computeIfAbsent(Counter.FACTORY, COUNTER_NAME).next();
		final HeightMapData data = new HeightMapData();
		data.setScale(scale);
		data.setDirty();
		storage(server).set(name(mapId), data);
		return mapId;
	}

	@Nullable
	public static HeightMapData get(MinecraftServer server, int mapId) {
		return storage(server).get(HeightMapData.FACTORY, name(mapId));
	}

	public static void markUpdated(int mapId, HeightMapData data) {
		data.setDirty();
		updatedMaps.add(mapId);
	}

	/**
	 * Legacy {@code ItemEmptyMap.upgradeToMap}: an empty map becomes a new height map with its scale; other stacks are returned as they are.
	 */
	public static ItemStack upgradeToMap(MinecraftServer server, ItemStack stack) {
		if (!stack.is(OBItems.EMPTY_MAP)) return stack;
		final int scale = stack.getOrDefault(OBComponents.MAP_SCALE, 0);
		return HeightMapItem.create(create(server, scale), scale);
	}

	public static void onServerTick(ServerTickEvent.Post event) {
		if (++ticksToUpdate < UPDATE_INTERVAL) return;
		ticksToUpdate = 0;
		if (updatedMaps.isEmpty()) return;
		final HeightMapUpdatesPayload payload = new HeightMapUpdatesPayload(List.copyOf(updatedMaps));
		// fake players (and clients without the mod) can not take it
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers())
			if (player.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(player, payload);
		updatedMaps.clear();
	}

	public static void sendMaps(ServerPlayer player, List<Integer> mapIds) {
		for (int mapId : mapIds) {
			final HeightMapData data = get(player.server, mapId);
			if (data != null) PacketDistributor.sendToPlayer(player, new HeightMapPayload(mapId, data.copy()));
			else OpenBlocks.LOG.debug("Player {} asked for non-existent height map {}", player.getName().getString(), mapId);
		}
	}

	private static class Counter extends SavedData {

		static final SavedData.Factory<Counter> FACTORY = new SavedData.Factory<>(Counter::new, Counter::load, null);

		private static final String TAG_NEXT = "Next";

		private int nextId;

		int next() {
			setDirty();
			return nextId++;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
			tag.putInt(TAG_NEXT, nextId);
			return tag;
		}

		private static Counter load(CompoundTag tag, HolderLookup.Provider registries) {
			final Counter counter = new Counter();
			counter.nextId = tag.getInt(TAG_NEXT);
			return counter;
		}
	}
}
