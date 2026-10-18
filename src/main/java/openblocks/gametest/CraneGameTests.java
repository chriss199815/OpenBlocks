package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.common.CraneState;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.Magnet;
import openblocks.entity.MountedBlock;
import openblocks.registry.OBEntities;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class CraneGameTests {

	private static final BlockPos TARGET = new BlockPos(1, 1, 1);

	/**
	 * A magnet right above the target, not added to the level (it would drop everything without a real owner).
	 */
	private static Magnet magnetAbove(GameTestHelper helper, BlockPos pos) {
		final Magnet magnet = new Magnet(OBEntities.MAGNET.get(), helper.getLevel());
		final Vec3 above = Vec3.atBottomCenterOf(helper.absolutePos(pos.above()));
		magnet.setPos(above.x, above.y + 0.1, above.z);
		return magnet;
	}

	/**
	 * Moves the magnet down, so what it carries hangs at the given position.
	 */
	private static void lowerTo(GameTestHelper helper, Magnet magnet, BlockPos pos) {
		final Vec3 bottom = Vec3.atBottomCenterOf(helper.absolutePos(pos));
		magnet.setPos(bottom.x, bottom.y + 0.01, bottom.z);
		magnet.positionRider(magnet.getFirstPassenger());
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void magnetCarriesChestWithContents(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.CHEST);
		final ChestBlockEntity chest = helper.getBlockEntity(TARGET);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Magnet magnet = magnetAbove(helper, TARGET);

		helper.assertTrue(magnet.toggle(player), "The magnet should pick up the chest");
		helper.assertBlockPresent(Blocks.AIR, TARGET);
		helper.assertTrue(magnet.getFirstPassenger() instanceof MountedBlock block && block.getBlockState().is(Blocks.CHEST), "The chest should hang on the magnet");
		helper.assertEntityNotPresent(EntityType.ITEM);

		lowerTo(helper, magnet, TARGET);
		helper.assertTrue(magnet.toggle(player), "The magnet should release the chest");
		helper.assertFalse(magnet.isVehicle(), "The chest should be released");
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.CHEST, TARGET);
			final ChestBlockEntity placed = helper.getBlockEntity(TARGET);
			helper.assertTrue(placed.getItem(0).is(Items.DIAMOND) && placed.getItem(0).getCount() == 3, "The chest should keep its contents");
			helper.assertEntityNotPresent(OBEntities.MOUNTED_BLOCK.get());
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void blockIsNotReleasedIntoOtherBlocks(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.STONE);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Magnet magnet = magnetAbove(helper, TARGET);
		helper.assertTrue(magnet.toggle(player), "The magnet should pick up stone");

		lowerTo(helper, magnet, TARGET);
		helper.setBlock(TARGET, Blocks.DIRT);
		helper.assertFalse(magnet.toggle(player), "The stone should not be released where a block is");
		helper.assertTrue(magnet.isVehicle(), "The stone should still hang on the magnet");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void magnetLeavesImmovableBlocks(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(TARGET, Blocks.BEDROCK);
		helper.assertFalse(magnetAbove(helper, TARGET).toggle(player), "Bedrock should stay");
		helper.setBlock(TARGET, Blocks.FURNACE);
		helper.assertTrue(magnetAbove(helper, TARGET).toggle(player), "Furnaces are in the tag");
		helper.setBlock(TARGET, Blocks.BARREL);
		helper.assertFalse(magnetAbove(helper, TARGET).toggle(player), "Barrels have a block entity and are not in the tag");
		helper.setBlock(TARGET, Blocks.WATER);
		helper.assertFalse(magnetAbove(helper, TARGET).toggle(player), "Water should stay");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void magnetPicksEntitiesBeforeBlocks(GameTestHelper helper) {
		helper.setBlock(TARGET.below(), Blocks.STONE);
		final Pig pig = helper.spawn(EntityType.PIG, TARGET);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Magnet magnet = magnetAbove(helper, TARGET);
		magnet.setPos(magnet.getX(), pig.getY() + pig.getBbHeight() + 0.1, magnet.getZ());

		helper.assertTrue(magnet.toggle(player), "The magnet should pick up the pig");
		helper.assertTrue(pig.getVehicle() == magnet, "The pig should hang on the magnet");
		helper.assertTrue(magnet.getBbHeight() > Magnet.SIZE + pig.getBbHeight() - 1e-3, "The magnet should grow by the pig");
		helper.assertBlockPresent(Blocks.STONE, TARGET.below());

		helper.assertTrue(magnet.toggle(player), "The magnet should release the pig");
		helper.assertFalse(pig.isPassenger(), "The pig should be released");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void craneLengthIsLimited(GameTestHelper helper) {
		final CraneState state = new CraneState();
		state.extending = true;
		for (int i = 0; i < 200; i++) state.updateLength();
		helper.assertTrue(state.length == CraneState.MAX_LENGTH, "The line should end at its maximum length, was " + state.length);
		state.extending = false;
		for (int i = 0; i < 200; i++) state.updateLength();
		helper.assertTrue(state.length == CraneState.MIN_LENGTH, "The line should end at its minimum length, was " + state.length);
		helper.assertTrue(state.tryToggle(100) && !state.tryToggle(103) && state.tryToggle(106), "Clicks within five ticks should count once");
		helper.succeed();
	}
}
