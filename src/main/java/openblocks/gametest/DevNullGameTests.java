package openblocks.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.DevNullItem;
import openblocks.menu.DevNullMenu;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class DevNullGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);

	private static ItemStack devNullWith(ItemStack contained) {
		final ItemStack devNull = new ItemStack(OBItems.DEV_NULL.get());
		DevNullItem.setContained(devNull, contained);
		return devNull;
	}

	/**
	 * A /dev/null nested {@code depth} deep (1 is an empty one).
	 */
	private static ItemStack nested(int depth) {
		ItemStack stack = new ItemStack(OBItems.DEV_NULL.get());
		for (int i = 1; i < depth; i++)
			stack = devNullWith(stack);
		return stack;
	}

	private static ItemEntity dropAt(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
		final ItemEntity item = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), stack);
		helper.getLevel().addFreshEntity(item);
		return item;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void pickupFillsDevNullAndVoidsRest(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().setItem(0, devNullWith(new ItemStack(Items.DIRT, 60)));

		final ItemEntity item = dropAt(helper, player, new ItemStack(Items.DIRT, 10));
		item.playerTouch(player);

		helper.assertTrue(DevNullItem.getContained(player.getInventory().getItem(0)).getCount() == 64, "The /dev/null should be full");
		helper.assertFalse(item.isAlive(), "The rest should be voided");
		helper.assertTrue(player.getInventory().countItem(Items.DIRT) == 0, "No dirt should end up in the inventory");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void pickupIgnoresOtherItems(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().setItem(0, devNullWith(new ItemStack(Items.DIRT, 1)));

		dropAt(helper, player, new ItemStack(Items.COBBLESTONE, 5)).playerTouch(player);

		helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 5, "Other items should be picked up normally");
		helper.assertTrue(DevNullItem.getContained(player.getInventory().getItem(0)).getCount() == 1, "The /dev/null should be unchanged");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void devNullPlacesContainedBlock(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.STONE);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final ItemStack devNull = devNullWith(new ItemStack(Items.OAK_PLANKS, 5));
		player.setItemInHand(InteractionHand.MAIN_HAND, devNull);

		helper.placeAt(player, devNull, GROUND, Direction.UP);

		helper.assertBlockPresent(Blocks.OAK_PLANKS, GROUND.above());
		helper.assertTrue(player.getMainHandItem() == devNull, "The player should still hold the /dev/null");
		helper.assertTrue(DevNullItem.getContained(devNull).getCount() == 4, "Placing should use one plank from the /dev/null");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void devNullMenuStoresItemAndLocksOwnSlot(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(GROUND)));
		final ItemStack devNull = new ItemStack(OBItems.DEV_NULL.get());
		player.getInventory().setItem(0, devNull);

		final DevNullMenu menu = new DevNullMenu(1, player.getInventory(), 0);
		final Slot slot = menu.getSlot(0);
		slot.set(new ItemStack(Items.DIRT, 12));
		helper.assertTrue(DevNullItem.getContained(devNull).getCount() == 12, "The slot should be stored on the item");

		final int protectedSlot = menu.slots.stream().filter(s -> s.container == player.getInventory() && s.getContainerSlot() == 0).findFirst().orElseThrow().index;
		helper.assertFalse(menu.getSlot(protectedSlot).isActive(), "The /dev/null's own slot should be hidden");
		menu.clicked(protectedSlot, 0, ClickType.PICKUP, player);
		menu.clicked(1, 0, ClickType.SWAP, player);
		helper.assertTrue(player.getInventory().getItem(0) == devNull && menu.getCarried().isEmpty(), "The /dev/null must not be moved");

		helper.assertTrue(menu.stillValid(player), "The menu should stay open while the /dev/null is held");
		player.getInventory().setItem(0, ItemStack.EMPTY);
		helper.assertFalse(menu.stillValid(player), "The menu should close when the /dev/null is gone");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void nestingFiveDeepGrantsAdvancement(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		final ItemStack devNull = new ItemStack(OBItems.DEV_NULL.get());
		player.getInventory().setItem(0, devNull);

		final DevNullMenu menu = new DevNullMenu(1, player.getInventory(), 0);
		final Slot slot = menu.getSlot(0);
		helper.assertFalse(slot.mayPlace(nested(DevNullItem.STACK_LIMIT)), "Nesting deeper than the limit should be refused");
		helper.assertTrue(slot.mayPlace(nested(DevNullItem.STACK_LIMIT - 1)), "Nesting up to the limit should be allowed");

		slot.set(nested(DevNullItem.STACK_LIMIT - 1));
		final DevNullItem.Contents contents = DevNullItem.getContents(devNull);
		helper.assertTrue(contents.depth() == DevNullItem.STACK_LIMIT && contents.stack().isEmpty(), "Expected the overflow depth, got " + contents);

		final AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(OpenBlocks.location("tma2"));
		helper.assertTrue(advancement != null, "The Stack Overflow advancement should be loaded");
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "Stack Overflow should be granted");
		helper.succeed();
	}
}
