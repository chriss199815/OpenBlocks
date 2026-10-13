package openblocks.client.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import openblocks.OpenBlocks;
import openblocks.common.TankEdge;
import openblocks.registry.OBBlocks;

/**
 * Tank frame that only draws the edges not hidden by connected tanks; replaces legacy {@code openmods:variantmodel}.
 * Without model data (items, block breaking) all edges are drawn.
 */
public class TankModel extends BakedModelWrapper<BakedModel> {

	private static final ModelProperty<Integer> VISIBLE_EDGES = new ModelProperty<>();

	private final BakedModel[] edges;

	private TankModel(BakedModel frame, BakedModel[] edges) {
		super(frame);
		this.edges = edges;
	}

	private static ModelResourceLocation edgeLocation(TankEdge edge) {
		return ModelResourceLocation.standalone(OpenBlocks.location("block/tank_edge_" + edge.getName()));
	}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		for (TankEdge edge : TankEdge.values())
			event.register(edgeLocation(edge));
	}

	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		final Map<ModelResourceLocation, BakedModel> models = event.getModels();
		final BakedModel[] edges = new BakedModel[TankEdge.values().length];
		for (TankEdge edge : TankEdge.values())
			edges[edge.ordinal()] = models.get(edgeLocation(edge));

		for (BlockState state : OBBlocks.TANK.get().getStateDefinition().getPossibleStates())
			models.computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, frame) -> new TankModel(frame, edges));
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		// called during chunk meshing, so neighbour changes are picked up whenever the section is rebuilt
		return modelData.derive().with(VISIBLE_EDGES, TankEdge.getVisibleEdges(level, pos)).build();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		final Integer visibleEdges = extraData.get(VISIBLE_EDGES);
		if (visibleEdges == null || visibleEdges == TankEdge.ALL) return super.getQuads(state, side, rand, extraData, renderType);

		final List<BakedQuad> result = new ArrayList<>();
		for (TankEdge edge : TankEdge.values())
			if ((visibleEdges & (1 << edge.ordinal())) != 0)
				result.addAll(edges[edge.ordinal()].getQuads(state, side, rand, ModelData.EMPTY, renderType));
		return result;
	}
}
