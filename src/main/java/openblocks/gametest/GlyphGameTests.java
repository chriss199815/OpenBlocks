package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import openblocks.OpenBlocks;
import openblocks.block.entity.DrawingTableBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.Glyph;
import openblocks.item.GlyphItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBEntities;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class GlyphGameTests {

	private static final BlockPos WALL = new BlockPos(1, 2, 1);
	private static final BlockPos TABLE = new BlockPos(1, 2, 1);

	/**
	 * Uses a glyph on the given side of the wall, in the pixel at the given point of the face (from the bottom left, seen
	 * from the front); clicks the middle of the pixel, so rounding does not matter.
	 */
	private static boolean place(GameTestHelper helper, Player player, Direction side, double u, double v) {
		final BlockPos wall = helper.absolutePos(WALL);
		// any direction along the face for the top and bottom
		final Direction right = side.getAxis().isVertical()? Direction.EAST : side.getCounterClockWise();
		final double pixelU = (Math.floor(u * 16) + 0.5) / 16;
		final double pixelV = (Math.floor(v * 16) + 0.5) / 16;
		final Vec3 hit = Vec3.atCenterOf(wall).relative(side, 0.5).relative(right, pixelU - 0.5).add(0, pixelV - 0.5, 0);
		final UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), new BlockHitResult(hit, side, wall, false));
		return player.getMainHandItem().useOn(context).consumesAction();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void glyphHangsAtTheClickedPixel(GameTestHelper helper) {
		helper.setBlock(WALL, Blocks.STONE);
		helper.setBlock(WALL.east(), Blocks.STONE);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, GlyphItem.create('A', 2));

		// a quarter from the left and three quarters up, on the north side: seen from the north, left is east
		helper.assertTrue(place(helper, player, Direction.NORTH, 0.25, 0.75), "The glyph should be placed");
		final Glyph glyph = helper.findOneEntity(OBEntities.GLYPH.get());
		helper.assertTrue(glyph.getChar() == 'A' && glyph.getDirection() == Direction.NORTH, "The glyph should be an A facing north");
		final Vec3 center = glyph.getBoundingBox().getCenter().subtract(Vec3.atLowerCornerOf(helper.absolutePos(WALL)));
		helper.assertTrue(Math.abs(center.x - 0.75) < 1e-6 && Math.abs(center.y - 0.75) < 1e-6 && center.z < 0 && center.z > -0.05,
				"The glyph should be centered on the clicked pixel in front of the wall, was " + center);
		helper.assertTrue(Math.abs(glyph.getBoundingBox().getXsize() - 0.5) < 1e-6, "The glyph should be eight pixels wide");
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "One glyph should be used");

		// overlapping glyphs are not allowed, like paintings
		helper.assertFalse(place(helper, player, Direction.NORTH, 0.3, 0.75), "A glyph should not overlap another one");
		// a glyph sticking out over the right edge needs the wall there too
		helper.assertTrue(place(helper, player, Direction.NORTH, 0.0, 0.25), "A glyph should hang across two wall blocks");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void glyphNeedsAWall(GameTestHelper helper) {
		helper.setBlock(WALL, Blocks.STONE);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, GlyphItem.create('B', 1));
		helper.assertFalse(place(helper, player, Direction.SOUTH, 0.0, 0.5), "Half the glyph would hang in the air");
		helper.assertFalse(place(helper, player, Direction.UP, 0.5, 0.5), "Glyphs only go on walls");

		helper.assertTrue(place(helper, player, Direction.SOUTH, 0.5, 0.5), "The glyph should be placed");
		final Glyph glyph = helper.findOneEntity(OBEntities.GLYPH.get());
		helper.setBlock(WALL, Blocks.AIR);
		helper.assertFalse(glyph.survives(), "Without the wall the glyph should not survive");
		glyph.dropItem(null);
		final List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, glyph.getBoundingBox().inflate(1));
		helper.assertTrue(drops.size() == 1 && GlyphItem.getChar(drops.get(0).getItem()) == 'B', "The glyph should drop itself");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void glyphNamesAndCharacters(GameTestHelper helper) {
		helper.assertTrue(GlyphItem.getChar(new ItemStack(OBItems.GLYPH.get())) == GlyphItem.DEFAULT_CHAR, "A plain glyph should be a question mark");
		helper.assertTrue(GlyphItem.getChar(GlyphItem.create('€', 1)) == GlyphItem.DEFAULT_CHAR, "Characters without a glyph become question marks");
		helper.assertTrue(GlyphItem.cycle('■', 1) == 'À' && GlyphItem.cycle('À', -1) == '■', "The selection should wrap around");
		helper.assertTrue(GlyphItem.create('x', 1).getHoverName().getString().contains("x"), "The name should contain the character");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void drawingTableMakesGlyphs(GameTestHelper helper) {
		helper.setBlock(TABLE, OBBlocks.DRAWING_TABLE.get());
		final IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TABLE), Direction.UP);
		final DrawingTableBlockEntity table = helper.getBlockEntity(TABLE);
		handler.insertItem(DrawingTableBlockEntity.SLOT_INPUT, new ItemStack(OBItems.UNPREPARED_STENCIL.get(), 5), false);

		helper.assertTrue(table.print("Hi") == 0, "Printing should only work in glyph mode");
		table.toggleMode();
		final ItemStack preview = handler.getStackInSlot(DrawingTableBlockEntity.SLOT_OUTPUT);
		helper.assertTrue(preview.is(OBItems.GLYPH.get()) && GlyphItem.getChar(preview) == 'A', "The output should be an A glyph, got " + preview);
		table.cycleSelection(1);
		helper.assertTrue(GlyphItem.getChar(handler.extractItem(DrawingTableBlockEntity.SLOT_OUTPUT, 1, false)) == 'B', "The next glyph should be a B");

		// four left: "Hi€there" stops at the euro sign
		helper.assertTrue(DrawingTableBlockEntity.printableCount("Hi€there", 4) == 2, "Printing should stop at characters without a glyph");
		helper.assertTrue(table.print("Hi€there") == 2, "Two glyphs should be printed");
		helper.assertTrue(table.getInput().getCount() == 2, "Each printed glyph should use one input, left: " + table.getInput());
		helper.assertTrue(table.print("abcdef") == 2, "Printing should stop when the input runs out");
		helper.assertTrue(table.getInput().isEmpty(), "The input should be used up");

		final List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(TABLE)).inflate(2));
		final String printed = drops.stream().map(drop -> String.valueOf(GlyphItem.getChar(drop.getItem()))).sorted().reduce("", String::concat);
		helper.assertTrue(printed.equals("Habi"), "The glyphs should drop on the table, got " + printed);
		helper.succeed();
	}
}
