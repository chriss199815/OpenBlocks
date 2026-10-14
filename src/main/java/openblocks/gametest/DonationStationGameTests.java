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
import openblocks.block.entity.DonationStationBlockEntity;
import openblocks.common.DonationInfo;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class DonationStationGameTests {

	private static final BlockPos STATION = new BlockPos(1, 2, 1);

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void acceptsItemsFromAnySideAndDropsThem(GameTestHelper helper) {
		helper.setBlock(STATION, OBBlocks.DONATION_STATION.get());
		final DonationStationBlockEntity station = helper.getBlockEntity(STATION);

		final IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(STATION), Direction.EAST);
		helper.assertTrue(handler != null, "Expected an item handler");
		helper.assertTrue(handler.insertItem(0, new ItemStack(Items.DIAMOND, 5), false).isEmpty(), "Expected all diamonds to be inserted");
		helper.assertTrue(station.getInventory().getStackInSlot(0).getCount() == 5, "Expected 5 diamonds in the station");

		helper.setBlock(STATION, Blocks.AIR);
		final int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(STATION)).inflate(2)).stream()
				.filter(item -> item.getItem().is(Items.DIAMOND))
				.mapToInt(item -> item.getItem().getCount())
				.sum();
		helper.assertTrue(dropped == 5, "Expected 5 dropped diamonds, got " + dropped);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void identifiesTheModOfAnItem(GameTestHelper helper) {
		final DonationInfo info = DonationInfo.of(new ItemStack(OBBlocks.DONATION_STATION.get())).orElse(null);
		helper.assertTrue(info != null, "Expected info for an OpenBlocks item");
		helper.assertTrue(info.modName().equals("OpenBlocks"), "Expected mod name OpenBlocks, got " + info.modName());
		helper.assertTrue(info.authors().contains("boq"), "Expected boq among the authors, got " + info.authors());

		helper.assertTrue(DonationInfo.of(ItemStack.EMPTY).isEmpty(), "Expected no info for an empty slot");
		helper.succeed();
	}
}
