package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.FanBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class FanGameTests {

	private static final BlockPos FAN = new BlockPos(1, 2, 1);
	// angle 180 blows towards +z
	private static final float SOUTH = 180;

	private static FanBlockEntity placeFan(GameTestHelper helper) {
		for (int x = 0; x < 3; x++)
			for (int z = 0; z < 3; z++)
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		helper.setBlock(FAN, OBBlocks.FAN.get());
		final FanBlockEntity fan = helper.getBlockEntity(FAN);
		fan.setAngle(SOUTH);
		return fan;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void poweredFanBlowsItems(GameTestHelper helper) {
		final FanBlockEntity fan = placeFan(helper);
		helper.setBlock(FAN.west(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(fan.getPower() == 15, "Expected power 15, got " + fan.getPower());

		final ItemEntity item = helper.spawnItem(Items.STICK, 1.5f, 2, 2.5f);
		final double startZ = item.getZ();
		helper.succeedWhen(() -> helper.assertTrue(item.getZ() > startZ + 0.25, "Item was not blown south, moved " + (item.getZ() - startZ)));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void unpoweredFanIsIdle(GameTestHelper helper) {
		placeFan(helper);
		final ItemEntity item = helper.spawnItem(Items.STICK, 1.5f, 2, 2.5f);
		final double startZ = item.getZ();
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(Math.abs(item.getZ() - startZ) < 0.01, "Unpowered fan moved the item by " + (item.getZ() - startZ));
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void itemsBehindFanStay(GameTestHelper helper) {
		final FanBlockEntity fan = placeFan(helper);
		fan.setAngle(0);
		helper.setBlock(FAN.west(), Blocks.REDSTONE_BLOCK);
		final ItemEntity item = helper.spawnItem(Items.STICK, 1.5f, 2, 2.5f);
		final double startZ = item.getZ();
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(Math.abs(item.getZ() - startZ) < 0.01, "Item behind the fan moved by " + (item.getZ() - startZ));
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void clickTurnsFan(GameTestHelper helper) {
		final FanBlockEntity fan = placeFan(helper);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(FAN, player);
		helper.assertTrue(fan.getAngle() == -170, "Expected -170° after a click, got " + fan.getAngle());

		player.setShiftKeyDown(true);
		helper.useBlock(FAN, player);
		helper.useBlock(FAN, player);
		helper.assertTrue(fan.getAngle() == 170, "Expected 170° after turning back, got " + fan.getAngle());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void placedFanFacesPlayer(GameTestHelper helper) {
		helper.setBlock(FAN.below(), Blocks.STONE);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setYHeadRot(90);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBBlocks.FAN.get()));
		helper.placeAt(player, player.getMainHandItem(), FAN.below(), Direction.UP);
		final FanBlockEntity fan = helper.getBlockEntity(FAN);
		helper.assertTrue(fan.getAngle() == 90, "Expected the head yaw 90°, got " + fan.getAngle());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void fanHeldBySideBlock(GameTestHelper helper) {
		placeFan(helper);
		helper.setBlock(FAN.east(), Blocks.STONE);
		helper.setBlock(FAN.below(), Blocks.AIR);
		helper.assertBlockPresent(OBBlocks.FAN.get(), FAN);

		helper.setBlock(FAN.east(), Blocks.AIR);
		helper.assertBlockNotPresent(OBBlocks.FAN.get(), FAN);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void fanDropsWithoutSupport(GameTestHelper helper) {
		placeFan(helper);
		helper.setBlock(FAN.below(), Blocks.AIR);
		helper.assertBlockNotPresent(OBBlocks.FAN.get(), FAN);
		helper.assertItemEntityPresent(OBBlocks.FAN.get().asItem(), FAN, 1);
		helper.succeed();
	}
}
