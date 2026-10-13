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
