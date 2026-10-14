package openblocks.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.common.SleepingBagHandler;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBItems;

/**
 * Uses fake players, since mock players can't receive the attachment sync payload. The tests change the world time, so each runs in
 * its own batch; {@code Level.isDay()} only follows the time on the next tick.
 */
@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class SleepingBagGameTests {

	private static final long NIGHT = 18000;
	private static final long DAY = 6000;

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	private static void atTime(GameTestHelper helper, long time, Consumer<FakePlayer> test) {
		final long previousTime = helper.getLevel().getDayTime();
		helper.getLevel().setDayTime(time);
		// thunderstorms allow sleeping during the day
		helper.getLevel().setWeatherParameters(6000, 0, false, false);

		final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sleeping-bag-test"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SLEEPING_BAG.get()));

		helper.runAfterDelay(1, () -> {
			try {
				test.accept(player);
			} finally {
				helper.getLevel().setDayTime(previousTime);
			}
		});
	}

	private static void sleepAt(GameTestHelper helper, FakePlayer player, BlockPos pos) {
		final Vec3 abs = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
		player.moveTo(abs.x, abs.y, abs.z);
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, batch = "sleeping_bag_night")
	public static void sleepsAtNightWithoutSettingSpawn(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		atTime(helper, NIGHT, player -> {
			sleepAt(helper, player, POS);

			helper.assertTrue(player.isSleeping(), "Player must sleep at night");
			helper.assertTrue(SleepingBagHandler.isInSleepingBag(player), "Player must be in the sleeping bag");
			helper.assertTrue(player.getRespawnPosition() == null, "Spawn point must not change, got " + player.getRespawnPosition());
			helper.assertTrue(EventHooks.canEntityContinueSleeping(player, Player.BedSleepingProblem.NOT_POSSIBLE_HERE), "Missing bed must not wake the player");

			player.stopSleepInBed(true, true);
			helper.assertFalse(SleepingBagHandler.isInSleepingBag(player), "Waking up must leave the sleeping bag");
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, batch = "sleeping_bag_day")
	public static void cannotSleepDuringDay(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		atTime(helper, DAY, player -> {
			sleepAt(helper, player, POS);
			helper.assertFalse(player.isSleeping(), "Player must not sleep during the day");
			helper.assertFalse(SleepingBagHandler.isInSleepingBag(player), "Sleeping bag flag must not stay set");
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, batch = "sleeping_bag_air")
	public static void cannotSleepInMidAir(GameTestHelper helper) {
		atTime(helper, NIGHT, player -> {
			sleepAt(helper, player, POS.above(2));
			helper.assertFalse(player.isSleeping(), "Player must not sleep without ground");
			helper.succeed();
		});
	}
}
