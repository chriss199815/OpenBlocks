package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws block models registered through {@code ModelEvent.RegisterAdditional} from block entity renderers, for the parts that replace legacy armatures.
 */
public class ExtraModels {

	private static final RandomSource RANDOM = RandomSource.create();

	public static void render(ModelResourceLocation location, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
		final ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
		RANDOM.setSeed(42);
		for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY))
			renderer.renderModel(poseStack.last(), bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(renderType, false)), state, model,
					1, 1, 1, packedLight, packedOverlay, ModelData.EMPTY, renderType);
	}
}
