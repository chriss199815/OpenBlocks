package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.Luggage;
import openblocks.item.LuggageItem;
import openblocks.registry.OBEntities;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class LuggageGameTests {

	private static void placeFloor(GameTestHelper helper) {
		for (int x = 0; x < 3; x++)
			for (int z = 0; z < 3; z++)
				helper.setBlock(x, 1, z, Blocks.STONE);
	}

	private static Luggage spawnLuggage(GameTestHelper helper, BlockPos pos) {
		return helper.spawn(OBEntities.LUGGAGE.get(), pos);
	}

	private static int count(Luggage luggage, Item item) {
		return luggage.getInventory().countItem(item);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void luggageCollectsItems(GameTestHelper helper) {
		placeFloor(helper);
		final Luggage luggage = spawnLuggage(helper, new BlockPos(0, 2, 1));
		helper.spawnItem(Items.DIAMOND, 2.5f, 2, 1.5f);
		helper.succeedWhen(() -> helper.assertTrue(count(luggage, Items.DIAMOND) == 1, "The luggage should have eaten the diamond"));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sneakClickTurnsLuggageIntoItem(GameTestHelper helper) {
		placeFloor(helper);
		final Luggage luggage = spawnLuggage(helper, new BlockPos(1, 2, 1));
		luggage.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
		luggage.setCustomName(Component.literal("Bob"));

		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		luggage.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertFalse(luggage.isAlive(), "The luggage should be gone");

		final ItemStack stack = player.getInventory().getItem(0);
		helper.assertTrue(stack.is(OBItems.LUGGAGE.get()), "The player should get the luggage item, got " + stack);
		helper.assertTrue(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).getStackInSlot(5).getCount() == 3, "The item should keep the contents");
		helper.assertTrue("Bob".equals(stack.getHoverName().getString()), "The item should keep the name");

		// placing it again restores contents and name
		player.setShiftKeyDown(false);
		player.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 0.5)));
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Placing should use up the item");

		final List<Luggage> placed = helper.getLevel().getEntitiesOfClass(Luggage.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(4));
		helper.assertTrue(placed.size() == 1, "Expected one placed luggage, got " + placed.size());
		final Luggage restored = placed.get(0);
		helper.assertTrue(restored.getInventory().getItem(5).getCount() == 3, "The placed luggage should have the contents");
		helper.assertTrue("Bob".equals(restored.getCustomName().getString()), "The placed luggage should have the name");
		helper.assertTrue(player.getUUID().equals(restored.getOwnerUUID()), "The player should own the placed luggage");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void lightningMakesLuggageSpecial(GameTestHelper helper) {
		placeFloor(helper);
		final Luggage luggage = spawnLuggage(helper, new BlockPos(1, 2, 1));
		luggage.getInventory().setItem(26, new ItemStack(Items.DIAMOND));
		helper.assertFalse(luggage.isSpecial(), "New luggage should be normal");

		final LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
		luggage.thunderHit(helper.getLevel(), lightning);
		helper.assertTrue(luggage.isSpecial() && luggage.getInventory().getContainerSize() == Luggage.SIZE_SPECIAL, "Lightning should double the inventory");
		helper.assertTrue(count(luggage, Items.DIAMOND) == 1, "The contents should be kept");
		helper.assertTrue(luggage.isAlive() && !luggage.isOnFire(), "Lightning should not hurt the luggage");

		final ItemStack stack = luggage.toItem();
		helper.assertTrue(LuggageItem.isSpecial(stack) && stack.hasFoil(), "The item should be special and shiny");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void droppedLuggageWithContentsSurvives(GameTestHelper helper) {
		placeFloor(helper);
		final Luggage luggage = spawnLuggage(helper, new BlockPos(1, 2, 1));
		final Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		final ItemEntity dropped = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, luggage.toItem());
		helper.getLevel().addFreshEntity(dropped);
		luggage.discard();

		helper.assertTrue(dropped.isInvulnerable() && dropped.getAge() < 0, "Luggage with contents should neither burn nor despawn");
		helper.assertFalse(dropped.hurt(helper.getLevel().damageSources().lava(), 10), "Lava should not destroy the luggage");

		final ItemEntity empty = helper.spawnItem(OBItems.LUGGAGE.get(), 1.5f, 2, 1.5f);
		helper.assertFalse(empty.isInvulnerable(), "A new luggage item should behave like other items");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void stickMakesLuggageWait(GameTestHelper helper) {
		placeFloor(helper);
		final Luggage luggage = spawnLuggage(helper, new BlockPos(1, 2, 1));
		final Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		luggage.setOwnerUUID(owner.getUUID());
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));

		final Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		luggage.mobInteract(stranger, InteractionHand.MAIN_HAND);
		helper.assertFalse(luggage.isOrderedToSit(), "Only the owner may tell the luggage to wait");

		luggage.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(luggage.isOrderedToSit(), "A stick should make the luggage wait");
		helper.assertTrue(luggage.unableToMoveToOwner(), "A waiting luggage must neither follow nor teleport");

		// a waiting luggage does not collect items either
		helper.spawnItem(Items.DIAMOND, 2.5f, 2, 1.5f);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(count(luggage, Items.DIAMOND) == 0, "A waiting luggage should not collect items");
			luggage.mobInteract(owner, InteractionHand.MAIN_HAND);
			helper.assertFalse(luggage.isOrderedToSit(), "Another click should make it follow again");
			helper.succeed();
		});
	}
}
