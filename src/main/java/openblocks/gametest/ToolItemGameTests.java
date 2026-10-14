package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.SlimalyzerItem;
import openblocks.registry.OBItems;

/**
 * Wrench, sponge on a stick and slimalyzer.
 */
@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class ToolItemGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	private static InteractionResult useWrench(GameTestHelper helper, Player player, BlockPos pos, Direction side) {
		final ItemStack wrench = new ItemStack(OBItems.WRENCH.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
		final BlockPos absolute = helper.absolutePos(pos);
		final UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), side, absolute, false));
		return wrench.onItemUseFirst(context);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void wrenchRotatesBlocks(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		helper.setBlock(POS, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
		helper.assertTrue(useWrench(helper, player, POS, Direction.UP).consumesAction(), "Clicking the top should turn the furnace");
		helper.assertBlockProperty(POS, FurnaceBlock.FACING, Direction.EAST);
		helper.assertFalse(useWrench(helper, player, POS, Direction.NORTH).consumesAction(), "Furnaces only turn around the vertical axis");

		helper.setBlock(POS, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP));
		useWrench(helper, player, POS, Direction.NORTH);
		helper.assertBlockProperty(POS, PistonBaseBlock.FACING, Direction.EAST);

		helper.setBlock(POS, Blocks.OAK_LOG);
		useWrench(helper, player, POS, Direction.EAST);
		helper.assertBlockProperty(POS, RotatedPillarBlock.AXIS, Direction.Axis.X);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void wrenchTurnsLeversOnlyWhileSneaking(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(POS, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.NORTH));

		useWrench(helper, player, POS, Direction.UP);
		helper.assertBlockProperty(POS, LeverBlock.FACING, Direction.NORTH);
		player.setShiftKeyDown(true);
		useWrench(helper, player, POS, Direction.UP);
		helper.assertBlockProperty(POS, LeverBlock.FACING, Direction.EAST);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spongeOnAStickSoaksUpWater(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final ItemStack sponge = new ItemStack(OBItems.SPONGE_ON_A_STICK.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, sponge);
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(POS.east(), Blocks.WATER);
		helper.setBlock(POS.above(), Blocks.WATER);

		helper.placeAt(player, sponge, POS.below(), Direction.UP);
		helper.assertBlockPresent(Blocks.AIR, POS.east());
		helper.assertBlockPresent(Blocks.AIR, POS.above());
		helper.assertTrue(sponge.getDamageValue() == 1, "Expected one point of damage per use, got " + sponge.getDamageValue());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spongeOnAStickDrainsFlowingWaterAtEdge(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final ItemStack sponge = new ItemStack(OBItems.SPONGE_ON_A_STICK.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, sponge);
		final BlockPos clicked = new BlockPos(1, 1, 1);
		// placeAt clicks the top of the block below the given position, so the sponge works around y = 2
		// flowing water just outside the range (3), resting on stone, whose pending tick is gone like in a settled lake
		final BlockPos edge = clicked.above(5);
		helper.setBlock(clicked, Blocks.STONE);
		helper.setBlock(clicked.above(), Blocks.STONE);
		helper.setBlock(edge.below(), Blocks.STONE);
		helper.setBlock(edge, Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock());
		// its source, inside the range
		helper.setBlock(edge.below().east(), Blocks.WATER);
		helper.getLevel().getFluidTicks().clearArea(BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(2, 7, 2))));

		helper.placeAt(player, sponge, clicked, Direction.UP);
		helper.assertBlockPresent(Blocks.AIR, edge.below().east());
		helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, edge));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void spongeOnAStickBurnsInLava(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final ItemStack sponge = new ItemStack(OBItems.SPONGE_ON_A_STICK.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, sponge);
		helper.setBlock(POS.below(), Blocks.STONE);
		helper.setBlock(POS.east(), Blocks.LAVA);

		helper.placeAt(player, sponge, POS.below(), Direction.UP);
		helper.assertBlockPresent(Blocks.AIR, POS.east());
		helper.assertTrue(sponge.isEmpty(), "The sponge should burn up");
		helper.assertTrue(player.isOnFire(), "The player should be set on fire");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void slimalyzerLightsUpInSlimeChunks(GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final ChunkPos origin = new ChunkPos(helper.absolutePos(POS));
		ChunkPos slime = null, other = null;
		for (int dx = -10; dx <= 10 && (slime == null || other == null); dx++) {
			for (int dz = -10; dz <= 10; dz++) {
				final ChunkPos chunk = new ChunkPos(origin.x + dx, origin.z + dz);
				final ItemEntity probe = new ItemEntity(EntityType.ITEM, level);
				probe.setPos(chunk.getMiddleBlockX(), 100, chunk.getMiddleBlockZ());
				if (SlimalyzerItem.isInSlimeChunk(level, probe)) slime = chunk;
				else other = chunk;
			}
		}
		helper.assertTrue(slime != null && other != null, "Expected slime and non-slime chunks nearby");

		final ItemStack stack = new ItemStack(OBItems.SLIMALYZER.get());
		final ItemEntity entity = new ItemEntity(level, slime.getMiddleBlockX(), 100, slime.getMiddleBlockZ(), stack);
		stack.getItem().onEntityItemUpdate(stack, entity);
		helper.assertTrue(SlimalyzerItem.isActive(stack), "Expected the slimalyzer to light up in a slime chunk");

		entity.setPos(other.getMiddleBlockX(), 100, other.getMiddleBlockZ());
		stack.getItem().onEntityItemUpdate(stack, entity);
		helper.assertFalse(SlimalyzerItem.isActive(stack), "Expected the slimalyzer to turn off outside of slime chunks");
		helper.succeed();
	}
}
