package openblocks.client.gui;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import openblocks.OpenBlocks;
import openblocks.client.HeightMapCache;
import openblocks.client.HeightMapRenderer;
import openblocks.common.HeightMapData;
import openblocks.menu.ProjectorMenu;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/**
 * Port of legacy {@code GuiProjector}: a 3D view of the map in the slot, with the projector's rotation. Drag to turn the
 * view (legacy trackball), middle-click to look at it like the player again, scroll to move the red level marker. Green
 * lines mark every 64 blocks of height.
 */
public class ProjectorScreen extends AbstractContainerScreen<ProjectorMenu> {

	private static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/gui/projector.png");

	private static final int VIEW_X = 8;
	private static final int VIEW_Y = 8;
	private static final int VIEW_WIDTH = 160;
	private static final int VIEW_HEIGHT = 138;
	private static final float VIEW_SCALE = 90;
	private static final float DRAG_SPEED = 0.01f;

	private static final int BUTTON_SIZE = 13;
	private static final int BUTTON_Y = 130;
	private static final int LEFT_BUTTON_X = 7;
	private static final int RIGHT_BUTTON_X = 152;
	private static final int ICON_U = 176;

	private static final double MIN_MAP_HEIGHT = -5;
	private static final double MAX_MAP_HEIGHT = 10;
	private static final double SCROLL_SPEED = 0.12;
	// green lines at y -64, 0, 64 ... 320
	private static final int LOWEST_LEVEL = -1;
	private static final int HIGHEST_LEVEL = 5;

	private final Quaternionf view = new Quaternionf();
	private double mapHeight = 2;
	private boolean draggingView;

	public ProjectorScreen(ProjectorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 234;
		resetView();
	}

	// legacy MathUtils.createEntityRotateMatrix: as the player looks at the world
	private void resetView() {
		final Entity camera = Minecraft.getInstance().getCameraEntity();
		if (camera == null) return;
		view.rotationX(camera.getXRot() * Mth.DEG_TO_RAD).rotateY((camera.getYRot() + 180) * Mth.DEG_TO_RAD);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		graphics.blit(BACKGROUND, leftPos + LEFT_BUTTON_X, topPos + BUTTON_Y, ICON_U, 0, BUTTON_SIZE, BUTTON_SIZE);

		final PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(leftPos + RIGHT_BUTTON_X + BUTTON_SIZE, topPos + BUTTON_Y, 0);
		pose.scale(-1, 1, 1);
		graphics.blit(BACKGROUND, 0, 0, ICON_U, 0, BUTTON_SIZE, BUTTON_SIZE);
		pose.popPose();

		final Integer mapId = menu.getMapId();
		final HeightMapData data = mapId != null? HeightMapCache.get(mapId) : null;
		if (data != null) renderMap(graphics, data);
	}

	private void renderMap(GuiGraphics graphics, HeightMapData data) {
		final int viewLeft = leftPos + VIEW_X;
		final int viewTop = topPos + VIEW_Y;

		// like legacy, the map is drawn with its own depth, then the rest of the screen on top of it
		graphics.flush();
		RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		graphics.enableScissor(viewLeft, viewTop, viewLeft + VIEW_WIDTH, viewTop + VIEW_HEIGHT);

		final PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(viewLeft + VIEW_WIDTH / 2.0, viewTop + VIEW_HEIGHT / 2.0, 0);
		pose.scale(VIEW_SCALE, -VIEW_SCALE, VIEW_SCALE);
		pose.mulPose(view);
		pose.mulPose(Axis.YP.rotationDegrees(90 * menu.getRotation()));
		pose.translate(-0.5, -mapHeight, -0.5);

		HeightMapRenderer.render(data, pose.last(), graphics.bufferSource().getBuffer(HeightMapRenderer.RENDER_TYPE));
		final VertexConsumer lines = graphics.bufferSource().getBuffer(RenderType.lines());
		for (int level = LOWEST_LEVEL; level <= HIGHEST_LEVEL; level++)
			renderLevel(lines, pose.last(), level, 0xFF00FF00);
		renderLevel(lines, pose.last(), (float)mapHeight, 0xFFFF0000);
		graphics.flush();

		pose.popPose();
		graphics.disableScissor();
		RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
	}

	private static void renderLevel(VertexConsumer consumer, PoseStack.Pose pose, float y, int color) {
		line(consumer, pose, 0, y, 0, 0, y, 1, color);
		line(consumer, pose, 0, y, 1, 1, y, 1, color);
		line(consumer, pose, 1, y, 1, 1, y, 0, color);
		line(consumer, pose, 1, y, 0, 0, y, 0, color);
	}

	private static void line(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y, float z0, float x1, float y1, float z1, int color) {
		final float nx = x1 - x0;
		final float nz = z1 - z0;
		consumer.addVertex(pose, x0, y, z0).setColor(color).setNormal(pose, nx, 0, nz);
		consumer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, 0, nz);
	}

	// legacy had no labels
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

	// the map slot is inside the view, like legacy
	private boolean isOverView(double mouseX, double mouseY) {
		return hoveredSlot == null && isHovering(VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (isHovering(LEFT_BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) return clickButton(ProjectorMenu.BUTTON_ROTATE_LEFT);
			if (isHovering(RIGHT_BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) return clickButton(ProjectorMenu.BUTTON_ROTATE_RIGHT);
		}

		if (isOverView(mouseX, mouseY)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) resetView();
			else draggingView = true;
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean clickButton(int id) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingView) {
			view.premul(new Quaternionf().rotationY((float)dragX * DRAG_SPEED).rotateX((float)dragY * DRAG_SPEED));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggingView) {
			draggingView = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		mapHeight = Mth.clamp(mapHeight + scrollY * SCROLL_SPEED, MIN_MAP_HEIGHT, MAX_MAP_HEIGHT);
		return true;
	}
}
