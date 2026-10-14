package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.CursorItem;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class CursorGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final BlockPos LEVER = GROUND.above();

	/**
	 * A lever on stone, and a survival player holding a cursor linked to it, standing {@code height} blocks above the lever.
	 */
	private static Player setUp(GameTestHelper helper, double height) {
		helper.setBlock(GROUND, Blocks.STONE);
		helper.setBlock(LEVER, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));

		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final ItemStack cursor = new ItemStack(OBItems.CURSOR.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, cursor);
		// clicks the lever, which is above the ground
		helper.placeAt(player, cursor, GROUND, Direction.UP);
		helper.assertTrue(cursor.get(OBComponents.CURSOR_TARGET.get()).pos().pos().equals(helper.absolutePos(LEVER)), "Clicking a block should link the cursor to it");
		helper.assertBlockProperty(LEVER, LeverBlock.POWERED, false);

		player.moveTo(helper.absoluteVec(new Vec3(1.5, 2.5 + height, 1.5)));
		return player;
	}

	private static void useInAir(GameTestHelper helper, Player player) {
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void cursorClicksLinkedBlock(GameTestHelper helper) {
		final Player player = setUp(helper, 2);
		useInAir(helper, player);
		helper.assertBlockProperty(LEVER, LeverBlock.POWERED, true);
		useInAir(helper, player);
		helper.assertBlockProperty(LEVER, LeverBlock.POWERED, false);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void cursorUsesDurability(GameTestHelper helper) {
		final Player player = setUp(helper, 4);
		final ItemStack cursor = player.getMainHandItem();
		useInAir(helper, player);
		helper.assertBlockProperty(LEVER, LeverBlock.POWERED, true);
		helper.assertTrue(cursor.getDamageValue() == 1, "Each remote click should cost one durability, damage is " + cursor.getDamageValue());

		cursor.setDamageValue(CursorItem.DURABILITY - 1);
		useInAir(helper, player);
		helper.assertBlockProperty(LEVER, LeverBlock.POWERED, false);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "The cursor should break when worn out");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void cursorIsRepairedWithWool(GameTestHelper helper) {
		final ItemStack cursor = new ItemStack(OBItems.CURSOR.get());
		helper.assertTrue(cursor.getItem().isValidRepairItem(cursor, new ItemStack(Items.WHITE_WOOL)), "Wool should repair the cursor");
		helper.assertFalse(cursor.getItem().isValidRepairItem(cursor, new ItemStack(Items.STICK)), "Only wool should repair the cursor");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void cursorDoesNotReachTooFar(GameTestHelper helper) {
		// legacy limit: squared distance below 64
		final Player player = setUp(helper, 9);
		player.getAbilities().instabuild = true;
		useInAir(helper, player);
		helper.assertBlockProperty(LEVER, LeverBlock.POWERED, false);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void cursorOpensChestFromAfar(GameTestHelper helper) {
		helper.setBlock(GROUND, Blocks.CHEST);
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		final ItemStack cursor = new ItemStack(OBItems.CURSOR.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, cursor);
		helper.placeAt(player, cursor, GROUND.below(), Direction.UP);

		player.moveTo(helper.absoluteVec(new Vec3(1.5, 7, 1.5)));
		player.gameMode.useItem(player, helper.getLevel(), cursor, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.containerMenu instanceof ChestMenu, "Expected an open chest, got " + player.containerMenu);
		helper.assertTrue(player.containerMenu.stillValid(player), "The chest should stay open");
		helper.succeed();
	}
}
