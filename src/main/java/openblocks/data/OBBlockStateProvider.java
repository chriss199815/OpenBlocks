package openblocks.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.block.BlockManipulatorBlock;
import openblocks.block.GraveBlock;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.common.TankEdge;
import openblocks.registry.OBItems;

public class OBBlockStateProvider extends BlockStateProvider {

	public OBBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, OpenBlocks.MODID, existingFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		final ModelFile tintedCube = tintedCubeTemplate();

		// all colors share one model, the color comes from the tint (see OBColorHandlers)
		final ModelFile elevator = tintedCube(tintedCube, "elevator", modLoc("block/elevator"), modLoc("block/elevator"));
		OBBlocks.ELEVATORS.values().forEach(block -> {
			simpleBlock(block.get(), elevator);
			simpleBlockItem(block.get(), elevator);
		});

		// arrows on the top texture point south when unrotated, so they show the direction the arriving player will face
		final ModelFile rotatingElevator = tintedCube(tintedCube, "elevator_rotating", modLoc("block/elevator"), modLoc("block/elevator_rot"));
		OBBlocks.ROTATING_ELEVATORS.values().forEach(block -> {
			horizontalBlock(block.get(), rotatingElevator, 0);
			simpleBlockItem(block.get(), rotatingElevator);
		});

		// fluid blocks are drawn by the fluid renderer, the model only provides break particles
		simpleBlock(OBBlocks.XP_JUICE.get(), models().getBuilder("xp_juice").texture("particle", modLoc("block/xp_juice_still")));
		itemModels().basicItem(OBItems.XP_BUCKET.get());
		itemModels().basicItem(OBItems.GLIDER_WING.get());
		itemModels().basicItem(OBItems.HANG_GLIDER.get());
		itemModels().basicItem(OBItems.SLEEPING_BAG.get());
		// like legacy, the plain stencil texture
		itemModels().withExistingParent("unprepared_stencil", mcLoc("item/generated")).texture("layer0", modLoc("item/stencil"));
		itemModels().basicItem(OBItems.SKETCHING_PENCIL.get());
		itemModels().handheldItem(OBItems.SQUEEGEE.get());
		// legacy paintbrush model: handle, plus the paint tinted by OBColorHandlers
		itemModels().withExistingParent("paintbrush", mcLoc("item/handheld_rod"))
				.texture("layer0", modLoc("item/paintbrush_handle"))
				.texture("layer1", modLoc("item/paintbrush_paint"));
		// textures from OBStencilTextureProvider
		OBItems.STENCILS.values().forEach(stencil -> itemModels().basicItem(stencil.get()));

		tank();

