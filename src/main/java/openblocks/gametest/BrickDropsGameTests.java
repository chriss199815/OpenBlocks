package openblocks.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.common.BrickDrops;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBAttachments;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class BrickDropsGameTests {

	/**
	 * Runs the test with brick drops enabled; GameTests of a batch run one after another on the server thread.
	 */
	private static void withBrickDrops(GameTestHelper helper, Runnable test) {
		final boolean serious = OBConfig.TOMFOOLERY.weAreSeriousPeople.get();
		OBConfig.TOMFOOLERY.weAreSeriousPeople.set(false);
		try {
			test.run();
		} finally {
			OBConfig.TOMFOOLERY.weAreSeriousPeople.set(serious);
		}
	}

	@SuppressWarnings("removal")
	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		player.teleportTo(pos.x, pos.y, pos.z);
		return player;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void tastyClayLetsPlayerDropBrick(GameTestHelper helper) {
		final ServerPlayer player = survivalPlayer(helper);
		withBrickDrops(helper, () -> {
			final ItemStack clay = new ItemStack(OBItems.TASTY_CLAY.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, clay);
			clay.finishUsingItem(helper.getLevel(), player);
			helper.assertValueEqual(player.getData(OBAttachments.BOWELS), 1, "bricks after eating tasty clay");

			BrickDrops.dropBrick(player);
			helper.assertValueEqual(player.getData(OBAttachments.BOWELS), 0, "bricks after dropping one");
			BrickDrops.dropBrick(player);
		});
		helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
				item -> item.getItem().is(Items.BRICK)).size(), 1, "dropped bricks");
		player.getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void seriousPeopleDropNoBricks(GameTestHelper helper) {
		final ServerPlayer player = survivalPlayer(helper);
		player.setData(OBAttachments.BOWELS, 3);
		// the default config
		BrickDrops.dropBrick(player);
		helper.assertValueEqual(player.getData(OBAttachments.BOWELS), 3, "bricks with brick drops disabled");
		helper.assertEntityNotPresent(EntityType.ITEM);
		player.getServer().getPlayerList().remove(player);
		helper.succeed();
	}
}
