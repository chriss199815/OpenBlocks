package openblocks.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Rotatable block in a screen whose sides can be clicked; replaces OpenModsLib {@code GuiComponentSideSelector}.
 * Dragging with the left button rotates it, a click without dragging toggles the side under the cursor, the middle button resets the view.
 */
public class SideSelectorWidget {

	private static final int HOVER_COLOR = 0x80444444;
	private static final int SELECTED_COLOR = 0x80CC0000;
	private static final double DRAG_THRESHOLD = 2;
	private static final AABB CUBE = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);

	private final int x;
	private final int y;
	private final float scale;
	private final int size;
	private final BlockEntity blockEntity;
	private final Supplier<Set<Direction>> selectedSides;
	private final boolean highlightSelected;
	private final Consumer<Direction> onToggle;

	private float yaw;
	private float pitch;

	private int left;
	private int top;
	private boolean dragging;
	private double dragDistance;

	/**
	 * @param x, y position relative to the origin passed to {@link #render}
	 * @param scale size of the block in pixels
	 * @param highlightSelected draw selected sides in red; off if the block model already shows them
	 */
	public SideSelectorWidget(int x, int y, float scale, BlockEntity blockEntity, Supplier<Set<Direction>> selectedSides, boolean highlightSelected, Consumer<Direction> onToggle) {
		this.x = x;
		this.y = y;
		this.scale = scale;
		this.size = Mth.ceil(scale * Math.sqrt(3));
		this.blockEntity = blockEntity;
		this.selectedSides = selectedSides;
		this.highlightSelected = highlightSelected;
		this.onToggle = onToggle;
		resetView();
	}

	/**
	 * Shows the block the way the player sees it.
	 */
	private void resetView() {
		final Entity camera = Minecraft.getInstance().getCameraEntity();
		yaw = camera != null? camera.getYRot() : 180;
		pitch = camera != null? camera.getXRot() : 0;
	}

	public int getSize() {
		return size;
	}

	/**
	 * Block space (cube centered on the origin) to screen space; the same rotation as the world camera, with y pointing down.
	 */
	private Matrix4f transform() {
		return new Matrix4f()
				.translate(left + x + size / 2.0f, top + y + size / 2.0f, 100 + size)
				.scale(scale, -scale, scale)
				.rotateX(pitch * Mth.DEG_TO_RAD)
				.rotateY((yaw + 180) * Mth.DEG_TO_RAD);
	}

	public void render(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
		this.left = left;
		this.top = top;
		final Matrix4f transform = transform();

		final PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.mulPose(transform);
		pose.translate(-0.5f, -0.5f, -0.5f);
		Lighting.setupFor3DItems();
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockEntity.getBlockState(), pose, graphics.bufferSource(),
				LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, blockEntity.getModelData(), null);
		graphics.flush();
		pose.popPose();

		final VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.guiOverlay());
		if (highlightSelected)
			for (Direction side : selectedSides.get())
				drawFace(buffer, transform, side, SELECTED_COLOR);
		final Direction hovered = dragging? null : pick(mouseX, mouseY);
		if (hovered != null) drawFace(buffer, transform, hovered, HOVER_COLOR);
		graphics.flush();
	}

	private static void drawFace(VertexConsumer buffer, Matrix4f transform, Direction side, int color) {
		// only faces turned towards the viewer, since the overlay ignores depth
		final Vector3f normal = transform.transformDirection(new Vector3f(side.step()));
		if (normal.z <= 0) return;

		final Vector3f n = side.step().mul(0.5f);
		final Vector3f u = side.getAxis() == Direction.Axis.X? new Vector3f(0, 0.5f, 0) : new Vector3f(0.5f, 0, 0);
		final Vector3f v = side.getAxis() == Direction.Axis.Z? new Vector3f(0, 0.5f, 0) : new Vector3f(0, 0, 0.5f);
		final Vector3f[] corners = {
				new Vector3f(n).sub(u).sub(v), new Vector3f(n).add(u).sub(v), new Vector3f(n).add(u).add(v), new Vector3f(n).sub(u).add(v)
		};
		// both windings, so culling doesn't matter
		for (int i : new int[] { 0, 1, 2, 3, 3, 2, 1, 0 })
			buffer.addVertex(transform, corners[i].x, corners[i].y, corners[i].z).setColor(color);
	}

	@Nullable
	private Direction pick(double mouseX, double mouseY) {
		if (!isMouseOver(mouseX, mouseY)) return null;
		// the GUI looks down the z axis, so cast a ray from in front of the block to behind it
		final Matrix4f inverse = transform().invert();
		final Vector3f from = inverse.transformPosition(new Vector3f((float)mouseX, (float)mouseY, 1000));
		final Vector3f to = inverse.transformPosition(new Vector3f((float)mouseX, (float)mouseY, -1000));
		final BlockHitResult hit = AABB.clip(List.of(CUBE), new Vec3(from), new Vec3(to), BlockPos.ZERO);
		return hit != null && hit.getType() == HitResult.Type.BLOCK? hit.getDirection() : null;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= left + x && mouseX < left + x + size && mouseY >= top + y && mouseY < top + y + size;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) return false;
		if (button == 0) {
			dragging = true;
			dragDistance = 0;
		} else if (button == 2) {
			resetView();
		}
		return true;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!dragging) return false;
		dragDistance += Math.abs(dragX) + Math.abs(dragY);
		if (dragDistance > DRAG_THRESHOLD) {
			yaw -= (float)dragX * 2;
			pitch = Mth.clamp(pitch + (float)dragY * 2, -90, 90);
		}
		return true;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (!dragging || button != 0) return false;
		dragging = false;
		if (dragDistance <= DRAG_THRESHOLD) {
			final Direction side = pick(mouseX, mouseY);
			if (side != null) onToggle.accept(side);
		}
		return true;
	}
}
