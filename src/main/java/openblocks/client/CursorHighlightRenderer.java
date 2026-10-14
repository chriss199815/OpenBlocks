package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import openblocks.item.CursorItem;
import openblocks.item.CursorTarget;
import openblocks.registry.OBComponents;

/**
 * Outlines the block a held cursor is linked to: light blue when it can be clicked, red when it is out of reach. Not in legacy.
 */
public class CursorHighlightRenderer {

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

		final Minecraft minecraft = Minecraft.getInstance();
		final Player player = minecraft.player;
		final Level level = minecraft.level;
		if (player == null || level == null) return;

		final ItemStack stack = player.getMainHandItem();
		final CursorTarget target = stack.get(OBComponents.CURSOR_TARGET.get());
		if (!(stack.getItem() instanceof CursorItem) || target == null || target.pos().dimension() != level.dimension()) return;

		final BlockPos pos = target.pos().pos();
		if (!level.isLoaded(pos)) return;
		final BlockState state = level.getBlockState(pos);
		if (state.isAir()) return;

		List<AABB> boxes = state.getShape(level, pos).toAabbs();
		if (boxes.isEmpty()) boxes = List.of(new AABB(0, 0, 0, 1, 1, 1));

		final boolean inReach = CursorItem.isInReach(player, pos);
		final float red = inReach? 0.4f : 1.0f;
		final float green = inReach? 0.8f : 0.2f;
		final float blue = inReach? 1.0f : 0.2f;

		final Vec3 camera = event.getCamera().getPosition();
		final PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

		final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		final VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
		for (AABB box : boxes)
			LevelRenderer.renderLineBox(poseStack, consumer, box.inflate(0.002), red, green, blue, 1.0f);
		buffers.endBatch(RenderType.lines());
		poseStack.popPose();
	}
}
