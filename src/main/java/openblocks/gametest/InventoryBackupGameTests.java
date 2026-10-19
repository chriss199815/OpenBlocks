package openblocks.gametest;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.common.PlayerInventoryStore;
import openblocks.data.OBGameTestStructureProvider;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class InventoryBackupGameTests {

	private static final BlockPos POS = new BlockPos(1, 3, 1);

	private static String idOf(Path file) {
		return PlayerInventoryStore.stripFilename(file.getFileName().toString());
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void storeAndRestore(GameTestHelper helper) throws Exception {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().add(new ItemStack(Items.DIAMOND, 5));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

		final Path file = PlayerInventoryStore.storePlayerInventory(player, "command");
		final String id = idOf(file);
		helper.assertTrue(PlayerInventoryStore.getMatchedDumps(player.server, "").contains(id), "Backup " + id + " should be listed");

		player.getInventory().clearContent();
		player.getInventory().add(new ItemStack(Items.DIRT));

		helper.assertTrue(PlayerInventoryStore.restoreInventory(player, "inventory-" + id + ".dat"), "Full file name should be accepted");
		helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 5, "Diamonds should be restored");
		helper.assertTrue(player.getInventory().countItem(Items.DIRT) == 0, "Inventory should be replaced, not merged");
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Armor should be restored");
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD), "Offhand should be restored");

		helper.assertFalse(PlayerInventoryStore.restoreInventory(player, "does-not-exist"), "Unknown backup should fail");
		helper.assertTrue(PlayerInventoryStore.loadInventories(player.server, "../level") == null, "Ids must not leave the data folder");

		player.server.getPlayerList().remove(player);
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spawnCommandDropsItems(GameTestHelper helper) throws Exception {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().setItem(3, new ItemStack(Items.EMERALD, 7));
		player.getInventory().setItem(4, new ItemStack(Items.GOLD_INGOT, 2));
		final String id = idOf(PlayerInventoryStore.storePlayerInventory(player, "command"));
		player.server.getPlayerList().remove(player);

		final MinecraftServer server = helper.getLevel().getServer();
		final Vec3 pos = helper.absoluteVec(Vec3.atCenterOf(POS));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(helper.getLevel()).withPosition(pos), "ob_inventory spawn " + id + " main 3");

		helper.succeedWhen(() -> {
			final List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(2));
			helper.assertTrue(items.size() == 1 && items.get(0).getItem().is(Items.EMERALD) && items.get(0).getItem().getCount() == 7, "Expected only the emeralds from slot 3, got " + items);
		});
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void deathStoresInventoryAndGrave(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(POS));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.getInventory().add(new ItemStack(Items.DIAMOND, 3));

		final MinecraftServer server = player.server;
		final Set<String> before = Set.copyOf(PlayerInventoryStore.getMatchedDumps(server, ""));

		player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);

		helper.succeedWhen(() -> {
			final List<String> added = PlayerInventoryStore.getMatchedDumps(server, "").stream().filter(id -> !before.contains(id)).toList();
			helper.assertTrue(added.stream().anyMatch(id -> id.contains("-death-")), "Expected a death backup, got " + added);
			helper.assertTrue(added.stream().anyMatch(id -> id.contains("-grave-")), "Expected a grave backup, got " + added);
			server.getPlayerList().remove(player);
		});
	}
}
