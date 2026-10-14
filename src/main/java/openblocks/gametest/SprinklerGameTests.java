package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.SprinklerBlockEntity;
import openblocks.block.entity.TankBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class SprinklerGameTests {

	private static final BlockPos POS = new BlockPos(1, 3, 1);
	private static final BlockPos FARMLAND = new BlockPos(0, 2, 0);

	private static SprinklerBlockEntity placeSprinkler(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(FARMLAND, Blocks.FARMLAND);
		helper.setBlock(POS, OBBlocks.SPRINKLER.get());
		return helper.getBlockEntity(POS);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void waterRunsSprinklerAndMoistensFarmland(GameTestHelper helper) {
		final SprinklerBlockEntity sprinkler = placeSprinkler(helper);
		sprinkler.getInventory().insertItem(0, new ItemStack(Items.BONE_MEAL, 2), false);
		helper.assertTrue(sprinkler.getTank().fill(new FluidStack(Fluids.LAVA, 10), IFluidHandler.FluidAction.EXECUTE) == 0, "The sprinkler took lava");
		helper.assertTrue(FarmlandWaterManager.hasBlockWaterTicket(helper.getLevel(), helper.absolutePos(FARMLAND)) == false, "Farmland watered without a running sprinkler");

		// enough for one drop: on, and off again on the next water check
		sprinkler.getTank().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(sprinkler.isEnabled(), "Expected the sprinkler to run with water");
			helper.assertTrue(FarmlandWaterManager.hasBlockWaterTicket(helper.getLevel(), helper.absolutePos(FARMLAND)), "Expected the farmland next to the sprinkler to be watered");
			helper.assertTrue(sprinkler.getInventory().getStackInSlot(0).getCount() == 1, "Expected one bone meal to be used up");

			helper.runAfterDelay(20, () -> {
				helper.assertFalse(sprinkler.isEnabled(), "Expected the sprinkler to stop without water");
				helper.assertFalse(FarmlandWaterManager.hasBlockWaterTicket(helper.getLevel(), helper.absolutePos(FARMLAND)), "Farmland still watered by a dry sprinkler");
				helper.succeed();
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sprinklerDrawsWaterFromTankBelow(GameTestHelper helper) {
		helper.setBlock(POS.below(), OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(POS.below());
		tank.getFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		helper.setBlock(POS, OBBlocks.SPRINKLER.get());
		final SprinklerBlockEntity sprinkler = helper.getBlockEntity(POS);

		helper.succeedWhen(() -> {
			helper.assertTrue(sprinkler.isEnabled(), "Expected the sprinkler to run");
			helper.assertTrue(tank.getFluidHandler().getFluidInTank(0).getAmount() < 1000, "Expected water to be taken from the tank");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sprinklerOnlyTakesBoneMeal(GameTestHelper helper) {
		final SprinklerBlockEntity sprinkler = placeSprinkler(helper);
		helper.assertTrue(sprinkler.getInventory().insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "The sprinkler took dirt");
		helper.assertTrue(sprinkler.getInventory().insertItem(0, new ItemStack(Items.BONE_MEAL), false).isEmpty(), "The sprinkler refused bone meal");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sprinklerDropsWithoutSupport(GameTestHelper helper) {
		final SprinklerBlockEntity sprinkler = placeSprinkler(helper);
		sprinkler.getInventory().insertItem(0, new ItemStack(Items.BONE_MEAL, 3), false);
		helper.setBlock(POS.below(), Blocks.AIR);
		helper.assertBlockNotPresent(OBBlocks.SPRINKLER.get(), POS);
		helper.assertItemEntityPresent(OBBlocks.SPRINKLER.get().asItem(), POS, 1);
		helper.assertItemEntityPresent(Items.BONE_MEAL, POS, 1);
		helper.succeed();
	}
}
