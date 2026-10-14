package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.AutoAnvilBlockEntity;
import openblocks.block.entity.TankBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;
import openblocks.util.XpUtils;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class AutoAnvilGameTests {

	private static final BlockPos ANVIL = new BlockPos(1, 1, 1);
	private static final BlockPos TANK = ANVIL.above();

	private static AutoAnvilBlockEntity setUp(GameTestHelper helper, int xpLevels) {
		helper.setBlock(TANK, OBBlocks.TANK.get());
		final TankBlockEntity tank = helper.getBlockEntity(TANK);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(XpUtils.getExperienceForLevel(xpLevels)), IFluidHandler.FluidAction.EXECUTE);
		helper.setBlock(ANVIL, OBBlocks.AUTO_ANVIL.get());
		return helper.getBlockEntity(ANVIL);
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 200)
	public static void appliesEnchantedBookWithXpFromTank(GameTestHelper helper) {
		final AutoAnvilBlockEntity anvil = setUp(helper, 10);
		final ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.set(DataComponents.CUSTOM_NAME, Component.literal("Stabby"));
		anvil.getInventory().setStackInSlot(AutoAnvilBlockEntity.SLOT_TOOL, sword);
		anvil.getInventory().setStackInSlot(AutoAnvilBlockEntity.SLOT_MODIFIER,
				EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment(helper, Enchantments.SHARPNESS), 1)));

		helper.succeedWhen(() -> {
			final ItemStack output = anvil.getInventory().getStackInSlot(AutoAnvilBlockEntity.SLOT_OUTPUT);
			helper.assertTrue(output.is(Items.IRON_SWORD), "Expected the sword in the output slot, got " + output);
			helper.assertTrue(output.getEnchantments().getLevel(enchantment(helper, Enchantments.SHARPNESS)) == 1, "Expected Sharpness I");
			helper.assertTrue("Stabby".equals(output.getHoverName().getString()), "Expected the custom name to be kept");
			helper.assertTrue(anvil.getInventory().getStackInSlot(AutoAnvilBlockEntity.SLOT_TOOL).isEmpty(), "Expected the tool to be used up");
			helper.assertTrue(anvil.getInventory().getStackInSlot(AutoAnvilBlockEntity.SLOT_MODIFIER).isEmpty(), "Expected the book to be used up");
			// Sharpness I from a book costs 1 level, the rest stays in the anvil's tank
			final int expected = FluidXpUtils.xpToFluid(XpUtils.getExperienceForLevel(10) - XpUtils.getExperienceForLevel(1));
			helper.assertTrue(anvil.getFluid().getAmount() == expected, "Expected " + expected + " mB left, got " + anvil.getFluid().getAmount());
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void repairsOnlyWithEnoughXp(GameTestHelper helper) {
		final AutoAnvilBlockEntity anvil = setUp(helper, 0);
		final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
		pickaxe.setDamageValue(200);
		anvil.getInventory().setStackInSlot(AutoAnvilBlockEntity.SLOT_TOOL, pickaxe);
		anvil.getInventory().setStackInSlot(AutoAnvilBlockEntity.SLOT_MODIFIER, new ItemStack(Items.IRON_INGOT, 10));

		helper.runAfterDelay(60, () -> {
			helper.assertTrue(anvil.getInventory().getStackInSlot(AutoAnvilBlockEntity.SLOT_OUTPUT).isEmpty(), "Expected no repair without XP");
			final TankBlockEntity tank = helper.getBlockEntity(TANK);
			tank.getFluidHandler().fill(FluidXpUtils.xpJuice(XpUtils.getExperienceForLevel(5)), IFluidHandler.FluidAction.EXECUTE);
			helper.succeedWhen(() -> {
				final ItemStack output = anvil.getInventory().getStackInSlot(AutoAnvilBlockEntity.SLOT_OUTPUT);
				helper.assertTrue(output.is(Items.IRON_PICKAXE) && output.getDamageValue() < 200, "Expected a repaired pickaxe, got " + output);
				// each ingot repairs a quarter of the durability, 200 damage needs 4 ingots
				helper.assertTrue(anvil.getInventory().getStackInSlot(AutoAnvilBlockEntity.SLOT_MODIFIER).getCount() == 6, "Expected 6 ingots left");
			});
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 200)
	public static void autoExtractAndEject(GameTestHelper helper) {
		final AutoAnvilBlockEntity anvil = setUp(helper, 10);
		final BlockPos input = ANVIL.west();
		final BlockPos output = ANVIL.east();
		helper.setBlock(input, Blocks.CHEST);
		helper.setBlock(output, Blocks.CHEST);
		final ChestBlockEntity inputChest = helper.getBlockEntity(input);
		final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
		pickaxe.setDamageValue(100);
		inputChest.setItem(0, pickaxe);
		inputChest.setItem(1, new ItemStack(Items.IRON_INGOT, 2));

		// otherwise auto extract would pull the result back from the output chest
		onlySide(anvil, AutoAnvilBlockEntity.AutoSlot.TOOL, Direction.WEST);
		onlySide(anvil, AutoAnvilBlockEntity.AutoSlot.MODIFIER, Direction.WEST);
		onlySide(anvil, AutoAnvilBlockEntity.AutoSlot.OUTPUT, Direction.EAST);
		anvil.getConfig().toggleAuto(AutoAnvilBlockEntity.AutoSlot.TOOL);
		anvil.getConfig().toggleAuto(AutoAnvilBlockEntity.AutoSlot.MODIFIER);
		anvil.getConfig().toggleAuto(AutoAnvilBlockEntity.AutoSlot.OUTPUT);

		helper.succeedWhen(() -> {
			final ChestBlockEntity outputChest = helper.getBlockEntity(output);
			helper.assertTrue(outputChest.countItem(Items.IRON_PICKAXE) == 1, "Expected the repaired pickaxe in the output chest");
			helper.assertTrue(inputChest.isEmpty(), "Expected the input chest to be emptied");
		});
	}

	private static void onlySide(AutoAnvilBlockEntity anvil, AutoAnvilBlockEntity.AutoSlot slot, Direction keep) {
		for (Direction side : Direction.values())
			if (side != keep) anvil.getConfig().toggleSide(slot, side);
	}
}
