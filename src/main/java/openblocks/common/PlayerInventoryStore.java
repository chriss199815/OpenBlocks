package openblocks.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.api.InventoryEvent;
import openblocks.api.InventoryEvent.SubInventory;

/**
 * Port of legacy {@code PlayerInventoryStore}: inventory backups as compressed NBT files {@code inventory-<player>-<date>-<type>-<n>.dat}
 * in the world's {@code data} folder, written on death, for graves and by {@code /ob_inventory store}.
 * Files from 1.12 are not compatible (item format changed).
 */
public class PlayerInventoryStore {

	public static final String TAG_PLAYER_UUID = "PlayerUUID";

	public static final String TAG_PLAYER_NAME = "PlayerName";

	private static final String TAG_LOCATION = "Location";

	private static final String TAG_INVENTORY = "Inventory";

	private static final String TAG_SUB_INVENTORIES = "SubInventories";

	private static final String TAG_SIZE = "Size";

	private static final String TAG_ITEMS = "Items";

	private static final String TAG_SLOT = "Slot";

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

	private static final Pattern SAFE_CHARS = Pattern.compile("[^A-Za-z0-9_-]");

	// also keeps ids from escaping the data folder
	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_.-]+");

	private static final String PREFIX = "inventory-";

	private static final String SUFFIX = ".dat";

	/**
	 * @param mainInventory slot contents, {@code null} if the file has none
	 */
	public record LoadedInventories(@Nullable List<ItemStack> mainInventory, Map<String, SubInventory> subInventories) {}

	public static Path getSaveFolder(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").normalize();
	}

	private static synchronized Path getNewDumpFile(MinecraftServer server, String player, String type) throws IOException {
		final Path folder = getSaveFolder(server);
		Files.createDirectories(folder);
		final String date = LocalDateTime.now().format(FORMATTER);

		int id = 0;
		while (true) {
			final Path file = folder.resolve(String.format(PREFIX + "%s-%s-%s-%d" + SUFFIX, player, date, type, id));
			if (!Files.exists(file)) {
				// reserve the name, so concurrent dumps don't pick it too
				Files.createFile(file);
				return file;
			}
			id++;
		}
	}

	/**
	 * Accepts both the full file name and just the id in between {@code inventory-} and {@code .dat}, like legacy.
	 */
	public static String stripFilename(String name) {
		String result = name;
		if (result.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) result = result.substring(PREFIX.length());
		if (result.regionMatches(true, result.length() - SUFFIX.length(), SUFFIX, 0, SUFFIX.length())) result = result.substring(0, result.length() - SUFFIX.length());
		return result;
	}

	public static Path storePlayerInventory(ServerPlayer player, String type) throws IOException {
		final InventoryEvent.Store event = NeoForge.EVENT_BUS.post(new InventoryEvent.Store(player));

		final Inventory inventory = player.getInventory();
		final NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++)
			items.set(i, inventory.getItem(i));

		final Map<String, SubInventory> subs = event.getSubInventories();
		return storeInventory(player.server, items, player.getGameProfile().getName(), type, meta -> {
			setPlayerInfo(meta, player);

			final ListTag location = new ListTag();
			location.add(DoubleTag.valueOf(player.getX()));
			location.add(DoubleTag.valueOf(player.getY()));
			location.add(DoubleTag.valueOf(player.getZ()));
			meta.put(TAG_LOCATION, location);

			final CompoundTag subInventories = new CompoundTag();
			for (Map.Entry<String, SubInventory> e : subs.entrySet())
				subInventories.put(e.getKey(), saveItems(player.registryAccess(), e.getValue().asMap()));
			meta.put(TAG_SUB_INVENTORIES, subInventories);
		});
	}

	public static void setPlayerInfo(CompoundTag meta, Player player) {
		meta.putString(TAG_PLAYER_NAME, player.getGameProfile().getName());
		meta.putString(TAG_PLAYER_UUID, player.getUUID().toString());
	}

	public static Path storeInventory(MinecraftServer server, List<ItemStack> inventory, String name, String type, Consumer<CompoundTag> extras) throws IOException {
		final Map<Integer, ItemStack> slots = new HashMap<>();
		for (int i = 0; i < inventory.size(); i++)
			slots.put(i, inventory.get(i));

		final CompoundTag invData = saveItems(server.registryAccess(), slots);
		invData.putInt(TAG_SIZE, inventory.size());

		final CompoundTag root = new CompoundTag();
		root.put(TAG_INVENTORY, invData);
		root.putLong("Created", System.currentTimeMillis());
		root.putString("Type", type);
		extras.accept(root);

		final Path dumpFile = getNewDumpFile(server, SAFE_CHARS.matcher(name).replaceAll("_"), type);
		try {
			NbtIo.writeCompressed(root, dumpFile);
		} catch (IOException e) {
			Files.deleteIfExists(dumpFile);
			throw e;
		}
		return dumpFile;
	}

	private static CompoundTag saveItems(HolderLookup.Provider registries, Map<Integer, ItemStack> slots) {
		final ListTag items = new ListTag();
		slots.entrySet().stream()
				.filter(e -> !e.getValue().isEmpty())
				.sorted(Map.Entry.comparingByKey())
				.forEach(e -> {
					final CompoundTag stackTag = new CompoundTag();
					stackTag.putInt(TAG_SLOT, e.getKey());
					items.add(e.getValue().save(registries, stackTag));
				});

		final CompoundTag result = new CompoundTag();
		result.put(TAG_ITEMS, items);
		return result;
	}

	private static Map<Integer, ItemStack> loadItems(HolderLookup.Provider registries, CompoundTag tag) {
		final Map<Integer, ItemStack> result = new HashMap<>();
		final ListTag items = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
		for (int i = 0; i < items.size(); i++) {
			final CompoundTag stackTag = items.getCompound(i);
			final ItemStack stack = ItemStack.parseOptional(registries, stackTag);
			if (!stack.isEmpty()) result.put(stackTag.getInt(TAG_SLOT), stack);
		}
		return result;
	}

	@Nullable
	private static Path findDump(MinecraftServer server, String fileId) {
		final String id = stripFilename(fileId);
		if (!VALID_ID.matcher(id).matches()) return null;
		final Path file = getSaveFolder(server).resolve(PREFIX + id + SUFFIX);
		return Files.isRegularFile(file)? file : null;
	}

	public static List<String> getMatchedDumps(MinecraftServer server, String prefix) {
		final String actualPrefix = PREFIX + stripFilename(prefix);
		try (Stream<Path> files = Files.list(getSaveFolder(server))) {
			return files.map(f -> f.getFileName().toString())
					.filter(name -> name.startsWith(actualPrefix) && name.endsWith(SUFFIX))
					.map(PlayerInventoryStore::stripFilename)
					.sorted()
					.toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	/**
	 * @return {@code null} if there is no such backup
	 */
	@Nullable
	public static LoadedInventories loadInventories(MinecraftServer server, String fileId) throws IOException {
		final Path file = findDump(server, fileId);
		if (file == null) return null;

		final CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		final HolderLookup.Provider registries = server.registryAccess();

		List<ItemStack> mainInventory = null;
		if (root.contains(TAG_INVENTORY, Tag.TAG_COMPOUND)) {
			final CompoundTag invTag = root.getCompound(TAG_INVENTORY);
			final Map<Integer, ItemStack> slots = loadItems(registries, invTag);
			final int size = Math.max(invTag.getInt(TAG_SIZE), slots.keySet().stream().mapToInt(slot -> slot + 1).max().orElse(0));
			final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
			slots.forEach((slot, stack) -> {
				if (slot >= 0) items.set(slot, stack);
			});
			mainInventory = items;
		}

		final Map<String, SubInventory> subInventories = new HashMap<>();
		final CompoundTag subsTag = root.getCompound(TAG_SUB_INVENTORIES);
		for (String key : subsTag.getAllKeys()) {
			final SubInventory sub = new SubInventory();
			loadItems(registries, subsTag.getCompound(key)).forEach(sub::addItemStack);
			subInventories.put(key, sub);
		}

		return new LoadedInventories(mainInventory, subInventories);
	}

	public static boolean restoreInventory(ServerPlayer player, String fileId) throws IOException {
		final LoadedInventories inventories = loadInventories(player.server, fileId);
		if (inventories == null) return false;

		final List<ItemStack> main = inventories.mainInventory();
		if (main != null) {
			final Inventory current = player.getInventory();
			final int targetSize = current.getContainerSize();
			for (int i = 0; i < main.size(); i++) {
				final ItemStack stack = main.get(i);
				if (i < targetSize) current.setItem(i, stack);
				else if (!stack.isEmpty()) player.drop(stack, false, false);
			}
		}

		NeoForge.EVENT_BUS.post(new InventoryEvent.Load(player, inventories.subInventories()));
		return true;
	}

	public static void onPlayerDeath(LivingDeathEvent event) {
		if (!OBConfig.ADDITIONAL.dumpDeadPlayersInventories.getAsBoolean()) return;
		if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;

		final String playerName = player.getGameProfile().getName();
		try {
			final Path file = storePlayerInventory(player, "death");
			OpenBlocks.LOG.info("Storing post-mortem inventory into {}. It can be restored with command '/ob_inventory restore {} {}'",
					file.toAbsolutePath(), playerName, stripFilename(file.getFileName().toString()));
		} catch (Exception e) {
			OpenBlocks.LOG.error("Failed to store inventory for player {}", playerName, e);
		}
	}
}
