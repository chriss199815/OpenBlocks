package openblocks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.OpenBlocks;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.registry.OBBlocks;

/**
 * Vacuum hopper body plus one nozzle per output side, textured by what it outputs; replaces legacy {@code openmods:variantmodel}.
 */
public class VacuumHopperModel extends BakedModelWrapper<BakedModel> {

	// [side][variant]
	private final BakedModel[][] nozzles;

	private VacuumHopperModel(BakedModel body, BakedModel[][] nozzles) {
		super(body);
		this.nozzles = nozzles;
	}

	private static ModelResourceLocation nozzleLocation(Direction side, String variant) {
		return ModelResourceLocation.standalone(OpenBlocks.location("block/vacuum_hopper_" + side.getSerializedName() + "_" + variant));
	}

	public static void registerAdditional(ModelEvent.RegisterAdditional event) {
		for (Direction side : Direction.values())
			for (String variant : VacuumHopperBlockEntity.NOZZLES)
				event.register(nozzleLocation(side, variant));
	}

	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		final Map<ModelResourceLocation, BakedModel> models = event.getModels();
		final BakedModel[][] nozzles = new BakedModel[6][VacuumHopperBlockEntity.NOZZLES.length];
		for (Direction side : Direction.values())
			for (int variant = 0; variant < VacuumHopperBlockEntity.NOZZLES.length; variant++)
				nozzles[side.get3DDataValue()][variant] = models.get(nozzleLocation(side, VacuumHopperBlockEntity.NOZZLES[variant]));

		for (BlockState state : OBBlocks.VACUUM_HOPPER.get().getStateDefinition().getPossibleStates())
			models.computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, body) -> new VacuumHopperModel(body, nozzles));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		final Integer outputs = extraData.get(VacuumHopperBlockEntity.OUTPUTS);
		final List<BakedQuad> body = super.getQuads(state, side, rand, extraData, renderType);
		if (outputs == null || outputs == 0) return body;

		final List<BakedQuad> result = new ArrayList<>(body);
		for (Direction dir : Direction.values()) {
			final int variant = ((outputs >> dir.get3DDataValue()) & 1) + ((outputs >> (6 + dir.get3DDataValue())) & 1) * 2;
			if (variant > 0) result.addAll(nozzles[dir.get3DDataValue()][variant - 1].getQuads(state, side, rand, ModelData.EMPTY, renderType));
		}
		return result;
	}
}
