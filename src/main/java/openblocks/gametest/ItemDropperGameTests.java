package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.ItemDropperBlock;
import openblocks.block.entity.ItemDropperBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class ItemDropperGameTests {

	private static final BlockPos DROPPER = new BlockPos(1, 2, 1);

	private static ItemDropperBlockEntity setUp(GameTestHelper helper, Direction facing) {
		helper.setBlock(DROPPER, OBBlocks.ITEM_DROPPER.get().defaultBlockState().setValue(ItemDropperBlock.FACING, facing));
		return helper.getBlockEntity(DROPPER);
	}

	private static List<ItemEntity> droppedItems(GameTestHelper helper) {
		// only this test's area, tests next to it may drop items too
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void dropsOneItemPerPulseStraightOut(GameTestHelper helper) {
		final ItemDropperBlockEntity dropper = setUp(helper, Direction.UP);
		dropper.getInventory().setStackInSlot(4, new ItemStack(Items.DIAMOND, 3));
		dropper.setSpeed(10);

		helper.setBlock(DROPPER.below(), Blocks.REDSTONE_BLOCK);
		final List<ItemEntity> items = droppedItems(helper);
		helper.assertTrue(items.size() == 1, "Expected one dropped item, got " + items.size());
		final ItemEntity item = items.get(0);
		helper.assertTrue(item.getItem().is(Items.DIAMOND) && item.getItem().getCount() == 1, "Expected a single diamond");
		helper.assertTrue(dropper.getInventory().getStackInSlot(4).getCount() == 2, "Expected 2 diamonds left");

		// centered in the block in front, moving straight out at full speed
		final Vec3 expected = Vec3.atCenterOf(helper.absolutePos(DROPPER.above())).subtract(0, EntityType.ITEM.getHeight() / 2, 0);
		helper.assertTrue(item.position().distanceTo(expected) < 1e-6, "Expected item at " + expected + ", got " + item.position());
		helper.assertTrue(item.getDeltaMovement().distanceTo(new Vec3(0, 1, 0)) < 1e-6, "Expected velocity (0, 1, 0), got " + item.getDeltaMovement());

		// a neighbour update while still powered must not drop again
		helper.setBlock(DROPPER.east(), Blocks.STONE);
		helper.assertTrue(droppedItems(helper).size() == 1, "Expected no second drop without a new pulse");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void scalesSpeedByRedstoneStrength(GameTestHelper helper) {
		final ItemDropperBlockEntity dropper = setUp(helper, Direction.EAST);
		dropper.getInventory().setStackInSlot(0, new ItemStack(Items.STONE));
		dropper.setSpeed(30);
		dropper.toggleUseRedstoneStrength();

		// signal 5 of 15 scales speed 3 down to 1
		dropper.dropItem(5);

		final List<ItemEntity> items = droppedItems(helper);
		helper.assertTrue(items.size() == 1, "Expected one dropped item, got " + items.size());
		helper.assertTrue(items.get(0).getDeltaMovement().distanceTo(new Vec3(1, 0, 0)) < 1e-6, "Expected velocity (1, 0, 0), got " + items.get(0).getDeltaMovement());
		helper.succeed();
	}
}
