package openblocks.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.Cartographer;
import openblocks.entity.CartographerData;
import openblocks.item.EmptyMapItem;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class CartographerGameTests {

	private static Cartographer spawn(GameTestHelper helper, Player player, ItemStack stack) {
		final Vec3 pos = helper.absoluteVec(new Vec3(1.5, 3, 1.5));
		player.setPos(pos.x, pos.y - player.getEyeHeight(), pos.z);
		final Cartographer cartographer = new Cartographer(helper.getLevel(), player, stack);
		cartographer.setPos(pos);
		helper.getLevel().addFreshEntity(cartographer);
		return cartographer;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 100)
	public static void mapsWithEmptyMapAndGivesItBack(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Cartographer cartographer = spawn(helper, player, new ItemStack(OBItems.CARTOGRAPHER.get()));

		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, EmptyMapItem.create(0));
		cartographer.interact(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Expected the cartographer to take the map");
		helper.assertTrue(cartographer.isMapping(), "Expected the cartographer to map");

		helper.succeedWhen(() -> {
			helper.assertTrue(cartographer.getProgress().finished().cardinality() > 0, "Expected mapped chunks");
			cartographer.interact(player, InteractionHand.MAIN_HAND);
			final ItemStack map = player.getMainHandItem();
			helper.assertTrue(map.is(OBItems.HEIGHT_MAP) && map.has(OBComponents.HEIGHT_MAP_ID), "Expected the height map back, got " + map);
			helper.assertFalse(cartographer.isMapping(), "Expected the cartographer to stop");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void keepsMapWhenHit(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Cartographer cartographer = spawn(helper, player, new ItemStack(OBItems.CARTOGRAPHER.get()));
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, EmptyMapItem.create(1));
		cartographer.interact(player, InteractionHand.MAIN_HAND);

		cartographer.hurt(helper.getLevel().damageSources().playerAttack(player), 1);
		helper.assertTrue(cartographer.isRemoved(), "Expected the cartographer to be gone");
		final List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, cartographer.getBoundingBox().inflate(2));
		helper.assertValueEqual(drops.size(), 1, "dropped items");
		final ItemStack item = drops.getFirst().getItem();
		helper.assertTrue(item.is(OBItems.CARTOGRAPHER), "Expected a cartographer item, got " + item);
		final CartographerData data = item.get(OBComponents.CARTOGRAPHER);
		helper.assertTrue(data != null && data.mapping() && data.map().is(OBItems.HEIGHT_MAP), "Expected the map in the item, got " + data);

		// released again, it carries on
		final Cartographer released = spawn(helper, player, item);
		helper.assertTrue(released.isMapping(), "Expected the released cartographer to map");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ignoresUseWithoutSneaking(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Cartographer cartographer = spawn(helper, player, new ItemStack(OBItems.CARTOGRAPHER.get()));
		player.setItemInHand(InteractionHand.MAIN_HAND, EmptyMapItem.create(0));
		cartographer.interact(player, InteractionHand.MAIN_HAND);
		helper.assertFalse(player.getMainHandItem().isEmpty(), "Expected the map to stay in hand");
		helper.assertFalse(cartographer.isMapping(), "Expected no mapping");
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, cartographer.getBoundingBox().inflate(2)).isEmpty(), "Expected no drops");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void newMapReplacesOldOne(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Cartographer cartographer = spawn(helper, player, new ItemStack(OBItems.CARTOGRAPHER.get()));
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, EmptyMapItem.create(0));
		cartographer.interact(player, InteractionHand.MAIN_HAND);

		final ItemStack secondMaps = EmptyMapItem.create(0);
		secondMaps.setCount(2);
		player.setItemInHand(InteractionHand.MAIN_HAND, secondMaps);
		cartographer.interact(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "empty maps left in hand");
		helper.assertTrue(player.getInventory().contains(stack -> stack.is(OBItems.HEIGHT_MAP)), "Expected the first map back in the inventory");
		helper.assertTrue(cartographer.isMapping(), "Expected the cartographer to map the new map");

		// with the last map in hand, the old one goes to the hand
		cartographer.interact(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().is(OBItems.HEIGHT_MAP), "Expected the second map in hand, got " + player.getMainHandItem());
		helper.succeed();
	}
}
