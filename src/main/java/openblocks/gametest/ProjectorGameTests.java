package openblocks.gametest;

import java.util.BitSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.block.ProjectorBlock;
import openblocks.block.entity.ProjectorBlockEntity;
import openblocks.common.HeightMapBuilder;
import openblocks.common.HeightMapData;
import openblocks.common.HeightMaps;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.EmptyMapItem;
import openblocks.item.HeightMapItem;
import openblocks.recipe.MapCloneRecipe;
import openblocks.recipe.MapResizeRecipe;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class ProjectorGameTests {

	private static final MapCloneRecipe CLONE = new MapCloneRecipe(CraftingBookCategory.MISC);
	private static final MapResizeRecipe RESIZE = new MapResizeRecipe(CraftingBookCategory.MISC);

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void emptyMapBecomesHeightMapInProjector(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, OBBlocks.PROJECTOR.get());
		final ProjectorBlockEntity projector = (ProjectorBlockEntity)helper.getBlockEntity(pos);

		helper.assertTrue(projector.getInventory().insertItem(0, EmptyMapItem.create(2), false).isEmpty(), "Expected the projector to take the empty map");
		final ItemStack map = projector.getInventory().getStackInSlot(0);
		helper.assertTrue(map.is(OBItems.HEIGHT_MAP), "Expected a height map, got " + map);
		helper.assertValueEqual(map.get(OBComponents.MAP_SCALE), 2, "map scale");
		final HeightMapData data = HeightMaps.get(helper.getLevel().getServer(), projector.getMapId());
		helper.assertTrue(data != null && data.getScale() == 2, "Expected new map data with scale 2");
		helper.assertBlockProperty(pos, ProjectorBlock.ACTIVE, true);
		helper.assertValueEqual(helper.getBlockState(pos).getLightEmission(helper.getLevel(), helper.absolutePos(pos)), OBConfig.PROJECTOR.lightLevel.getAsInt(), "light");

		helper.assertFalse(projector.getInventory().isItemValid(0, new ItemStack(Blocks.STONE)), "Expected only maps in the slot");
		projector.getInventory().extractItem(0, 1, false);
		helper.assertBlockProperty(pos, ProjectorBlock.ACTIVE, false);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void mapCloneNeedsSameScale(GameTestHelper helper) {
		final ItemStack heightMap = HeightMapItem.create(7, 1);
		final ItemStack copies = CLONE.assemble(CraftingInput.of(2, 1, List.of(heightMap, EmptyMapItem.create(1))), helper.getLevel().registryAccess());
		helper.assertTrue(copies.is(OBItems.HEIGHT_MAP) && copies.getCount() == 2, "Expected two copies, got " + copies);
		helper.assertValueEqual(copies.get(OBComponents.HEIGHT_MAP_ID), 7, "copied map id");
		helper.assertFalse(CLONE.matches(CraftingInput.of(2, 1, List.of(heightMap, EmptyMapItem.create(0))), helper.getLevel()), "Expected no copy with a different scale");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void mapResizeDoublesScale(GameTestHelper helper) {
		final ItemStack memory = new ItemStack(OBItems.MAP_MEMORY.get());
		final ItemStack bigger = RESIZE.assemble(CraftingInput.of(3, 1, List.of(memory, EmptyMapItem.create(0), memory)), helper.getLevel().registryAccess());
		helper.assertValueEqual(bigger.get(OBComponents.MAP_SCALE), 1, "resized scale");
		helper.assertFalse(RESIZE.matches(CraftingInput.of(3, 1, List.of(memory, EmptyMapItem.create(HeightMapData.MAX_SCALE), memory)), helper.getLevel()),
				"Expected no resize past the largest scale");
		helper.assertFalse(RESIZE.matches(CraftingInput.of(3, 1, List.of(memory, EmptyMapItem.create(0), ItemStack.EMPTY)), helper.getLevel()), "Expected two memory modules");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_16X8X16)
	public static void builderMapsGroundAndWater(GameTestHelper helper) {
		final BlockPos gold = new BlockPos(5, 3, 5);
		final BlockPos water = new BlockPos(5, 3, 7);
		helper.setBlock(gold, Blocks.GOLD_BLOCK);
		helper.setBlock(water.below(), Blocks.STONE);
		helper.setBlock(water, Blocks.WATER);

		final BlockPos goldPos = helper.absolutePos(gold);
		final BlockPos waterPos = helper.absolutePos(water);
		final HeightMapData data = new HeightMapData();
		HeightMapBuilder.start(helper.getLevel(), data, goldPos.getX(), goldPos.getZ());
		final BitSet finished = new BitSet();
		// the blocks may be in two chunks, both loaded by the test
		while (HeightMapBuilder.mapNextChunk(helper.getLevel(), data, finished, goldPos.getX(), goldPos.getZ()));
		helper.assertTrue(finished.cardinality() > 0, "Expected mapped chunks");

		final HeightMapData.Layer ground = data.getLayers().get(HeightMapData.LAYER_TERRAIN);
		final HeightMapData.Layer liquid = data.getLayers().get(HeightMapData.LAYER_LIQUIDS);
		final int goldPixel = pixel(data, goldPos);
		helper.assertValueEqual(ground.colors()[goldPixel] & 0xFF, MapColor.GOLD.id, "gold color");
		helper.assertValueEqual(ground.heights()[goldPixel], goldPos.getY(), "gold height");
		helper.assertValueEqual(liquid.colors()[goldPixel] & 0xFF, 0, "liquid over gold");

		final int waterPixel = pixel(data, waterPos);
		helper.assertValueEqual(liquid.colors()[waterPixel] & 0xFF, MapColor.WATER.id, "water color");
		helper.assertValueEqual(liquid.heights()[waterPixel], waterPos.getY(), "water height");
		helper.assertValueEqual(ground.colors()[waterPixel] & 0xFF, MapColor.STONE.id, "ground under water");
		helper.assertValueEqual(ground.heights()[waterPixel], waterPos.getY() - 1, "ground height under water");
		helper.succeed();
	}

	// scale 0: one block per pixel, the map starts two chunks before the center chunk
	private static int pixel(HeightMapData data, BlockPos pos) {
		final int x = pos.getX() - (data.getCenterX() - 32);
		final int z = pos.getZ() - (data.getCenterZ() - 32);
		return z * HeightMapData.SIZE + x;
	}
}
