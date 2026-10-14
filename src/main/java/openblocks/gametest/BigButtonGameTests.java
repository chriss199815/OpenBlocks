package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.BigButtonBlock;
import openblocks.block.entity.BigButtonBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class BigButtonGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final BlockPos POS = GROUND.above();

	private static BigButtonBlockEntity placeButton(GameTestHelper helper, Block block) {
		helper.setBlock(GROUND, Blocks.STONE);
		helper.setBlock(POS, block.defaultBlockState().setValue(BigButtonBlock.FACE, AttachFace.FLOOR));
		return helper.getBlockEntity(POS);
	}

	private static void assertPowered(GameTestHelper helper, boolean powered) {
		helper.assertBlockProperty(POS, BigButtonBlock.POWERED, powered);
		// strong signal into the block it is attached to
		helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(GROUND.east())) == powered, "Expected the attached block to be " + (powered? "powered" : "unpowered"));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void emptyButtonPopsAfterOneTick(GameTestHelper helper) {
		placeButton(helper, OBBlocks.BIG_BUTTON.get());
		helper.useBlock(POS);
		assertPowered(helper, true);
		helper.runAfterDelay(2, () -> {
			assertPowered(helper, false);
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void itemsExtendPressTime(GameTestHelper helper) {
		final BigButtonBlockEntity button = placeButton(helper, OBBlocks.BIG_BUTTON.get());
		button.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 20));
		button.getInventory().setStackInSlot(5, new ItemStack(Items.DIRT, 10));
		helper.assertTrue(button.getTickTime() == 30, "Expected 30 ticks, got " + button.getTickTime());

		helper.useBlock(POS);
		helper.runAfterDelay(25, () -> {
			assertPowered(helper, true);
			helper.runAfterDelay(10, () -> {
				assertPowered(helper, false);
				helper.succeed();
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void arrowPressesWoodenButton(GameTestHelper helper) {
		placeButton(helper, OBBlocks.BIG_BUTTON_WOOD.get());
		final Arrow arrow = helper.spawn(EntityType.ARROW, Vec3.atBottomCenterOf(POS.above(3)));
		arrow.setDeltaMovement(0, -1, 0);
		helper.succeedWhen(() -> assertPowered(helper, true));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void arrowDoesNotPressStoneButton(GameTestHelper helper) {
		placeButton(helper, OBBlocks.BIG_BUTTON.get());
		final Arrow arrow = helper.spawn(EntityType.ARROW, Vec3.atBottomCenterOf(POS.above(3)));
		arrow.setDeltaMovement(0, -1, 0);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(arrow.isAlive() && arrow.blockPosition().equals(helper.absolutePos(POS)), "The arrow should be stuck in the button");
			assertPowered(helper, false);
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void buttonDropsItsInventory(GameTestHelper helper) {
		final BigButtonBlockEntity button = placeButton(helper, OBBlocks.BIG_BUTTON.get());
		button.getInventory().setStackInSlot(3, new ItemStack(Items.COBBLESTONE, 7));
		helper.getLevel().destroyBlock(helper.absolutePos(POS), true);
		helper.assertItemEntityCountIs(Items.COBBLESTONE, POS, 1, 7);
		helper.assertItemEntityCountIs(OBBlocks.BIG_BUTTON.get().asItem(), POS, 1, 1);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void wallButtonPowersBlockBehind(GameTestHelper helper) {
		helper.setBlock(POS.south(), Blocks.STONE);
		helper.setBlock(POS, OBBlocks.BIG_BUTTON.get().defaultBlockState().setValue(BigButtonBlock.FACE, AttachFace.WALL).setValue(BigButtonBlock.FACING, Direction.NORTH));
		helper.useBlock(POS);
		helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(POS.south().east())), "Expected the wall behind the button to be powered");
		helper.succeed();
	}
}
