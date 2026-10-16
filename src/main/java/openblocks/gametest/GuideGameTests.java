package openblocks.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.block.GuideBlock;
import openblocks.block.entity.GuideBlockEntity;
import openblocks.common.GuideOrientation;
import openblocks.common.GuideRedstoneMode;
import openblocks.common.GuideSettings;
import openblocks.common.GuideShape;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.menu.GuideMenu;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.util.PaintColors;
import openblocks.util.shapes.ShapeGenerator;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class GuideGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);
	private static final BlockPos BUILDER_POS = new BlockPos(8, 3, 8);

	private static final GuideSettings SMALL_CUBOID = GuideSettings.DEFAULT
			.withSize(Direction.WEST, 2).withSize(Direction.DOWN, 2).withSize(Direction.NORTH, 2)
			.withSize(Direction.EAST, 2).withSize(Direction.UP, 2).withSize(Direction.SOUTH, 2)
			.withShape(GuideShape.CUBOID);

	/**
	 * Port of the OpenModsLib {@code ShapesTest}: shapes reach exactly to the edges of their box.
	 */
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void shapesFillTheirBox(GameTestHelper helper) {
		for (GuideShape shape : List.of(GuideShape.SPHERE, GuideShape.CYLINDER, GuideShape.CUBOID, GuideShape.FULL_CUBOID)) {
			final ShapeGenerator generator = shape.generator();
			final Set<BlockPos> zero = generate(generator, 0, 0, 0, 0, 0, 0);
			helper.assertTrue(zero.equals(Set.of(BlockPos.ZERO)), shape + " of size 0 should be one block");

			for (int i = 1; i <= 5; i++) {
				checkBox(helper, shape, -i, -i, -i, i, i, i);
				checkBox(helper, shape, 0, 0, 0, 2 * i, 2 * i, 2 * i);
			}
			checkBox(helper, shape, 0, 0, 0, 3, 3, 3);
			checkBox(helper, shape, 0, 0, 0, 5, 5, 5);
			checkBox(helper, shape, 1, 1, 1, 4, 4, 4);
			checkBox(helper, shape, 2, 2, 2, 7, 7, 7);
		}
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void shapesLeaveTheGuideFree(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get());
		final GuideBlockEntity guide = guide(helper, POS);
		helper.assertFalse(guide.getShape().isEmpty(), "Default shape should not be empty");
		helper.assertFalse(guide.getShape().contains(BlockPos.ZERO), "Shape should leave out the guide");

		helper.setBlock(POS, OBBlocks.BUILDER_GUIDE.get());
		for (BlockPos pos : guide(helper, POS).getShape())
			helper.assertTrue(Math.max(Math.abs(pos.getX()), Math.max(Math.abs(pos.getY()), Math.abs(pos.getZ()))) > 1,
					"Builder guide shape should keep the space around it free, but has " + pos);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void buttonsChangeSizeAndOrientation(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get());
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();

		// east face of the unrotated guide, legacy inc_pos_z at y 9..12, z 12..15
		click(helper, player, ItemStack.EMPTY, new Vec3(1, 10.5 / 16, 13.5 / 16), Direction.EAST);
		helper.assertTrue(guide(helper, POS).getSettings().posZ() == GuideSettings.DEFAULT_SIZE + 1, "inc_pos_z should grow the shape");

		// legacy copy_pos_z at y 6..10, z 10..12: copies to neg_z
		click(helper, player, ItemStack.EMPTY, new Vec3(1, 8.0 / 16, 11.0 / 16), Direction.EAST);
		helper.assertTrue(guide(helper, POS).getSettings().negZ() == GuideSettings.DEFAULT_SIZE + 1, "copy_pos_z should copy to neg_z");

		// top face, legacy inc_mode at x 5..11, z 2..5
		click(helper, player, ItemStack.EMPTY, new Vec3(0.5, 1, 3.5 / 16), Direction.UP);
		helper.assertTrue(guide(helper, POS).getSettings().shape() == GuideShape.CYLINDER, "inc_mode should switch to the next shape");

		// top face, legacy rotate_cw at x 11..15
		click(helper, player, ItemStack.EMPTY, new Vec3(13.0 / 16, 1, 0.5), Direction.UP);
		helper.assertBlockProperty(POS, GuideBlock.ORIENTATION, GuideOrientation.ZP_YP);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void rotatedGuideMapsButtons(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get().defaultBlockState().setValue(GuideBlock.ORIENTATION, GuideOrientation.ZP_YP));
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();

		// local x points south: the local east face is the world south face, local z points west
		click(helper, player, ItemStack.EMPTY, new Vec3(1 - 13.5 / 16, 10.5 / 16, 1), Direction.SOUTH);
		helper.assertTrue(guide(helper, POS).getSettings().posZ() == GuideSettings.DEFAULT_SIZE + 1, "inc_pos_z should be on the rotated face");

		final List<BlockPos> shape = guide(helper, POS).getShape();
		helper.assertTrue(shape.stream().anyMatch(p -> p.getX() == -(GuideSettings.DEFAULT_SIZE + 1)), "Local +z should reach west");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void menuButtonsChangeSettings(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get());
		guide(helper, POS).setSettings(GuideSettings.DEFAULT);
		final GuideMenu menu = new GuideMenu(0, guide(helper, POS));
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();

		menu.clickMenuButton(player, GuideMenu.sizeButton(Direction.UP, 3));
		helper.assertTrue(guide(helper, POS).getSettings().posY() == GuideSettings.DEFAULT_SIZE + 10, "+10 should grow the shape");
		for (int i = 0; i < 10; i++)
			menu.clickMenuButton(player, GuideMenu.sizeButton(Direction.UP, 3));
		helper.assertTrue(guide(helper, POS).getSettings().posY() == GuideSettings.MAX_SIZE, "Size should stop at the maximum");
		menu.clickMenuButton(player, GuideMenu.sizeButton(Direction.WEST, 0));
		helper.assertTrue(guide(helper, POS).getSettings().negX() == GuideSettings.DEFAULT_SIZE - 1, "-1 should shrink the shape");

		menu.clickMenuButton(player, GuideMenu.BUTTON_PREVIOUS_SHAPE);
		helper.assertTrue(guide(helper, POS).getSettings().shape() == GuideShape.PLANES, "Previous shape should wrap around");
		menu.clickMenuButton(player, GuideMenu.BUTTON_COLOR + DyeColor.LIME.getId());
		helper.assertTrue(guide(helper, POS).getSettings().color() == PaintColors.rgb(DyeColor.LIME), "Color should change");
		menu.clickMenuButton(player, GuideMenu.BUTTON_REDSTONE_MODE);
		helper.assertTrue(guide(helper, POS).getSettings().redstoneMode() == GuideRedstoneMode.POWERED, "Redstone mode should cycle");
		menu.clickMenuButton(player, GuideMenu.BUTTON_ROTATE_CCW);
		helper.assertBlockProperty(POS, GuideBlock.ORIENTATION, GuideOrientation.ZN_YP);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void dyeChangesColor(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get());
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		click(helper, player, new ItemStack(Items.RED_DYE), new Vec3(0.5, 0.5, 1), Direction.SOUTH);
		helper.assertTrue(guide(helper, POS).getSettings().color() == PaintColors.rgb(DyeColor.RED), "Dye should color the markers");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void newGuideIsAlwaysShown(GameTestHelper helper) {
		final int sensitivity = OBConfig.GUIDE.redstoneSensitivity.getAsInt();
		OBConfig.GUIDE.redstoneSensitivity.set(0);
		try {
			helper.setBlock(POS, OBBlocks.GUIDE.get());
			helper.assertTrue(guide(helper, POS).isEnabled(), "New guide should be shown without redstone");
			helper.assertTrue(guide(helper, POS).getSettings().redstoneMode() == GuideRedstoneMode.ALWAYS, "New guide should use the config mode");
		} finally {
			OBConfig.GUIDE.redstoneSensitivity.set(sensitivity);
		}
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void redstoneEnablesGuide(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get());
		guide(helper, POS).setSettings(GuideSettings.DEFAULT.withRedstoneMode(GuideRedstoneMode.POWERED));
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(guide(helper, POS).isEnabled(), "Unpowered guide should be off");
			helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
			helper.succeedWhen(() -> helper.assertTrue(guide(helper, POS).isEnabled(), "Powered guide should be on"));
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void dropKeepsSettings(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GUIDE.get());
		final GuideSettings settings = SMALL_CUBOID.withColor(0x123456);
		guide(helper, POS).setSettings(settings);
		helper.getLevel().destroyBlock(helper.absolutePos(POS), true);

		helper.succeedWhen(() -> {
			final List<ItemEntity> items = helper.getLevel().getEntities(EntityType.ITEM, new AABB(helper.absolutePos(POS)).inflate(2), e -> e.getItem().is(OBBlocks.GUIDE.asItem()));
			helper.assertTrue(items.size() == 1, "Guide should drop");
			helper.assertTrue(settings.equals(items.get(0).getItem().get(OBComponents.GUIDE)), "Dropped guide should keep its settings");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_16X8X16)
	public static void builderPlacesHeldBlock(GameTestHelper helper) {
		helper.setBlock(BUILDER_POS.north(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(BUILDER_POS, OBBlocks.BUILDER_GUIDE.get());
		guide(helper, BUILDER_POS).setSettings(SMALL_CUBOID);
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(guide(helper, BUILDER_POS).isEnabled(), "Builder guide should be on"))
				.thenExecute(() -> {
					click(helper, player, BUILDER_POS, new ItemStack(Items.STONE, 64), new Vec3(0.5, 1, 0.5), Direction.UP);
					final List<BlockPos> shape = guide(helper, BUILDER_POS).getShape();
					helper.assertBlockPresent(Blocks.STONE, BUILDER_POS.offset(shape.get(0)));
					helper.assertBlockNotPresent(Blocks.STONE, BUILDER_POS.offset(shape.get(1)));
					helper.assertTrue(shape.get(0).getY() == -2, "Builder guide should start at the bottom");
				})
				.thenSucceed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_16X8X16)
	public static void builderFillsShapeInCreative(GameTestHelper helper) {
		helper.setBlock(BUILDER_POS.north(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(BUILDER_POS.above(), Blocks.OBSIDIAN);
		helper.setBlock(BUILDER_POS, OBBlocks.BUILDER_GUIDE.get());
		guide(helper, BUILDER_POS).setSettings(SMALL_CUBOID);
		// the mock player is always creative
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(guide(helper, BUILDER_POS).isEnabled(), "Builder guide should be on"))
				.thenExecute(() -> {
					click(helper, player, BUILDER_POS, new ItemStack(Items.STONE), new Vec3(0.5, 0.5, 1), Direction.SOUTH);
					for (BlockPos pos : guide(helper, BUILDER_POS).getShape())
						helper.assertBlockPresent(Blocks.STONE, BUILDER_POS.offset(pos));
				})
				.thenSucceed();
	}

	private static void click(GameTestHelper helper, ServerPlayer player, ItemStack stack, Vec3 hit, Direction face) {
		click(helper, player, POS, stack, hit, face);
	}

	// like a click on the block, hit in block space
	private static void click(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack stack, Vec3 hit, Direction face) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		final BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(hit), face, absolute, false));
	}

	private static GuideBlockEntity guide(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos);
	}

	private static Set<BlockPos> generate(ShapeGenerator generator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		final Set<BlockPos> result = new HashSet<>();
		generator.generate(minX, minY, minZ, maxX, maxY, maxZ, (x, y, z) -> result.add(new BlockPos(x, y, z)));
		return result;
	}

	private static void checkBox(GameTestHelper helper, GuideShape shape, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		final Set<BlockPos> blocks = generate(shape.generator(), minX, minY, minZ, maxX, maxY, maxZ);
		final AABB expected = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		final AABB actual = blocks.stream().map(AABB::new).reduce(AABB::minmax).map(box -> box.contract(1, 1, 1)).orElseThrow();
		helper.assertTrue(expected.equals(actual), shape + " in " + expected + " should fill it, but covers " + actual);
	}
}
