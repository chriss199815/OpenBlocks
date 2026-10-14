package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.common.CanvasSide;
import openblocks.common.StencilPattern;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.PaintBrushItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class CanvasGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	private static Player playerWith(GameTestHelper helper, ItemStack stack) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static void useOn(GameTestHelper helper, Player player, Direction side) {
		final BlockPos absolute = helper.absolutePos(POS);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), side, absolute, false)));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void brushTurnsBlocksIntoCanvas(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.STONE);
		final Player player = playerWith(helper, PaintBrushItem.create(0x123456));
		useOn(helper, player, Direction.UP);

		helper.assertBlockPresent(OBBlocks.CANVAS.get(), POS);
		final CanvasBlockEntity canvas = helper.getBlockEntity(POS);
		helper.assertTrue(canvas.getPaintedBlock() == Blocks.STONE.defaultBlockState(), "Expected the canvas to keep the stone");
		helper.assertTrue(canvas.getSide(Direction.UP).background() == 0xFF123456, "Expected the top to be painted");
		helper.assertTrue(!canvas.getSide(Direction.NORTH).hasPaint(), "Expected the other sides to stay unpainted");

		// glass does not fill its space, so it becomes canvas glass
		helper.setBlock(POS.above(), Blocks.GLASS);
		final BlockPos glass = helper.absolutePos(POS.above());
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(glass), Direction.UP, glass, false)));
		helper.assertBlockPresent(OBBlocks.CANVAS_GLASS.get(), POS.above());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void stencilLimitsThePaint(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.STONE);
		final Player player = playerWith(helper, new ItemStack(OBItems.STENCILS.get(StencilPattern.BORDER).get(), 2));
		useOn(helper, player, Direction.NORTH);

		final CanvasBlockEntity canvas = helper.getBlockEntity(POS);
		helper.assertTrue(canvas.getSide(Direction.NORTH).cover().isPresent(), "Expected the stencil to lie on the north side");
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Expected one stencil to be used");
		// one stencil per side
		useOn(helper, player, Direction.NORTH);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Expected the second stencil to stay in hand");

		player.setItemInHand(InteractionHand.MAIN_HAND, PaintBrushItem.create(0xFF0000));
		useOn(helper, player, Direction.NORTH);
		final CanvasSide side = canvas.getSide(Direction.NORTH);
		// BORDER cuts out the two left columns
		helper.assertTrue(side.paintAt(0, 5) == 0xFFFF0000 && side.paintAt(8, 5) == 0, "Expected paint only where the stencil is cut out");

		// turning the stencil moves the cut out part
		helper.assertTrue(canvas.rotateStencil(Direction.NORTH), "Expected the stencil to turn");
		helper.assertTrue(CanvasSide.isCutOut(StencilPattern.BORDER, 1, 5, 0) && !CanvasSide.isCutOut(StencilPattern.BORDER, 1, 0, 5), "Expected the border on top after a quarter turn");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void squeegeeRestoresTheBlock(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.STONE);
		final Player player = playerWith(helper, PaintBrushItem.create(0x00FF00));
		useOn(helper, player, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.STENCILS.get(StencilPattern.HEART).get()));
		useOn(helper, player, Direction.EAST);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SQUEEGEE.get()));
		useOn(helper, player, Direction.UP);
		helper.assertBlockPresent(Blocks.STONE, POS);
		// the stencil left on the east side is dropped
		helper.assertItemEntityPresent(OBItems.STENCILS.get(StencilPattern.HEART).get(), POS, 2);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void paintedRedstoneBlockStillPowers(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.REDSTONE_BLOCK);
		helper.setBlock(POS.east().below(), Blocks.STONE);
		helper.setBlock(POS.east(), Blocks.REDSTONE_WIRE);
		final Player player = playerWith(helper, PaintBrushItem.create(0x0000FF));
		useOn(helper, player, Direction.UP);

		helper.assertBlockPresent(OBBlocks.CANVAS.get(), POS);
		helper.assertBlockProperty(POS.east(), BlockStateProperties.POWER, 15);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void blocksWithBlockEntitiesStayUnpainted(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.CHEST);
		final Player player = playerWith(helper, PaintBrushItem.create(0x00FF00));
		useOn(helper, player, Direction.UP);
		helper.assertBlockPresent(Blocks.CHEST, POS);
		helper.succeed();
	}
}
