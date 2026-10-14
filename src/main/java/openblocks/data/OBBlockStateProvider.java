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
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
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
