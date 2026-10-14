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
import openblocks.registry.OBItems;

/**
 * Wrench.
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
}
