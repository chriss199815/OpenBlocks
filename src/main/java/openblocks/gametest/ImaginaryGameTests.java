package openblocks.gametest;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.ImaginaryBlock;
import openblocks.block.entity.ImaginaryBlockEntity;
import openblocks.block.entity.ImaginaryBlockEntity.Property;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.ImaginaryItem;
import openblocks.item.ImaginaryPlacementMode;
import openblocks.item.ImaginationGlassesItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class ImaginaryGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final BlockPos DRAWN = GROUND.above();

	private static final int RED = 0xB3312C;
	private static final int BLUE = 0x253192;

	private static Player playerWith(GameTestHelper helper, ItemStack stack) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static ItemStack pencil(ImaginaryPlacementMode mode, float uses) {
		return ImaginaryItem.create(null, mode, uses);
	}

	/**
	 * Draws on top of the ground block.
	 */
	private static void draw(GameTestHelper helper, Player player) {
		helper.setBlock(GROUND, Blocks.STONE);
		helper.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
	}

	private static ImaginaryBlockEntity imaginary(GameTestHelper helper, BlockPos pos, @Nullable Integer color, boolean inverted, ImaginaryBlock.Shape shape, Direction facing) {
		helper.setBlock(pos, OBBlocks.IMAGINARY.get().defaultBlockState()
				.setValue(ImaginaryBlock.SHAPE, shape)
				.setValue(ImaginaryBlock.FACING, facing)
				.setValue(ImaginaryBlock.TYPE, color != null? ImaginaryBlock.Type.CRAYON : ImaginaryBlock.Type.PENCIL));
		final ImaginaryBlockEntity imaginary = helper.getBlockEntity(pos);
		imaginary.setup(color, inverted);
		return imaginary;
	}

	private static Player wearing(GameTestHelper helper, GameType gameType, ItemStack helmet) {
		final Player player = helper.makeMockPlayer(gameType);
		player.setItemSlot(EquipmentSlot.HEAD, helmet);
		return player;
	}

	private static void assertProperties(GameTestHelper helper, ImaginaryBlockEntity imaginary, Player player, boolean visible, boolean selectable, boolean solid, String what) {
		helper.assertTrue(imaginary.is(Property.VISIBLE, player) == visible, what + " should " + (visible? "" : "not ") + "be visible");
		helper.assertTrue(imaginary.is(Property.SELECTABLE, player) == selectable, what + " should " + (selectable? "" : "not ") + "be selectable");
		helper.assertTrue(imaginary.is(Property.SOLID, player) == solid, what + " should " + (solid? "" : "not ") + "be solid");
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void pencilDrawsBlocks(GameTestHelper helper) {
		final Player player = playerWith(helper, pencil(ImaginaryPlacementMode.STAIRS, 10));
		draw(helper, player);

		helper.assertBlockPresent(OBBlocks.IMAGINARY.get(), DRAWN);
		helper.assertBlockProperty(DRAWN, ImaginaryBlock.SHAPE, ImaginaryBlock.Shape.STAIRS);
		helper.assertBlockProperty(DRAWN, ImaginaryBlock.TYPE, ImaginaryBlock.Type.PENCIL);
		final ImaginaryBlockEntity imaginary = helper.getBlockEntity(DRAWN);
		helper.assertTrue(imaginary.isPencil() && !imaginary.isInverted(), "A pencil should draw a plain pencil block");
		final float uses = ImaginaryItem.getUses(player.getMainHandItem());
		helper.assertTrue(uses == 10 - ImaginaryPlacementMode.STAIRS.cost, "Stairs should cost 0.75 uses, uses left: " + uses);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void crayonDrawsColoredInvertedBlocks(GameTestHelper helper) {
		final Player player = playerWith(helper, ImaginaryItem.create(RED, ImaginaryPlacementMode.INVERTED_PANEL, 10));
		draw(helper, player);

		helper.assertBlockProperty(DRAWN, ImaginaryBlock.TYPE, ImaginaryBlock.Type.CRAYON);
		helper.assertBlockProperty(DRAWN, ImaginaryBlock.SHAPE, ImaginaryBlock.Shape.PANEL);
		final ImaginaryBlockEntity imaginary = helper.getBlockEntity(DRAWN);
		helper.assertTrue(Integer.valueOf(RED).equals(imaginary.getColor()) && imaginary.isInverted(), "A crayon should draw a block of its color and mode");

		final ItemStack picked = OBBlocks.IMAGINARY.get().getCloneItemStack(helper.getLevel(), helper.absolutePos(DRAWN), helper.getBlockState(DRAWN));
		helper.assertTrue(picked.is(OBItems.CRAYON.get()) && ImaginaryItem.getMode(picked) == ImaginaryPlacementMode.INVERTED_PANEL
				&& Integer.valueOf(RED).equals(ImaginaryItem.getColor(picked)), "Pick block should give a crayon of the block's color and mode");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void drawingNeedsEnoughUses(GameTestHelper helper) {
		final Player player = playerWith(helper, pencil(ImaginaryPlacementMode.INVERTED_BLOCK, 1));
		draw(helper, player);
		helper.assertBlockNotPresent(OBBlocks.IMAGINARY.get(), DRAWN);
		helper.assertTrue(ImaginaryItem.getUses(player.getMainHandItem()) == 1, "Failed drawing should not cost uses");

		player.getMainHandItem().set(OBComponents.IMAGINARY_MODE, ImaginaryPlacementMode.BLOCK);
		draw(helper, player);
		helper.assertBlockPresent(OBBlocks.IMAGINARY.get(), DRAWN);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "The pencil should be used up after its last use");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sneakingSwitchesMode(GameTestHelper helper) {
		final Player player = playerWith(helper, pencil(ImaginaryPlacementMode.INVERTED_STAIRS, 10));
		player.setShiftKeyDown(true);
		draw(helper, player);
		helper.assertBlockNotPresent(OBBlocks.IMAGINARY.get(), DRAWN);
		helper.assertTrue(ImaginaryItem.getMode(player.getMainHandItem()) == ImaginaryPlacementMode.BLOCK, "Sneaking should switch to the next mode, wrapping around");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void glassesDecideWhatPlayersSee(GameTestHelper helper) {
		final ImaginaryBlockEntity pencil = imaginary(helper, new BlockPos(0, 1, 0), null, false, ImaginaryBlock.Shape.BLOCK, Direction.NORTH);
		final ImaginaryBlockEntity invertedPencil = imaginary(helper, new BlockPos(1, 1, 0), null, true, ImaginaryBlock.Shape.BLOCK, Direction.NORTH);
		final ImaginaryBlockEntity red = imaginary(helper, new BlockPos(2, 1, 0), RED, false, ImaginaryBlock.Shape.BLOCK, Direction.NORTH);
		final ImaginaryBlockEntity invertedRed = imaginary(helper, new BlockPos(0, 1, 1), RED, true, ImaginaryBlock.Shape.BLOCK, Direction.NORTH);

		final Player plain = wearing(helper, GameType.SURVIVAL, ItemStack.EMPTY);
		assertProperties(helper, pencil, plain, false, false, true, "A pencil block without glasses");
		assertProperties(helper, invertedPencil, plain, true, true, true, "An inverted pencil block without glasses");
		assertProperties(helper, red, plain, false, false, false, "A crayon block without glasses");
		assertProperties(helper, invertedRed, plain, true, true, true, "An inverted crayon block without glasses");

		final Player pencilGlasses = wearing(helper, GameType.SURVIVAL, new ItemStack(OBItems.PENCIL_GLASSES.get()));
		assertProperties(helper, pencil, pencilGlasses, true, true, true, "A pencil block with pencil glasses");
		assertProperties(helper, invertedPencil, pencilGlasses, false, false, true, "An inverted pencil block with pencil glasses");
		assertProperties(helper, red, pencilGlasses, false, false, false, "A crayon block with pencil glasses");

		final Player redGlasses = wearing(helper, GameType.SURVIVAL, ImaginationGlassesItem.createCrayonGlasses(RED));
		assertProperties(helper, red, redGlasses, true, true, true, "A crayon block with glasses of its color");
		assertProperties(helper, invertedRed, redGlasses, false, false, false, "An inverted crayon block with glasses of its color");
		final Player blueGlasses = wearing(helper, GameType.SURVIVAL, ImaginationGlassesItem.createCrayonGlasses(BLUE));
		assertProperties(helper, red, blueGlasses, false, false, false, "A crayon block with glasses of another color");

		final Player technicolor = wearing(helper, GameType.SURVIVAL, new ItemStack(OBItems.TECHNICOLOR_GLASSES.get()));
		assertProperties(helper, red, technicolor, true, false, false, "A crayon block with technicolor glasses");
		assertProperties(helper, invertedRed, technicolor, true, true, true, "An inverted crayon block with technicolor glasses");

		final Player serious = wearing(helper, GameType.SURVIVAL, new ItemStack(OBItems.SERIOUS_GLASSES.get()));
		assertProperties(helper, red, serious, true, true, true, "A crayon block with serious glasses");

		final Player spectator = wearing(helper, GameType.SPECTATOR, ItemStack.EMPTY);
		helper.assertTrue(red.is(Property.VISIBLE, spectator), "Spectators should see every imaginary block");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void collisionFollowsGlasses(GameTestHelper helper) {
		imaginary(helper, DRAWN, RED, false, ImaginaryBlock.Shape.STAIRS, Direction.NORTH);
		final BlockState state = helper.getBlockState(DRAWN);
		final BlockPos pos = helper.absolutePos(DRAWN);

		final Player plain = wearing(helper, GameType.SURVIVAL, ItemStack.EMPTY);
		helper.assertTrue(state.getCollisionShape(helper.getLevel(), pos, CollisionContext.of(plain)).isEmpty(), "A crayon block should not collide without glasses");
		helper.assertTrue(state.getShape(helper.getLevel(), pos, CollisionContext.of(plain)).isEmpty(), "A crayon block should not be targeted without glasses");
		helper.assertTrue(state.getCollisionShape(helper.getLevel(), pos, CollisionContext.empty()).isEmpty(), "Imaginary blocks should not collide with anything but players");

		final Player redGlasses = wearing(helper, GameType.SURVIVAL, ImaginationGlassesItem.createCrayonGlasses(RED));
		final VoxelShape shape = state.getCollisionShape(helper.getLevel(), pos, CollisionContext.of(redGlasses));
		helper.assertTrue(Shapes.joinIsNotEmpty(shape, Shapes.box(0.4, 0.95, 0.1, 0.6, 0.99, 0.3), BooleanOp.AND), "Stairs facing north should have their upper step in the north");
		helper.assertTrue(Shapes.joinIsNotEmpty(shape, Shapes.box(0.4, 0.45, 0.7, 0.6, 0.49, 0.9), BooleanOp.AND), "Stairs facing north should have their lower step in the south");
		helper.assertFalse(Shapes.joinIsNotEmpty(shape, Shapes.box(0.4, 0.95, 0.7, 0.6, 0.99, 0.9), BooleanOp.AND), "Stairs should be open above the lower step");
		helper.succeed();
	}

	private static Optional<RecipeHolder<CraftingRecipe>> recipe(GameTestHelper helper, CraftingInput input) {
		return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void crayonsMerge(GameTestHelper helper) {
		final CraftingInput input = CraftingInput.of(2, 1, List.of(ImaginaryItem.create(RED, ImaginaryPlacementMode.PANEL, 3), ImaginaryItem.create(RED, ImaginaryPlacementMode.STAIRS, 4)));
		final CraftingRecipe recipe = recipe(helper, input).orElseThrow(() -> new GameTestAssertException("Crayons of one color should merge")).value();
		final ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(ImaginaryItem.getUses(result) == 7 && ImaginaryItem.getMode(result) == ImaginaryPlacementMode.BLOCK
				&& Integer.valueOf(RED).equals(ImaginaryItem.getColor(result)), "Merging should add up the uses and reset the mode");
		helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Merged crayons should not stay in the grid");

		final CraftingInput mixed = CraftingInput.of(2, 1, List.of(ImaginaryItem.create(null, ImaginaryPlacementMode.BLOCK, 3), ImaginaryItem.create(RED, ImaginaryPlacementMode.BLOCK, 4)));
		helper.assertTrue(recipe(helper, mixed).isEmpty(), "A pencil and a crayon should not merge");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void crayonsMix(GameTestHelper helper) {
		final CraftingInput input = CraftingInput.of(2, 1, List.of(ImaginaryItem.create(RED, ImaginaryPlacementMode.PANEL, 10), ImaginaryItem.create(BLUE, ImaginaryPlacementMode.PANEL, 10)));
		final CraftingRecipe recipe = recipe(helper, input).orElseThrow(() -> new GameTestAssertException("Crayons of different colors should mix")).value();
		final ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
		final int expected = (((0xB3 + 0x25) / 2) << 16) | (((0x31 + 0x31) / 2) << 8) | ((0x2C + 0x92) / 2);
		helper.assertTrue(Integer.valueOf(expected).equals(ImaginaryItem.getColor(result)), "Mixing should average the colors");
		helper.assertTrue(Math.abs(ImaginaryItem.getUses(result) - 1.8f) < 1e-5 && ImaginaryItem.getMode(result) == ImaginaryPlacementMode.PANEL,
				"Mixing should give 0.9 uses per crayon and keep the common mode");
		helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(stack -> ImaginaryItem.getUses(stack) == 9), "Mixed crayons should stay with one use less");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void glassesAreCrafted(GameTestHelper helper) {
		final CraftingInput crayonInput = CraftingInput.of(2, 1, List.of(ImaginaryItem.create(RED, ImaginaryPlacementMode.BLOCK, 10), new ItemStack(Items.PAPER)));
		final CraftingRecipe crayonRecipe = recipe(helper, crayonInput).orElseThrow(() -> new GameTestAssertException("A crayon and paper should make glasses")).value();
		final ItemStack crayonGlasses = crayonRecipe.assemble(crayonInput, helper.getLevel().registryAccess());
		helper.assertTrue(crayonGlasses.is(OBItems.CRAYON_GLASSES.get()) && ImaginationGlassesItem.getColor(crayonGlasses) == RED, "Crayon glasses should have the crayon's color");
		helper.assertTrue(ImaginaryItem.getUses(crayonRecipe.getRemainingItems(crayonInput).get(0)) == 9, "The crayon should stay with one use less");

		final CraftingInput pencilInput = CraftingInput.of(2, 1, List.of(pencil(ImaginaryPlacementMode.BLOCK, 1), new ItemStack(Items.PAPER)));
		final CraftingRecipe pencilRecipe = recipe(helper, pencilInput).orElseThrow(() -> new GameTestAssertException("A pencil and paper should make glasses")).value();
		helper.assertTrue(pencilRecipe.assemble(pencilInput, helper.getLevel().registryAccess()).is(OBItems.PENCIL_GLASSES.get()), "A pencil and paper should make pencil glasses");
		helper.assertTrue(pencilRecipe.getRemainingItems(pencilInput).get(0).isEmpty(), "A pencil with its last use should be used up");
		helper.succeed();
	}
}
