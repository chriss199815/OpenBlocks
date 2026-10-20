package openblocks.block.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import openblocks.block.VillageHighlighterBlock;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityVillageHighlighter}. Villages as objects are gone since 1.14, a village is now made
 * of points of interest that villagers claim: beds, job sites and bells. So instead of the village sphere and the old
 * golem spawn box, the highlighter shows these points (free or claimed) and, around every villager, the area where it
 * can summon an iron golem.
 * <p>
 * Villagers breed when there is a free bed; the signal ({@link #canVillagersBreed()}) is on while there are a free bed
 * and two adult villagers in range (legacy: fewer villagers than 35% of the doors).
 */
public class VillageHighlighterBlockEntity extends BlockEntity {

	/**
	 * Like the distance at which breeding villagers look for a free bed.
	 */
	public static final int RANGE = 48;

	private static final int UPDATE_INTERVAL = 10;

	private static final String TAG_POIS = "Pois";
	private static final String TAG_POI_KINDS = "PoiKinds";
	private static final String TAG_VILLAGERS = "Villagers";
	private static final String TAG_VILLAGER_FLAGS = "VillagerFlags";

	public enum PoiKind {
		BED, JOB_SITE, MEETING_POINT
	}

	public record Poi(BlockPos pos, PoiKind kind, boolean free) {}

	/**
	 * Entity id of a villager and whether it could summon an iron golem now (it slept recently and has not seen a golem).
	 * The client follows the entity itself, so its golem area moves along without waiting for the next update.
	 */
	public record VillagerInfo(int entityId, boolean wantsGolem) {}

	private List<Poi> pois = List.of();
	private List<VillagerInfo> villagers = List.of();

	private boolean canBreed;

	public VillageHighlighterBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, VillageHighlighterBlockEntity highlighter) {
		if (level.getGameTime() % UPDATE_INTERVAL == 0) highlighter.update((ServerLevel)level, state);
	}

	/**
	 * Beds, job sites and bells in range, also while unpowered (for computers).
	 */
	public List<Poi> findPois(ServerLevel level) {
		final List<Poi> pois = new ArrayList<>();
		level.getPoiManager().getInRange(type -> true, worldPosition, RANGE, PoiManager.Occupancy.ANY).forEach(record -> {
			final PoiKind kind = kindOf(record);
			if (kind != null) pois.add(new Poi(record.getPos(), kind, record.hasSpace()));
		});
		return pois;
	}

	public List<Villager> findVillagers(ServerLevel level) {
		return level.getEntitiesOfClass(Villager.class, new AABB(worldPosition).inflate(RANGE));
	}

	private void update(ServerLevel level, BlockState state) {
		final List<Poi> pois = findPois(level);

		final List<VillagerInfo> villagers = new ArrayList<>();
		int adults = 0;
		for (Villager villager : findVillagers(level)) {
			villagers.add(new VillagerInfo(villager.getId(), villager.wantsToSpawnGolem(level.getGameTime())));
			if (!villager.isBaby()) adults++;
		}

		final boolean canBreed = adults >= 2 && pois.stream().anyMatch(poi -> poi.kind() == PoiKind.BED && poi.free());
		if (canBreed != this.canBreed) {
			this.canBreed = canBreed;
			VillageHighlighterBlock.updateOutput(level, worldPosition, state);
		}

		// nothing to show while unpowered
		if (!state.getValue(VillageHighlighterBlock.POWERED)) {
			pois.clear();
			villagers.clear();
		}
		if (!pois.equals(this.pois) || !villagers.equals(this.villagers)) {
			this.pois = List.copyOf(pois);
			this.villagers = List.copyOf(villagers);
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	private static PoiKind kindOf(PoiRecord record) {
		if (record.getPoiType().is(PoiTypes.HOME)) return PoiKind.BED;
		if (record.getPoiType().is(PoiTypes.MEETING)) return PoiKind.MEETING_POINT;
		if (VillagerProfession.ALL_ACQUIRABLE_JOBS.test(record.getPoiType())) return PoiKind.JOB_SITE;
		return null;
	}

	public boolean canVillagersBreed() {
		return canBreed;
	}

	public List<Poi> getPois() {
		return pois;
	}

	public List<VillagerInfo> getVillagers() {
		return villagers;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		final long[] poiPositions = new long[pois.size()];
		final byte[] poiKinds = new byte[pois.size()];
		for (int i = 0; i < pois.size(); i++) {
			final Poi poi = pois.get(i);
			poiPositions[i] = poi.pos().asLong();
			poiKinds[i] = (byte)(poi.kind().ordinal() << 1 | (poi.free()? 1 : 0));
		}
		tag.putLongArray(TAG_POIS, poiPositions);
		tag.putByteArray(TAG_POI_KINDS, poiKinds);

		final int[] villagerIds = new int[villagers.size()];
		final byte[] villagerFlags = new byte[villagers.size()];
		for (int i = 0; i < villagers.size(); i++) {
			villagerIds[i] = villagers.get(i).entityId();
			villagerFlags[i] = (byte)(villagers.get(i).wantsGolem()? 1 : 0);
		}
		tag.putIntArray(TAG_VILLAGERS, villagerIds);
		tag.putByteArray(TAG_VILLAGER_FLAGS, villagerFlags);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		final long[] poiPositions = tag.getLongArray(TAG_POIS);
		final byte[] poiKinds = tag.getByteArray(TAG_POI_KINDS);
		final PoiKind[] kinds = PoiKind.values();
		final List<Poi> pois = new ArrayList<>();
		for (int i = 0; i < Math.min(poiPositions.length, poiKinds.length); i++) {
			final int kind = poiKinds[i] >> 1;
			if (kind < kinds.length) pois.add(new Poi(BlockPos.of(poiPositions[i]), kinds[kind], (poiKinds[i] & 1) != 0));
		}
		this.pois = pois;

		final int[] villagerIds = tag.getIntArray(TAG_VILLAGERS);
		final byte[] villagerFlags = tag.getByteArray(TAG_VILLAGER_FLAGS);
		final List<VillagerInfo> villagers = new ArrayList<>();
		for (int i = 0; i < Math.min(villagerIds.length, villagerFlags.length); i++)
			villagers.add(new VillagerInfo(villagerIds[i], villagerFlags[i] != 0));
		this.villagers = villagers;
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		handleUpdateTag(packet.getTag(), registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
