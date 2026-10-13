package openblocks.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.common.HangGliderHandler;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBItems;

/**
 * Uses fake players, since mock players can't receive the attachment sync payload. Fake players aren't ticked by the server,
 * so the glider logic is called directly.
 */
@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class HangGliderGameTests {

	private static FakePlayer playerWithGlider(GameTestHelper helper, double y) {
		final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "glider-test"));
		final Vec3 pos = helper.absoluteVec(new Vec3(1.5, y, 1.5));
		player.moveTo(pos.x, pos.y, pos.z);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.HANG_GLIDER.get()));
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		return player;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void useTogglesGlider(GameTestHelper helper) {
		final FakePlayer player = playerWithGlider(helper, 1);
		helper.assertTrue(HangGliderHandler.getGliderHand(player) == InteractionHand.MAIN_HAND, "Expected glider on back");
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertFalse(HangGliderHandler.hasGlider(player), "Second use must take the glider off");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void gliderRemovedWhenItemLeavesHand(GameTestHelper helper) {
		final FakePlayer player = playerWithGlider(helper, 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		HangGliderHandler.tick(player);
		helper.assertFalse(HangGliderHandler.hasGlider(player), "Glider must be removed without the item in hand");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void deployedGliderSlowsFall(GameTestHelper helper) {
		final FakePlayer player = playerWithGlider(helper, 6);
		player.setOnGround(false);
		player.setDeltaMovement(0, -1.0, 0);
		player.fallDistance = 10;

		HangGliderHandler.tick(player);

		helper.assertTrue(HangGliderHandler.isDeployed(player), "Glider must be deployed in the air");
		helper.assertTrue(player.getDeltaMovement().y > -0.2, "Expected slow sink, got " + player.getDeltaMovement().y);
		helper.assertTrue(player.fallDistance == 0, "Fall distance must be reset, got " + player.fallDistance);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void gliderOnGroundIsNotDeployed(GameTestHelper helper) {
		final FakePlayer player = playerWithGlider(helper, 1);
		player.setOnGround(true);
		player.setDeltaMovement(0, -1.0, 0);

		HangGliderHandler.tick(player);

		helper.assertFalse(HangGliderHandler.isDeployed(player), "Glider must stay folded on the ground");
		helper.assertTrue(player.getDeltaMovement().y == -1.0, "Motion must be untouched on the ground");
		helper.succeed();
	}
}