		final ModelFile xpDrain = models().getBuilder("xp_drain")
				.renderType("cutout")
				.texture("all", modLoc("block/xp_drain"))
				.texture("particle", modLoc("block/xp_drain"))
				.element()
				.from(0, 0, 0)
				.to(16, 1, 16)
				.allFaces((dir, face) -> {
					face.texture("#all");
					if (dir.getAxis().isHorizontal()) face.uvs(0, 0, 16, 1);
					if (dir != Direction.UP) face.cullface(dir);
				})
				.end();
		horizontalBlock(OBBlocks.XP_DRAIN.get(), xpDrain, 0);
		final ModelFile xpShower = models().getBuilder("xp_shower")
				.texture("all", modLoc("block/xp_shower"))
				.texture("particle", modLoc("block/xp_shower"))
				// nozzle
				.element().from(7, 7, 7).to(9, 8, 9)
				.face(Direction.NORTH).uvs(0, 12, 2, 13).texture("#all").end()
				.face(Direction.EAST).uvs(6, 12, 8, 13).texture("#all").end()
				.face(Direction.SOUTH).uvs(4, 12, 6, 13).texture("#all").end()
				.face(Direction.WEST).uvs(2, 12, 4, 13).texture("#all").end()
				.face(Direction.DOWN).uvs(4, 13, 6, 15).texture("#all").end()
				.end()
				// pipe, attached to the block on the north side
				.element().from(7, 8, 0).to(9, 9, 9)
				.face(Direction.NORTH).uvs(0, 0, 2, 1).texture("#all").cullface(Direction.NORTH).end()
				.face(Direction.EAST).uvs(2, 1, 11, 2).texture("#all").end()
				.face(Direction.SOUTH).uvs(0, 1, 2, 2).texture("#all").end()
				.face(Direction.WEST).uvs(2, 0, 11, 1).texture("#all").end()
				.face(Direction.UP).uvs(0, 2, 2, 11).texture("#all").end()
				.face(Direction.DOWN).uvs(2, 2, 4, 11).texture("#all").end()
				.end();
		horizontalBlock(OBBlocks.XP_SHOWER.get(), xpShower, 0);
		// the pipe only fills a small part of the block, so it is enlarged and centered (the pipe's middle is at z = 4.5)
		itemModels().getBuilder("xp_shower")
				.parent(xpShower)
				.transforms()
				.transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(-3, 1.5f, 0).scale(1.2f).end()
				.transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.5f).end()
				.transform(ItemDisplayContext.FIXED).rotation(0, 90, 0).scale(1.0f).end()
				.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.6f).end()
				.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).scale(0.7f).end()
				.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).scale(0.7f).end()
				.end();

		final ModelFile xpBottler = models().cube("xp_bottler",
				modLoc("block/xp_bottler_bottom"), modLoc("block/xp_bottler_top"),
				modLoc("block/xp_bottler_front"), modLoc("block/xp_bottler_back"),
				modLoc("block/xp_bottler_sides"), modLoc("block/xp_bottler_sides"))
				.texture("particle", modLoc("block/xp_bottler_front"));
		horizontalBlock(OBBlocks.XP_BOTTLER.get(), xpBottler);
		simpleBlockItem(OBBlocks.XP_BOTTLER.get(), xpBottler);

		grave();
		vacuumHopper();

		// front on top: directionalBlock rotates the up face to the facing, like the vanilla dropper_vertical model
		final ModelFile itemDropper = models().withExistingParent("item_dropper", mcLoc("block/orientable_vertical"))
				.texture("front", modLoc("block/item_dropper_front"))
				.texture("side", modLoc("block/item_dropper"));
		directionalBlock(OBBlocks.ITEM_DROPPER.get(), itemDropper);
		simpleBlockItem(OBBlocks.ITEM_DROPPER.get(), itemDropper);

		// front on top, like the legacy cube_bottom_top models
		final ModelFile breakerInactive = models().cubeBottomTop("block_breaker", modLoc("block/block_breaker_side"), modLoc("block/block_breaker_back"), modLoc("block/block_breaker_inactive"));
		final ModelFile breakerActive = models().cubeBottomTop("block_breaker_active", modLoc("block/block_breaker_side"), modLoc("block/block_breaker_back"), modLoc("block/block_breaker_active"));
		directionalBlock(OBBlocks.BLOCK_BREAKER.get(), state -> state.getValue(BlockManipulatorBlock.POWERED)? breakerActive : breakerInactive);
		simpleBlockItem(OBBlocks.BLOCK_BREAKER.get(), breakerInactive);

		final ModelFile placer = models().cubeBottomTop("block_placer", modLoc("block/block_placer_side"), modLoc("block/block_placer_back"), modLoc("block/block_placer_front"));
		directionalBlock(OBBlocks.BLOCK_PLACER.get(), placer);
		simpleBlockItem(OBBlocks.BLOCK_PLACER.get(), placer);

		autoAnvil();
		paintMixer();
		// the blank canvas; CanvasModel shows the painted block instead
		final ModelFile canvas = models().cubeAll("canvas", modLoc("block/canvas"));
		simpleBlock(OBBlocks.CANVAS.get(), canvas);
		simpleBlockItem(OBBlocks.CANVAS.get(), canvas);
		simpleBlock(OBBlocks.CANVAS_GLASS.get(), canvas);
		paintCan();

		// like the legacy model: a 12 pixel high slab, the book is drawn by AutoEnchantmentTableRenderer
		final BlockModelBuilder autoEnchantmentTable = models().withExistingParent("auto_enchantment_table", mcLoc("block/block"))
				.texture("bottom", modLoc("block/auto_enchantment_table_bottom"))
				.texture("top", modLoc("block/auto_enchantment_table_top"))
				.texture("side", modLoc("block/auto_enchantment_table"))
				.texture("particle", modLoc("block/auto_enchantment_table_top"))
				.element().from(0, 0, 0).to(16, 12, 16)
				.face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#bottom").cullface(Direction.DOWN).end()
				.face(Direction.UP).uvs(0, 0, 16, 16).texture("#top").end()
				.face(Direction.NORTH).uvs(0, 4, 16, 16).texture("#side").cullface(Direction.NORTH).end()
				.face(Direction.SOUTH).uvs(0, 4, 16, 16).texture("#side").cullface(Direction.SOUTH).end()
				.face(Direction.WEST).uvs(0, 4, 16, 16).texture("#side").cullface(Direction.WEST).end()
				.face(Direction.EAST).uvs(0, 4, 16, 16).texture("#side").cullface(Direction.EAST).end()
				.end();
		simpleBlock(OBBlocks.AUTO_ENCHANTMENT_TABLE.get(), autoEnchantmentTable);
		simpleBlockItem(OBBlocks.AUTO_ENCHANTMENT_TABLE.get(), autoEnchantmentTable);

		// legacy piggy.obj, snout towards north, so the default offset makes it face the player
		final BlockModelBuilder donationStation = models().withExistingParent("donation_station", mcLoc("block/block"))
				.customLoader(ObjModelBuilder::begin)
				.modelLocation(modLoc("models/block/piggy.obj"))
				.flipV(true)
				.end()
				.texture("piggy1", modLoc("block/piggy1"))
				.texture("piggy2", modLoc("block/piggy2"))
				.texture("particle", modLoc("block/donation_station"));
		horizontalBlock(OBBlocks.DONATION_STATION.get(), donationStation);
		simpleBlockItem(OBBlocks.DONATION_STATION.get(), donationStation);

		final ModelFile drawingTable = models().orientableWithBottom("drawing_table",
				modLoc("block/drawing_table"), modLoc("block/drawing_table_front"), modLoc("block/drawing_table"), modLoc("block/drawing_table_top"));
		horizontalBlock(OBBlocks.DRAWING_TABLE.get(), drawingTable);
		simpleBlockItem(OBBlocks.DRAWING_TABLE.get(), drawingTable);

		// flat icon like vanilla iron bars, the 1px block model is barely visible from the side
		itemModels().withExistingParent("xp_drain", mcLoc("item/generated")).texture("layer0", modLoc("block/xp_drain"));
	}

	/**
	 * Frame with all edges, plus one model per edge for {@code TankModel}, which hides the edges shared with connected tanks.
	 * The fluid is drawn by the block entity and item renderers.
	 */
	private void tank() {
		final BlockModelBuilder frame = tankFrame("tank");
		for (TankEdge edge : TankEdge.values())
			tankEdge(frame, edge);
		simpleBlock(OBBlocks.TANK.get(), frame);

		for (TankEdge edge : TankEdge.values())
			tankEdge(tankFrame("tank_edge_" + edge.getName()), edge);

		itemModels().getBuilder("tank")
				.parent(new ModelFile.UncheckedModelFile("builtin/entity"))
				// same as minecraft:block/block, which can't be the parent of a builtin/entity model
				.transforms()
				.transform(ItemDisplayContext.GUI).rotation(30, 225, 0).scale(0.625f).end()
				.transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.25f).end()
				.transform(ItemDisplayContext.FIXED).scale(0.5f).end()
				.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
				.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).scale(0.4f).end()
				.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).scale(0.4f).end()
				.end();
	}

	/**
	 * Port of legacy grave_ground/grave_free: a gravestone at the south edge, with an optional dirt mound. The inscription faces north at 0°.
	 */
	private void grave() {
		final BlockModelBuilder ground = models().withExistingParent("grave_ground", mcLoc("block/block"))
				.texture("base", modLoc("block/grave_base"))
				.texture("front", modLoc("block/grave_front"))
				.texture("back", modLoc("block/grave_back"))
				.texture("particle", modLoc("block/grave_base"))
				.element().from(0, 0, 0).to(16, 1, 16)
				.allFaces((dir, face) -> face.texture("#base").uvs(0, 0, 16, dir.getAxis().isHorizontal()? 1 : 16))
				.end();
		graveStone(ground, 1);

		final BlockModelBuilder free = models().withExistingParent("grave_free", mcLoc("block/block"))
				.texture("front", modLoc("block/grave_front"))
				.texture("back", modLoc("block/grave_back"))
				.texture("particle", modLoc("block/grave_front"));
		graveStone(free, 0)
				.face(Direction.DOWN).uvs(2, 14, 14, 16).texture("#back").end();

		getVariantBuilder(OBBlocks.GRAVE.get()).forAllStates(state -> ConfiguredModel.builder()
				.modelFile(state.getValue(GraveBlock.HAS_BASE)? ground : free)
				.rotationY(((int)state.getValue(GraveBlock.FACING).toYRot() + 180) % 360)
				.build());
		simpleBlockItem(OBBlocks.GRAVE.get(), ground);
	}

	/**
	 * Elements of the legacy auto_anvil model, top along the z axis; rotated so the long side faces the player, like the vanilla anvil.
	 */
	private void autoAnvil() {
		final BlockModelBuilder model = models().withExistingParent("auto_anvil", mcLoc("block/block"))
				.texture("long_side", modLoc("block/auto_anvil_long_side"))
				.texture("short_side", modLoc("block/auto_anvil_short_side"))
				.texture("bottom", modLoc("block/auto_anvil_bottom"))
				.texture("top", modLoc("block/auto_anvil_top"))
				.texture("top_under", modLoc("block/auto_anvil_up"))
				.texture("base_top", modLoc("block/auto_anvil_down"))
				.texture("particle", modLoc("block/auto_anvil_long_side"))
				// base
				.element().from(2, 0, 2).to(14, 4, 14)
				.face(Direction.NORTH).uvs(2, 12, 14, 16).texture("#short_side").end()
				.face(Direction.EAST).uvs(2, 12, 14, 16).texture("#long_side").end()
				.face(Direction.SOUTH).uvs(2, 12, 14, 16).texture("#short_side").end()
				.face(Direction.WEST).uvs(2, 12, 14, 16).texture("#long_side").end()
				.face(Direction.UP).uvs(2, 2, 14, 14).texture("#base_top").end()
				.face(Direction.DOWN).uvs(2, 2, 14, 14).texture("#bottom").cullface(Direction.DOWN).end()
				.end()
				// lower narrow part
				.element().from(4, 4, 3).to(12, 5, 13)
				.face(Direction.NORTH).uvs(4, 11, 12, 12).texture("#short_side").end()
				.face(Direction.EAST).uvs(3, 11, 13, 12).texture("#long_side").end()
				.face(Direction.SOUTH).uvs(4, 11, 12, 12).texture("#short_side").end()
				.face(Direction.WEST).uvs(3, 11, 13, 12).texture("#long_side").end()
				.face(Direction.UP).uvs(4, 3, 12, 13).texture("#base_top").end()
				.end()
				// waist
				.element().from(6, 5, 4).to(10, 10, 12)
				.face(Direction.NORTH).uvs(6, 6, 10, 11).texture("#short_side").end()
				.face(Direction.EAST).uvs(4, 6, 12, 11).texture("#long_side").end()
				.face(Direction.SOUTH).uvs(6, 6, 10, 11).texture("#short_side").end()
				.face(Direction.WEST).uvs(4, 6, 12, 11).texture("#long_side").end()
				.end()
				// top
				.element().from(3, 10, 0).to(13, 16, 16)
				.face(Direction.NORTH).uvs(3, 0, 13, 6).texture("#short_side").end()
				.face(Direction.EAST).uvs(0, 0, 16, 6).texture("#long_side").end()
				.face(Direction.SOUTH).uvs(3, 0, 13, 6).texture("#short_side").end()
				.face(Direction.WEST).uvs(0, 0, 16, 6).texture("#long_side").end()
				.face(Direction.UP).uvs(3, 0, 13, 16).texture("#top").end()
				.face(Direction.DOWN).uvs(3, 0, 13, 16).texture("#top_under").end()
				.end();
		horizontalBlock(OBBlocks.AUTO_ANVIL.get(), model, 90);
		simpleBlockItem(OBBlocks.AUTO_ANVIL.get(), model);
	}

	/**
	 * Elements of the legacy paint_mixer model, plus the can, which {@code PaintMixerRenderer} draws tinted and shaking while mixing.
	 */
	private void paintMixer() {
		final BlockModelBuilder model = models().withExistingParent("paint_mixer", mcLoc("block/block"))
				.texture("model", modLoc("block/paint_mixer_model"))
				.texture("particle", modLoc("block/paint_mixer"))
				// bottom
				.element().from(3, 1, 2).to(13, 2, 14)
				.face(Direction.NORTH).uvs(3, 3, 5.5f, 3.25f).texture("#model").end()
				.face(Direction.EAST).uvs(0, 3, 3, 3.25f).texture("#model").end()
				.face(Direction.SOUTH).uvs(5.5f, 3, 8, 3.25f).texture("#model").end()
				.face(Direction.WEST).uvs(8, 3, 11, 3.25f).texture("#model").end()
				.face(Direction.UP).uvs(3, 0, 5.5f, 3).texture("#model").end()
				.face(Direction.DOWN).uvs(5.5f, 0, 8, 3).texture("#model").end()
				.end()
				// bottom2
				.element().from(3, 2, 3).to(13, 6, 4)
				.face(Direction.NORTH).uvs(6.75f, 9.25f, 9.25f, 10.25f).texture("#model").end()
				.face(Direction.EAST).uvs(11.75f, 9.25f, 12, 10.25f).texture("#model").end()
				.face(Direction.SOUTH).uvs(9.25f, 9.25f, 11.75f, 10.25f).texture("#model").end()
				.face(Direction.WEST).uvs(6.5f, 9.25f, 6.75f, 10.25f).texture("#model").end()
				.face(Direction.UP).uvs(8, 8.75f, 10.5f, 9).texture("#model").end()
				.face(Direction.DOWN).uvs(9.25f, 9, 11.75f, 9.25f).texture("#model").end()
				.end()
				// back
				.element().from(3, 0, 14).to(13, 16, 15)
				.face(Direction.NORTH).uvs(0.25f, 5, 2.75f, 9).texture("#model").end()
				.face(Direction.EAST).uvs(0, 5, 0.25f, 9).texture("#model").end()
				.face(Direction.SOUTH).uvs(3, 5, 5.5f, 9).texture("#model").end()
				.face(Direction.WEST).uvs(2.75f, 5, 3, 9).texture("#model").end()
				.face(Direction.UP).uvs(0.25f, 4.75f, 2.75f, 5).texture("#model").cullface(Direction.UP).end()
				.face(Direction.DOWN).uvs(3, 5, 5.5f, 5.25f).texture("#model").cullface(Direction.DOWN).end()
				.end()
				// top
				.element().from(3, 14, 5).to(13, 15, 14)
				.face(Direction.NORTH).uvs(7.75f, 8.75f, 10.25f, 9).texture("#model").end()
				.face(Direction.EAST).uvs(12.75f, 8.75f, 15, 9).texture("#model").end()
				.face(Direction.SOUTH).uvs(10.25f, 8.75f, 12.75f, 9).texture("#model").end()
				.face(Direction.WEST).uvs(5.5f, 8.75f, 7.75f, 9).texture("#model").end()
				.face(Direction.UP).uvs(7.75f, 6.5f, 10.25f, 8.75f).texture("#model").end()
				.face(Direction.DOWN).uvs(10.25f, 6.5f, 12.75f, 8.75f).texture("#model").end()
				.end()
				// controls
				.element().from(3, 10, 2).to(13, 14, 3)
				.rotation().origin(8, 8, 6).axis(Direction.Axis.X).angle(22.5f).end()
				.face(Direction.NORTH).uvs(5.75f, 5.25f, 8.25f, 6.25f).texture("#model").end()
				.face(Direction.EAST).uvs(5.5f, 5.25f, 5.75f, 6.25f).texture("#model").end()
				.face(Direction.SOUTH).uvs(8.25f, 5.25f, 10.75f, 6.25f).texture("#model").end()
				.face(Direction.WEST).uvs(10.75f, 5.25f, 11, 6.25f).texture("#model").end()
				.face(Direction.UP).uvs(5.75f, 5, 8.25f, 5.25f).texture("#model").end()
				.face(Direction.DOWN).uvs(8.25f, 5, 10.75f, 5.25f).texture("#model").end()
				.end()
				// left
				.element().from(2, 0, 3).to(3, 16, 15)
				.face(Direction.NORTH).uvs(3, 12, 3.25f, 16).texture("#model").end()
				.face(Direction.EAST).uvs(3.25f, 12, 6.25f, 16).texture("#model").end()
				.face(Direction.SOUTH).uvs(6.25f, 12, 6.5f, 16).texture("#model").end()
				.face(Direction.WEST).uvs(0, 12, 3, 16).texture("#model").end()
				.face(Direction.UP).uvs(3, 9, 3.25f, 12).texture("#model").cullface(Direction.UP).end()
				.face(Direction.DOWN).uvs(3.25f, 9, 3.5f, 12).texture("#model").cullface(Direction.DOWN).end()
				.end()
				// right
				.element().from(13, 0, 3).to(14, 16, 15)
				.face(Direction.NORTH).uvs(3, 12, 3.25f, 16).texture("#model").end()
				.face(Direction.EAST).uvs(0, 12, 3, 16).texture("#model").end()
				.face(Direction.SOUTH).uvs(6.25f, 12, 6.5f, 16).texture("#model").end()
				.face(Direction.WEST).uvs(3.25f, 12, 6.25f, 16).texture("#model").end()
				.face(Direction.UP).uvs(3, 9, 3.25f, 12).texture("#model").cullface(Direction.UP).end()
				.face(Direction.DOWN).uvs(3.25f, 9, 3.5f, 12).texture("#model").cullface(Direction.DOWN).end()
				.end();
		horizontalBlock(OBBlocks.PAINT_MIXER.get(), model);
		simpleBlockItem(OBBlocks.PAINT_MIXER.get(), model);

		models().withExistingParent("paint_mixer_can", mcLoc("block/block"))
				.texture("bottom", modLoc("block/paint_can_bottom"))
				.texture("front", modLoc("block/paint_can_front"))
				.texture("left", modLoc("block/paint_can_left"))
				.texture("side", modLoc("block/paint_can_side"))
				.texture("right", modLoc("block/paint_can_right"))
				.texture("spill", modLoc("block/paint_can_side2"))
				.texture("top", modLoc("block/paint_can_top"))
				.texture("particle", modLoc("block/paint_can_side"))
				// body
				.element().from(5, 2, 7).to(11, 10, 13)
				.face(Direction.NORTH).uvs(4, 5, 12, 16).texture("#side").end()
				.face(Direction.EAST).uvs(4, 5, 12, 16).texture("#right").end()
				.face(Direction.SOUTH).uvs(4, 5, 12, 16).texture("#front").end()
				.face(Direction.WEST).uvs(4, 5, 12, 16).texture("#left").end()
				.face(Direction.DOWN).uvs(4, 4, 12, 12).texture("#bottom").end()
				.end()
				// overlay
				.element().from(5, 2, 7).to(11, 10, 13)
				.face(Direction.NORTH).uvs(4, 5, 12, 16).texture("#spill").tintindex(0).end()
				.face(Direction.UP).uvs(4, 4, 12, 12).texture("#top").tintindex(0).end()
				.end();
	}

	/**
	 * Elements of the legacy paint_can model; tint index 1 is the paint, see {@code OBColorHandlers}.
	 */
	private void paintCan() {
		final BlockModelBuilder model = models().withExistingParent("paint_can", mcLoc("block/block"))
				.renderType("cutout")
				.texture("north", modLoc("block/paint_can_front"))
				.texture("east", modLoc("block/paint_can_left"))
				.texture("south", modLoc("block/paint_can_side"))
				.texture("west", modLoc("block/paint_can_right"))
				.texture("bottom", modLoc("block/paint_can_bottom"))
				.texture("top", modLoc("block/paint_can_top"))
				.texture("spill", modLoc("block/paint_can_side2"))
				.texture("particle", modLoc("block/paint_can_side"))
				// can
				.element().from(4, 0, 4).to(12, 11, 12)
				.face(Direction.NORTH).uvs(4, 5, 12, 16).texture("#north").end()
				.face(Direction.EAST).uvs(4, 5, 12, 16).texture("#east").end()
				.face(Direction.SOUTH).uvs(4, 5, 12, 16).texture("#south").end()
				.face(Direction.WEST).uvs(4, 5, 12, 16).texture("#west").end()
				.face(Direction.DOWN).uvs(4, 4, 12, 12).texture("#bottom").end()
				.end()
				// paint
				.element().from(4, 0, 4).to(12, 11, 12)
				.face(Direction.SOUTH).uvs(4, 5, 12, 16).texture("#spill").tintindex(1).end()
				.face(Direction.UP).uvs(4, 4, 12, 12).texture("#top").tintindex(1).end()
				.end();
		horizontalBlock(OBBlocks.PAINT_CAN.get(), model);
		simpleBlockItem(OBBlocks.PAINT_CAN.get(), model);
	}

	/**
	 * Body, plus one model per side and output kind for {@code VacuumHopperModel}, which adds the nozzles of the configured outputs.
	 */
	private void vacuumHopper() {
		final BlockModelBuilder body = models().withExistingParent("vacuum_hopper", mcLoc("block/block"))
				.renderType("cutout")
				.texture("body", modLoc("block/vacuum_hopper_body"))
				.texture("particle", modLoc("block/vacuum_hopper_body"))
				.element().from(4, 4, 4).to(12, 12, 12)
				.allFaces((dir, face) -> face.texture("#body").uvs(4, 4, 12, 12))
				.end();
		simpleBlock(OBBlocks.VACUUM_HOPPER.get(), body);
		simpleBlockItem(OBBlocks.VACUUM_HOPPER.get(), body);

		for (Direction side : Direction.values()) {
			final BlockModelBuilder template = models().getBuilder("vacuum_hopper_" + side.getSerializedName());
			vacuumHopperNozzle(template, side);
			for (String variant : VacuumHopperBlockEntity.NOZZLES)
				models().getBuilder("vacuum_hopper_" + side.getSerializedName() + "_" + variant)
						.parent(template)
						.texture("texture", modLoc("block/vacuum_hopper_" + variant));
		}
	}

	/**
	 * Elements of the legacy vacuum_hopper_&lt;side&gt; models.
	 */
	private static void vacuumHopperNozzle(BlockModelBuilder model, Direction side) {
		switch (side) {
			case DOWN -> model
					.element().from(5.5f, 3, 5.5f).to(10.5f, 4, 10.5f)
					.face(Direction.NORTH).uvs(12, 0, 13, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.EAST).uvs(14, 0, 13, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.SOUTH).uvs(14, 0, 15, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.WEST).uvs(15, 0, 16, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.DOWN).uvs(5, 0, 10, 5).texture("#texture").end()
					.end()
					.element().from(6.5f, 0, 6.5f).to(9.5f, 3, 9.5f)
					.face(Direction.NORTH).uvs(0, 0, 3, 3).texture("#texture").rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
					.face(Direction.EAST).uvs(0, 3, 3, 6).texture("#texture").rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
					.face(Direction.SOUTH).uvs(0, 6, 3, 9).texture("#texture").rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
					.face(Direction.WEST).uvs(0, 9, 3, 12).texture("#texture").rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
					.face(Direction.DOWN).uvs(6, 6, 9, 9).texture("#texture").cullface(Direction.DOWN).end()
					.end();
			case UP -> model
					.element().from(5.5f, 12, 5.5f).to(10.5f, 13, 10.5f)
					.face(Direction.NORTH).uvs(12, 0, 13, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.EAST).uvs(14, 0, 13, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.SOUTH).uvs(14, 0, 15, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.WEST).uvs(15, 0, 16, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.UP).uvs(5, 0, 10, 5).texture("#texture").end()
					.end()
					.element().from(6.5f, 13, 6.5f).to(9.5f, 16, 9.5f)
					.face(Direction.NORTH).uvs(0, 0, 3, 3).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.EAST).uvs(0, 3, 3, 6).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.SOUTH).uvs(0, 6, 3, 9).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.WEST).uvs(0, 9, 3, 12).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.UP).uvs(6, 6, 9, 9).texture("#texture").cullface(Direction.UP).end()
					.end();
			case NORTH -> model
					.element().from(5.5f, 5.5f, 3).to(10.5f, 10.5f, 4)
					.face(Direction.NORTH).uvs(5, 0, 10, 5).texture("#texture").end()
					.face(Direction.EAST).uvs(12, 0, 13, 5).texture("#texture").end()
					.face(Direction.WEST).uvs(14, 0, 15, 5).texture("#texture").end()
					.face(Direction.UP).uvs(13, 0, 14, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.DOWN).uvs(15, 0, 16, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.end()
					.element().from(6.5f, 6.5f, 0).to(9.5f, 9.5f, 3)
					.face(Direction.NORTH).uvs(6, 6, 9, 9).texture("#texture").cullface(Direction.NORTH).end()
					.face(Direction.EAST).uvs(0, 6, 3, 9).texture("#texture").rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN).end()
					.face(Direction.WEST).uvs(0, 0, 3, 3).texture("#texture").end()
					.face(Direction.UP).uvs(0, 3, 3, 6).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.DOWN).uvs(0, 9, 3, 12).texture("#texture").rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
					.end();
			case SOUTH -> model
					.element().from(5.5f, 5.5f, 12).to(10.5f, 10.5f, 13)
					.face(Direction.EAST).uvs(12, 0, 13, 5).texture("#texture").end()
					.face(Direction.SOUTH).uvs(5, 0, 10, 5).texture("#texture").end()
					.face(Direction.WEST).uvs(14, 0, 15, 5).texture("#texture").end()
					.face(Direction.UP).uvs(13, 0, 14, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.face(Direction.DOWN).uvs(15, 0, 16, 5).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.end()
					.element().from(6.5f, 6.5f, 13).to(9.5f, 9.5f, 16)
					.face(Direction.EAST).uvs(0, 6, 3, 9).texture("#texture").end()
					.face(Direction.SOUTH).uvs(6, 6, 9, 9).texture("#texture").cullface(Direction.SOUTH).end()
					.face(Direction.WEST).uvs(0, 0, 3, 3).texture("#texture").rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN).end()
					.face(Direction.UP).uvs(0, 3, 3, 6).texture("#texture").rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
					.face(Direction.DOWN).uvs(0, 9, 3, 12).texture("#texture").rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
					.end();
			case WEST -> model
					.element().from(3, 5.5f, 5.5f).to(4, 10.5f, 10.5f)
					.face(Direction.NORTH).uvs(12, 0, 13, 5).texture("#texture").end()
					.face(Direction.SOUTH).uvs(14, 0, 15, 5).texture("#texture").end()
					.face(Direction.WEST).uvs(5, 0, 10, 5).texture("#texture").end()
					.face(Direction.UP).uvs(13, 0, 14, 5).texture("#texture").end()
					.face(Direction.DOWN).uvs(15, 0, 16, 5).texture("#texture").end()
					.end()
					.element().from(0, 6.5f, 6.5f).to(3, 9.5f, 9.5f)
					.face(Direction.NORTH).uvs(0, 0, 3, 3).texture("#texture").rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN).end()
					.face(Direction.SOUTH).uvs(0, 6, 3, 9).texture("#texture").end()
					.face(Direction.WEST).uvs(6, 6, 9, 9).texture("#texture").cullface(Direction.WEST).end()
					.face(Direction.UP).uvs(0, 3, 3, 6).texture("#texture").end()
					.face(Direction.DOWN).uvs(0, 9, 3, 12).texture("#texture").end()
					.end();
			case EAST -> model
					.element().from(12, 5.5f, 5.5f).to(13, 10.5f, 10.5f)
					.face(Direction.NORTH).uvs(12, 0, 13, 5).texture("#texture").end()
					.face(Direction.EAST).uvs(5, 0, 10, 5).texture("#texture").end()
					.face(Direction.SOUTH).uvs(14, 0, 15, 5).texture("#texture").end()
					.face(Direction.UP).uvs(13, 0, 14, 5).texture("#texture").end()
					.face(Direction.DOWN).uvs(15, 0, 16, 5).texture("#texture").end()
					.end()
					.element().from(13, 6.5f, 6.5f).to(16, 9.5f, 9.5f)
					.face(Direction.NORTH).uvs(0, 0, 3, 3).texture("#texture").end()
					.face(Direction.EAST).uvs(6, 6, 9, 9).texture("#texture").cullface(Direction.EAST).end()
					.face(Direction.SOUTH).uvs(0, 6, 3, 9).texture("#texture").rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN).end()
					.face(Direction.UP).uvs(0, 3, 3, 6).texture("#texture").rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN).end()
					.face(Direction.DOWN).uvs(0, 9, 3, 12).texture("#texture").rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN).end()
					.end();
	
		}
	}

	private static ModelBuilder<BlockModelBuilder>.ElementBuilder graveStone(BlockModelBuilder model, int bottom) {
		return model.element().from(2, bottom, 14).to(14, bottom + 15, 16)
				.face(Direction.NORTH).uvs(2, 1, 14, 16).texture("#front").end()
				.face(Direction.EAST).uvs(14, 1, 16, 16).texture("#back").end()
				.face(Direction.SOUTH).uvs(2, 1, 14, 16).texture("#back").end()
				.face(Direction.WEST).uvs(0, 1, 2, 16).texture("#back").end()
				.face(Direction.UP).uvs(2, 0, 14, 2).texture("#back").end();
	}

	private BlockModelBuilder tankFrame(String name) {
		return models().getBuilder(name)
				.ao(false)
				.texture("particle", modLoc("block/tank"))
				.texture("frame", modLoc("block/tank"));
	}

	private static void tankEdge(ModelBuilder<?> model, TankEdge edge) {
		final float[] bounds = edge.getBounds();
		final Direction.Axis axis = edge.getAxis();
		model.element()
				.from(bounds[0], bounds[1], bounds[2])
				.to(bounds[3], bounds[4], bounds[5])
				.allFaces((dir, face) -> {
					face.texture("#frame");
					// the frame border runs along the top row/left column of the texture
					if (dir.getAxis() == axis) face.uvs(0, 0, 1, 1);
					else if (axis == Direction.Axis.Y || dir.getAxis() == Direction.Axis.Y) face.uvs(0, 0, 1, 16);
					else face.uvs(0, 0, 16, 1);
				})
				.end();
	}

	/**
	 * Replacement for {@code openmods:block/cube_tinted}: a full cube with tint index 0 on every face.
	 */
	private ModelFile tintedCubeTemplate() {
		return models().withExistingParent("tinted_cube", mcLoc("block/block"))
				.texture("particle", "#north")
				.element()
				.allFaces((dir, face) -> face.texture("#" + dir.getSerializedName()).cullface(dir).tintindex(0))
				.end();
	}

	private ModelFile tintedCube(ModelFile parent, String name, ResourceLocation side, ResourceLocation top) {
		final BlockModelBuilder model = models().getBuilder(name).parent(parent);
		for (Direction dir : Direction.values())
			model.texture(dir.getSerializedName(), dir == Direction.UP? top : side);
		return model;
	}
}
