package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import openblocks.OpenBlocks;
import openblocks.block.entity.DrawingTableBlockEntity;
import openblocks.common.StencilPattern;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class DrawingTableGameTests {

	private static final BlockPos TABLE = new BlockPos(1, 2, 1);

	private static IItemHandler setUp(GameTestHelper helper) {
		helper.setBlock(TABLE, OBBlocks.DRAWING_TABLE.get());
		return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TABLE), Direction.UP);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void turnsUnpreparedStencilsIntoTheSelectedPattern(GameTestHelper helper) {
		final IItemHandler handler = setUp(helper);
		final DrawingTableBlockEntity table = helper.getBlockEntity(TABLE);

		helper.assertTrue(!handler.insertItem(DrawingTableBlockEntity.SLOT_INPUT, new ItemStack(Items.PAPER), false).isEmpty(), "Expected paper to be rejected");
		helper.assertTrue(handler.insertItem(DrawingTableBlockEntity.SLOT_INPUT, new ItemStack(OBItems.UNPREPARED_STENCIL.get(), 3), false).isEmpty(), "Expected unprepared stencils to be accepted");

		// like the stonecutter, one at a time
		final ItemStack preview = handler.getStackInSlot(DrawingTableBlockEntity.SLOT_OUTPUT);
		helper.assertTrue(preview.is(OBItems.STENCILS.get(StencilPattern.CREEPER_FACE).get()) && preview.getCount() == 1, "Expected 1 creeper face stencil, got " + preview);

		table.cyclePattern(1);
		final ItemStack taken = handler.extractItem(DrawingTableBlockEntity.SLOT_OUTPUT, 64, false);
		helper.assertTrue(taken.is(OBItems.STENCILS.get(StencilPattern.BORDER).get()) && taken.getCount() == 1, "Expected 1 border stencil, got " + taken);
		helper.assertTrue(table.getInput().getCount() == 2, "Expected 2 unprepared stencils left, got " + table.getInput());

		table.cyclePattern(-2);
		helper.assertTrue(table.getPattern() == StencilPattern.BALLOON, "Expected the selection to wrap around to the last pattern");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void redrawsFinishedStencils(GameTestHelper helper) {
		final IItemHandler handler = setUp(helper);
		final DrawingTableBlockEntity table = helper.getBlockEntity(TABLE);

		helper.assertTrue(handler.insertItem(DrawingTableBlockEntity.SLOT_INPUT, new ItemStack(OBItems.STENCILS.get(StencilPattern.HEART).get(), 2), false).isEmpty(), "Expected heart stencils to be accepted");
		table.cyclePattern(1);
		final ItemStack taken = handler.extractItem(DrawingTableBlockEntity.SLOT_OUTPUT, 1, false);
		helper.assertTrue(taken.is(OBItems.STENCILS.get(StencilPattern.BORDER).get()), "Expected a border stencil, got " + taken);
		helper.assertTrue(table.getInput().is(OBItems.STENCILS.get(StencilPattern.HEART).get()) && table.getInput().getCount() == 1, "Expected 1 heart stencil left, got " + table.getInput());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void dropsOnlyTheInput(GameTestHelper helper) {
		final IItemHandler handler = setUp(helper);
		handler.insertItem(DrawingTableBlockEntity.SLOT_INPUT, new ItemStack(OBItems.UNPREPARED_STENCIL.get(), 4), false);

		helper.setBlock(TABLE, Blocks.AIR);
		final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(TABLE)).inflate(2));
		helper.assertTrue(drops.size() == 1, "Expected one drop, got " + drops.size());
		final ItemStack drop = drops.get(0).getItem();
		helper.assertTrue(drop.is(OBItems.UNPREPARED_STENCIL.get()) && drop.getCount() == 4, "Expected 4 unprepared stencils, got " + drop);
		helper.succeed();
	}
}
