package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.AutoEnchantmentTableBlockEntity;
import openblocks.block.entity.TankBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;
import openblocks.util.XpUtils;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class AutoEnchantmentTableGameTests {

	// centered in the 3 wide template, so there is no room for bookshelves: power stays 0
	private static final BlockPos TABLE = new BlockPos(1, 1, 1);
	private static final BlockPos TANK = TABLE.below();

	private static AutoEnchantmentTableBlockEntity setUp(GameTestHelper helper, int xpLevels) {
		helper.setBlock(TANK, OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(TANK);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(XpUtils.getExperienceForLevel(xpLevels)), IFluidHandler.FluidAction.EXECUTE);
		helper.setBlock(TABLE, OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
		return helper.getBlockEntity(TABLE);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 200)
	public static void enchantsWithLapisAndXp(GameTestHelper helper) {
		final AutoEnchantmentTableBlockEntity table = setUp(helper, 30);
		table.getInventory().setStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_TOOL, new ItemStack(Items.DIAMOND_PICKAXE));
		table.getInventory().setStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_LAPIS, new ItemStack(Items.LAPIS_LAZULI, 5));
		// without bookshelves, only the first option is always available
		table.setOption(0);

		helper.succeedWhen(() -> {
			final ItemStack output = table.getInventory().getStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_OUTPUT);
			helper.assertTrue(output.is(Items.DIAMOND_PICKAXE) && output.isEnchanted(), "Expected an enchanted pickaxe, got " + output);
			helper.assertTrue(table.getInventory().getStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_TOOL).isEmpty(), "Expected the tool to be used up");
			// the first option costs 1 lapis and 1 level
			helper.assertTrue(table.getInventory().getStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_LAPIS).getCount() == 4, "Expected 4 lapis left");
			helper.assertTrue(table.getFluid().getAmount() < FluidXpUtils.xpToFluid(XpUtils.getExperienceForLevel(30)), "Expected XP to be used");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void needsEnoughXp(GameTestHelper helper) {
		final AutoEnchantmentTableBlockEntity table = setUp(helper, 0);
		table.getInventory().setStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_TOOL, new ItemStack(Items.BOOK));
		table.getInventory().setStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_LAPIS, new ItemStack(Items.LAPIS_LAZULI, 5));

		helper.runAfterDelay(40, () -> {
			helper.assertTrue(table.getInventory().getStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_OUTPUT).isEmpty(), "Expected nothing to happen without XP");
			final TankBlockEntity tank = helper.getBlockEntity(TANK);
			tank.getFluidHandler().fill(FluidXpUtils.xpJuice(XpUtils.getExperienceForLevel(10)), IFluidHandler.FluidAction.EXECUTE);
			helper.succeedWhen(() -> {
				final ItemStack output = table.getInventory().getStackInSlot(AutoEnchantmentTableBlockEntity.SLOT_OUTPUT);
				// like the vanilla table, books become enchanted books
				helper.assertTrue(output.is(Items.ENCHANTED_BOOK), "Expected an enchanted book, got " + output);
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void rejectsInvalidItems(GameTestHelper helper) {
		final AutoEnchantmentTableBlockEntity table = setUp(helper, 0);
		final ItemStack dirt = table.getItemHandler(null).insertItem(AutoEnchantmentTableBlockEntity.SLOT_TOOL, new ItemStack(Blocks.DIRT), false);
		helper.assertTrue(dirt.getCount() == 1, "Dirt can't be enchanted");
		final ItemStack stone = table.getItemHandler(null).insertItem(AutoEnchantmentTableBlockEntity.SLOT_LAPIS, new ItemStack(Items.STONE), false);
		helper.assertTrue(stone.getCount() == 1, "Only lapis goes into the lapis slot");
		table.getItemHandler(null).insertItem(AutoEnchantmentTableBlockEntity.SLOT_TOOL, new ItemStack(Items.BOOK), false);
		final ItemStack secondBook = table.getItemHandler(null).insertItem(AutoEnchantmentTableBlockEntity.SLOT_TOOL, new ItemStack(Items.BOOK), false);
		helper.assertTrue(secondBook.getCount() == 1, "Only one item at a time, like the vanilla table");
		helper.succeed();
	}
}
