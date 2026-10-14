package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import openblocks.OpenBlocks;
import openblocks.block.entity.TankBlockEntity;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class VacuumHopperGameTests {

	private static final BlockPos HOPPER = new BlockPos(1, 1, 1);

	private static VacuumHopperBlockEntity setUp(GameTestHelper helper) {
		helper.setBlock(HOPPER, OBBlocks.VACUUM_HOPPER.get());
		return helper.getBlockEntity(HOPPER);
	}

	// floating entities, so they only reach the hopper through its suction
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void pullsInItemsAndOrbs(GameTestHelper helper) {
		final VacuumHopperBlockEntity hopper = setUp(helper);
		final Vec3 pos = helper.absoluteVec(Vec3.atCenterOf(HOPPER).add(0, 2.5, 0));

		final ItemEntity item = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.DIAMOND, 5));
		item.setNoGravity(true);
		item.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(item);

		// three merged orbs of 4 XP each
		final ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), pos.x, pos.y, pos.z, 4);
		orb.count = 3;
		orb.setNoGravity(true);
		orb.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(orb);

		helper.succeedWhen(() -> {
			helper.assertTrue(item.isRemoved() && orb.isRemoved(), "Expected item and orb to be collected");
			helper.assertTrue(hopper.getInventory().getStackInSlot(0).is(Items.DIAMOND) && hopper.getInventory().getStackInSlot(0).getCount() == 5, "Expected 5 diamonds in the hopper");
			helper.assertTrue(hopper.getFluid().getAmount() == FluidXpUtils.xpToFluid(12), "Expected 12 XP in the tank, got " + hopper.getFluid().getAmount());
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ejectsItemsOnlyToOutputSides(GameTestHelper helper) {
		final VacuumHopperBlockEntity hopper = setUp(helper);
		final BlockPos output = HOPPER.east();
		final BlockPos other = HOPPER.west();
		helper.setBlock(output, Blocks.CHEST);
		helper.setBlock(other, Blocks.CHEST);
		hopper.getInventory().setStackInSlot(3, new ItemStack(Items.COBBLESTONE, 10));
		hopper.toggleOutput(VacuumHopperBlockEntity.Output.ITEMS, Direction.EAST);

		final IItemHandler east = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(HOPPER), Direction.EAST);
		final IItemHandler west = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(HOPPER), Direction.WEST);
		helper.assertTrue(east != null && west == null, "Expected item access only on the output side");
		helper.assertTrue(east.insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "Output side must not accept items");

		helper.succeedWhen(() -> {
			// not assertContainerContains, which expects exactly one item
			final ChestBlockEntity chest = helper.getBlockEntity(output);
			helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 10, "Expected 10 cobblestone in the output chest");
			helper.assertContainerEmpty(other);
			helper.assertTrue(hopper.getInventory().getStackInSlot(3).isEmpty(), "Expected the hopper to be empty");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void ejectsXpToOutputSide(GameTestHelper helper) {
		final VacuumHopperBlockEntity hopper = setUp(helper);
		final BlockPos tankPos = HOPPER.above();
		helper.setBlock(tankPos, OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(tankPos);

		hopper.collect(new ExperienceOrb(helper.getLevel(), 0, 0, 0, 20));
		helper.assertTrue(hopper.getFluid().getAmount() == FluidXpUtils.xpToFluid(20), "Expected 20 XP in the hopper");
		hopper.toggleOutput(VacuumHopperBlockEntity.Output.XP, Direction.UP);

		helper.succeedWhen(() -> {
			helper.assertTrue(tank.getFluid().getAmount() == FluidXpUtils.xpToFluid(20), "Expected 20 XP in the tank, got " + tank.getFluid().getAmount());
			helper.assertTrue(hopper.getFluid().isEmpty(), "Expected the hopper tank to be empty");
		});
	}
}
