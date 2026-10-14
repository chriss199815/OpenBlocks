package openblocks.client.canvas;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.registry.OBBlocks;

/**
 * Replaces the legacy {@code BakedModelCanvas}: shows the block the canvas replaced, or the plain canvas.
 * The paint and stencils on top are drawn by {@link CanvasRenderer}.
 */
public class CanvasModel extends BakedModelWrapper<BakedModel> {

	private CanvasModel(BakedModel blank) {
		super(blank);
	}

	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		for (Block block : new Block[] { OBBlocks.CANVAS.get(), OBBlocks.CANVAS_GLASS.get() })
			for (BlockState state : block.getStateDefinition().getPossibleStates())
				event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, blank) -> new CanvasModel(blank));
	}

	@Nullable
	private static BakedModel paintedModel(@Nullable BlockState painted) {
		return painted != null? Minecraft.getInstance().getBlockRenderer().getBlockModel(painted) : null;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		final BlockState painted = extraData.get(CanvasBlockEntity.PAINTED_BLOCK);
		final BakedModel model = paintedModel(painted);
		return model != null? model.getQuads(painted, side, rand, ModelData.EMPTY, renderType) : super.getQuads(state, side, rand, extraData, renderType);
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		final BlockState painted = data.get(CanvasBlockEntity.PAINTED_BLOCK);
		final BakedModel model = paintedModel(painted);
		return model != null? model.getRenderTypes(painted, rand, ModelData.EMPTY) : super.getRenderTypes(state, rand, data);
	}

	@Override
	public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
		final BlockState painted = data.get(CanvasBlockEntity.PAINTED_BLOCK);
		final BakedModel model = paintedModel(painted);
		return model != null? model.useAmbientOcclusion(painted, ModelData.EMPTY, renderType) : super.useAmbientOcclusion(state, data, renderType);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		final BakedModel model = paintedModel(data.get(CanvasBlockEntity.PAINTED_BLOCK));
		return model != null? model.getParticleIcon(ModelData.EMPTY) : super.getParticleIcon(data);
	}
}
