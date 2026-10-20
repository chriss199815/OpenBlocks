package openblocks.integration.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import openblocks.block.entity.VillageHighlighterBlockEntity;

/**
 * Legacy {@code AdapterVillageHighlighter}. Villages with doors and a radius are gone since 1.14, so {@code getVillages}
 * now describes what the highlighter shows: beds, job sites and bells, and the villagers around it. Positions are relative
 * to the highlighter, like legacy; this works without redstone.
 */
class VillageHighlighterPeripheral extends BlockEntityPeripheral<VillageHighlighterBlockEntity> {

	VillageHighlighterPeripheral(VillageHighlighterBlockEntity highlighter) {
		super(highlighter, "openblocks_village_highlighter");
	}

	@LuaFunction(mainThread = true)
	public final Map<String, Object> getVillages() throws LuaException {
		if (!(blockEntity.getLevel() instanceof ServerLevel level)) throw new LuaException("Not available");
		final BlockPos origin = blockEntity.getBlockPos();

		final List<Map<String, Object>> pois = new ArrayList<>();
		final Map<String, int[]> counts = new HashMap<>();
		for (VillageHighlighterBlockEntity.Poi poi : blockEntity.findPois(level)) {
			final String kind = poi.kind().name().toLowerCase(Locale.ROOT);
			final Map<String, Object> entry = relative(poi.pos(), origin);
			entry.put("kind", kind);
			entry.put("free", poi.free());
			pois.add(entry);
			final int[] count = counts.computeIfAbsent(kind, k -> new int[2]);
			count[0]++;
			if (poi.free()) count[1]++;
		}

		final List<Map<String, Object>> villagers = new ArrayList<>();
		int adults = 0;
		int golemReady = 0;
		for (Villager villager : blockEntity.findVillagers(level)) {
			final Map<String, Object> entry = relative(villager.blockPosition(), origin);
			entry.put("profession", BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString());
			entry.put("baby", villager.isBaby());
			final boolean wantsGolem = villager.wantsToSpawnGolem(level.getGameTime());
			entry.put("wants_golem", wantsGolem);
			villagers.add(entry);
			if (!villager.isBaby()) adults++;
			if (wantsGolem) golemReady++;
		}

		final Map<String, Object> result = new HashMap<>();
		result.put("radius", VillageHighlighterBlockEntity.RANGE);
		result.put("villagers", villagers.size());
		result.put("adults", adults);
		result.put("golem_ready", golemReady);
		result.put("can_breed", blockEntity.canVillagersBreed());
		for (VillageHighlighterBlockEntity.PoiKind kind : VillageHighlighterBlockEntity.PoiKind.values()) {
			final String name = kind.name().toLowerCase(Locale.ROOT);
			final int[] count = counts.getOrDefault(name, new int[2]);
			result.put(name + "s", Map.of("total", count[0], "free", count[1]));
		}
		result.put("poi_list", pois);
		result.put("villager_list", villagers);
		return result;
	}

	private static Map<String, Object> relative(BlockPos pos, BlockPos origin) {
		final Map<String, Object> result = new HashMap<>();
		result.put("x", pos.getX() - origin.getX());
		result.put("y", pos.getY() - origin.getY());
		result.put("z", pos.getZ() - origin.getZ());
		return result;
	}
}
