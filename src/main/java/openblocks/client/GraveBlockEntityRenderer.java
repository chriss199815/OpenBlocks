package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.block.GraveBlock;
import openblocks.block.entity.GraveBlockEntity;

/**
 * Port of legacy {@code TileEntityGraveRenderer}: writes the player's name on the gravestone. The text boxes come from legacy {@code hitboxes/grave_text.json}.
 */
public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {

	private static final float TEXT_WIDTH = 8 / 16f;
	private static final float TEXT_CENTER_Y_WITH_BASE = 10 / 16f;
	private static final float TEXT_CENTER_Y_WITHOUT_BASE = 9 / 16f;
	/**
	 * Just in front of the stone's inscription face.
	 */
	private static final float TEXT_Z = 14 / 16f - 0.001f;
	private static final float MAX_SCALE = 0.02f;

	private final Font font;

	public GraveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.getFont();
	}

	@Override
	public void render(GraveBlockEntity grave, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		final String username = grave.getUsername();
		if (username.isEmpty()) return;

		final BlockState state = grave.getBlockState();
		if (!(state.getBlock() instanceof GraveBlock)) return;

		final int textWidth = font.width(username);
		final float scale = Math.min(TEXT_WIDTH / textWidth, MAX_SCALE);
		final float centerY = state.getValue(GraveBlock.HAS_BASE)? TEXT_CENTER_Y_WITH_BASE : TEXT_CENTER_Y_WITHOUT_BASE;

		poseStack.pushPose();
		// same rotation as the blockstate, which turns the north-facing model towards FACING
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(-(state.getValue(GraveBlock.FACING).toYRot() + 180)));
		poseStack.translate(-0.5, 0, -0.5);

		poseStack.translate(0.5, centerY, TEXT_Z);
		// text faces north (towards -z), so it is readable in front of the inscription
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		poseStack.scale(scale, -scale, scale);
		font.drawInBatch(username, -textWidth / 2f, -font.lineHeight / 2f, 0x000000, false, poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
		poseStack.popPose();
	}
}
