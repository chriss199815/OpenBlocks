package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import openblocks.OpenBlocks;
import openblocks.block.entity.TankBlockEntity;
import openblocks.block.entity.XpBottlerBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class XpBottlerGameTests {

	private static final BlockPos BOTTLER = new BlockPos(1, 1, 1);
	private static final BlockPos TANK = BOTTLER.above();

	// two bottles take 80 ticks of work plus the time to pull the fluid
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 200)
	public static void bottlesXpFromAdjacentTank(GameTestHelper helper) {
		helper.setBlock(TANK, OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(TANK);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(2 * XpBottlerBlockEntity.XP_PER_BOTTLE + 3), IFluidHandler.FluidAction.EXECUTE);

		helper.setBlock(BOTTLER, OBBlocks.XP_BOTTLER.get());
		final XpBottlerBlockEntity bottler = helper.getBlockEntity(BOTTLER);
		bottler.getItemHandler(null).insertItem(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE, 5), false);

		helper.succeedWhen(() -> {
			final ItemStack output = bottler.getInventory().getStackInSlot(XpBottlerBlockEntity.SLOT_OUTPUT);
			helper.assertTrue(output.is(Items.EXPERIENCE_BOTTLE) && output.getCount() == 2, "Expected 2 XP bottles, got " + output);
			helper.assertTrue(bottler.getInventory().getStackInSlot(XpBottlerBlockEntity.SLOT_INPUT).getCount() == 3, "Expected 3 glass bottles left");
			// the remaining 3 XP aren't enough for another bottle
			helper.assertTrue(tank.getFluid().isEmpty(), "Expected tank drained into bottler");
			helper.assertTrue(bottler.getFluid().getAmount() == FluidXpUtils.xpToFluid(3), "Expected 3 XP left in bottler, got " + bottler.getFluid().getAmount());
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void automationRules(GameTestHelper helper) {
		helper.setBlock(BOTTLER, OBBlocks.XP_BOTTLER.get());
		final XpBottlerBlockEntity bottler = helper.getBlockEntity(BOTTLER);

		final ItemStack rejected = bottler.getItemHandler(null).insertItem(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.DIRT), false);
		helper.assertTrue(rejected.getCount() == 1, "Only glass bottles may be inserted");

		bottler.getInventory().setStackInSlot(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(bottler.getItemHandler(null).extractItem(XpBottlerBlockEntity.SLOT_INPUT, 1, false).isEmpty(), "Input slot must not be extractable");

		helper.assertTrue(bottler.getFluidHandler(null).drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Tank must be fill-only");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void accessOnlyFromConfiguredSides(GameTestHelper helper) {
		helper.setBlock(BOTTLER, OBBlocks.XP_BOTTLER.get());
		final XpBottlerBlockEntity bottler = helper.getBlockEntity(BOTTLER);
		onlySide(bottler, XpBottlerBlockEntity.AutoSlot.INPUT, Direction.WEST);
		onlySide(bottler, XpBottlerBlockEntity.AutoSlot.OUTPUT, Direction.EAST);
		onlySide(bottler, XpBottlerBlockEntity.AutoSlot.XP, Direction.UP);
		bottler.getInventory().setStackInSlot(XpBottlerBlockEntity.SLOT_OUTPUT, new ItemStack(Items.EXPERIENCE_BOTTLE));

		final BlockPos pos = helper.absolutePos(BOTTLER);
		final IItemHandler west = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.WEST);
		final IItemHandler east = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.EAST);
		helper.assertTrue(west != null && east != null, "Expected item access on input and output sides");
		helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.NORTH) == null, "Expected no item access on other sides");
		helper.assertTrue(west.extractItem(XpBottlerBlockEntity.SLOT_OUTPUT, 1, true).isEmpty(), "Input side must not give XP bottles");
		helper.assertTrue(east.insertItem(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE), true).getCount() == 1, "Output side must not take glass bottles");
		helper.assertTrue(west.insertItem(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE), true).isEmpty(), "Input side must take glass bottles");
		helper.assertTrue(east.extractItem(XpBottlerBlockEntity.SLOT_OUTPUT, 1, true).is(Items.EXPERIENCE_BOTTLE), "Output side must give XP bottles");

		helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) != null, "Expected fluid access on the XP side");
		helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN) == null, "Expected no fluid access on other sides");
		helper.succeed();
	}

	// two bottles take 80 ticks of work plus the time to move bottles and fluid
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 200)
	public static void autoExtractAndEject(GameTestHelper helper) {
		final BlockPos input = BOTTLER.west();
		final BlockPos output = BOTTLER.east();
		helper.setBlock(TANK, OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(TANK);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(2 * XpBottlerBlockEntity.XP_PER_BOTTLE), IFluidHandler.FluidAction.EXECUTE);
		helper.setBlock(input, Blocks.CHEST);
		helper.setBlock(output, Blocks.CHEST);
		final ChestBlockEntity inputChest = helper.getBlockEntity(input);
		inputChest.setItem(5, new ItemStack(Items.GLASS_BOTTLE, 3));

		helper.setBlock(BOTTLER, OBBlocks.XP_BOTTLER.get());
		final XpBottlerBlockEntity bottler = helper.getBlockEntity(BOTTLER);
		onlySide(bottler, XpBottlerBlockEntity.AutoSlot.INPUT, Direction.WEST);
		onlySide(bottler, XpBottlerBlockEntity.AutoSlot.OUTPUT, Direction.EAST);
		bottler.getConfig().toggleAuto(XpBottlerBlockEntity.AutoSlot.INPUT);
		bottler.getConfig().toggleAuto(XpBottlerBlockEntity.AutoSlot.OUTPUT);

		helper.succeedWhen(() -> {
			// not assertContainerContains, which expects exactly one item
			final ChestBlockEntity outputChest = helper.getBlockEntity(output);
			helper.assertTrue(outputChest.countItem(Items.EXPERIENCE_BOTTLE) == 2, "Expected 2 XP bottles in the output chest");
			// the third one may already have been pulled in, but not bottled for lack of XP
			final int left = inputChest.countItem(Items.GLASS_BOTTLE) + bottler.getInventory().getStackInSlot(XpBottlerBlockEntity.SLOT_INPUT).getCount();
			helper.assertTrue(left == 1, "Expected 1 glass bottle left, got " + left);
		});
	}

	private static void onlySide(XpBottlerBlockEntity bottler, XpBottlerBlockEntity.AutoSlot slot, Direction keep) {
		for (Direction side : Direction.values())
			if (side != keep) bottler.getConfig().toggleSide(slot, side);
	}
}
