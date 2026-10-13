package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
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
		bottler.getAutomationHandler().insertItem(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE, 5), false);

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

		final ItemStack rejected = bottler.getAutomationHandler().insertItem(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.DIRT), false);
		helper.assertTrue(rejected.getCount() == 1, "Only glass bottles may be inserted");

		bottler.getInventory().setStackInSlot(XpBottlerBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(bottler.getAutomationHandler().extractItem(XpBottlerBlockEntity.SLOT_INPUT, 1, false).isEmpty(), "Input slot must not be extractable");

		helper.assertTrue(bottler.getFluidHandler().drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Tank must be fill-only");
		helper.succeed();
	}
}
