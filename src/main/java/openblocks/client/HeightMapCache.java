package openblocks.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import openblocks.common.HeightMapData;
import openblocks.network.HeightMapRequestPayload;
import openblocks.registry.OBComponents;

/**
 * Port of the client side of legacy {@code MapDataManager}: height maps received from the server. Unknown maps are
 * asked for once; changed maps are asked for again when the server announces them.
 */
public class HeightMapCache {

	private static final Map<Integer, HeightMapData> maps = new HashMap<>();
	private static final Set<Integer> requested = new HashSet<>();

	/**
	 * @return the map, or null until the server sent it
	 */
	@Nullable
	public static HeightMapData get(int mapId) {
		final HeightMapData data = maps.get(mapId);
		if (data == null && requested.add(mapId)) PacketDistributor.sendToServer(new HeightMapRequestPayload(List.of(mapId)));
		return data;
	}

	public static void put(int mapId, HeightMapData data) {
		maps.put(mapId, data);
	}

	public static void onUpdates(List<Integer> mapIds) {
		final List<Integer> known = new ArrayList<>();
		for (int mapId : mapIds)
			if (maps.containsKey(mapId)) known.add(mapId);
		if (!known.isEmpty()) PacketDistributor.sendToServer(new HeightMapRequestPayload(known));
	}

	/**
	 * Legacy {@code ItemHeightMap.addInformation}: the center of a height map that has been mapped.
	 */
	public static void onItemTooltip(ItemTooltipEvent event) {
		final Integer mapId = event.getItemStack().get(OBComponents.HEIGHT_MAP_ID);
		if (mapId == null || event.getEntity() == null) return;
		final HeightMapData data = get(mapId);
		if (data == null || data.getDimension() == null) return;
		final List<Component> tooltip = event.getToolTip();
		final int index = Math.min(1, tooltip.size());
		tooltip.add(index, Component.translatable("openblocks.misc.map_center", data.getCenterX(), data.getCenterZ()).withStyle(ChatFormatting.GRAY));
	}

	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		maps.clear();
		requested.clear();
	}
}
