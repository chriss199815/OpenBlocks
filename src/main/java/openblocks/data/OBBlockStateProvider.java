package openblocks.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
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
