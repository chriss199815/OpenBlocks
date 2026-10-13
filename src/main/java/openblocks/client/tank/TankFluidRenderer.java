package openblocks.client.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the fluid box of a tank, shared by the block entity and item renderers. Port of legacy {@code TileEntityTankRenderer.renderFluid}.
 */
public class TankFluidRenderer {

	/**
	 * Surface heights (0-1) at the corners and in the center, plus which walls to draw.
	 */
	public record Shape(float nw, float ne, float se, float sw, float center, boolean[] walls) {
		public boolean hasWall(Direction side) {
			return walls[side.get3DDataValue()];
		}
	}

	public static void render(PoseStack poseStack, MultiBufferSource buffers, FluidStack fluid, Shape shape, int light) {
		final IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
		final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
		final int color = extensions.getTintColor(fluid);

		final Quads quads = new Quads(buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack.last(), sprite, color, light);

		final float nw = shape.nw, ne = shape.ne, se = shape.se, sw = shape.sw;

		if (shape.hasWall(Direction.NORTH) && (nw > 0 || ne > 0)) {
			quads.side(1, 0, 0, 0, ne, nw, 0, 0, -1);
		}

		if (shape.hasWall(Direction.SOUTH) && (sw > 0 || se > 0)) {
			quads.side(0, 1, 1, 1, sw, se, 0, 0, 1);
		}

		if (shape.hasWall(Direction.WEST) && (nw > 0 || sw > 0)) {
			quads.side(0, 0, 0, 1, nw, sw, -1, 0, 0);
		}

		if (shape.hasWall(Direction.EAST) && (se > 0 || ne > 0)) {
			quads.side(1, 1, 1, 0, se, ne, 1, 0, 0);
		}

		if (shape.hasWall(Direction.UP)) {
			// fan around the center, so the surface can bend towards each corner
			quads.top(0.5f, shape.center, 0.5f, 0, nw, 0, 0, sw, 1);
			quads.top(0.5f, shape.center, 0.5f, 0, sw, 1, 1, se, 1);
			quads.top(0.5f, shape.center, 0.5f, 1, se, 1, 1, ne, 0);
			quads.top(0.5f, shape.center, 0.5f, 1, ne, 0, 0, nw, 0);
		}

		if (shape.hasWall(Direction.DOWN)) {
			quads.vertex(0, 0, 1, 0, 1, 0, -1, 0);
			quads.vertex(0, 0, 0, 0, 0, 0, -1, 0);
			quads.vertex(1, 0, 0, 1, 0, 0, -1, 0);
			quads.vertex(1, 0, 1, 1, 1, 0, -1, 0);
		}
	}

	private record Quads(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, int color, int light) {

		void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
			consumer.addVertex(pose, x, y, z)
					.setColor(color)
					.setUv(sprite.getU(u), sprite.getV(v))
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, nx, ny, nz);
		}

		/**
		 * Vertical wall from corner A to corner B (counter-clockwise when seen from outside), with the fluid surface at heights {@code hA}/{@code hB}.
		 */
		void side(float xA, float xB, float zA, float zB, float hA, float hB, float nx, float ny, float nz) {
			vertex(xA, hA, zA, 0, 1 - hA, nx, ny, nz);
			vertex(xA, 0, zA, 0, 1, nx, ny, nz);
			vertex(xB, 0, zB, 1, 1, nx, ny, nz);
			vertex(xB, hB, zB, 1, 1 - hB, nx, ny, nz);
		}

		/**
		 * Triangle of the surface (emitted as quad with a repeated vertex).
		 */
		void top(float cx, float cy, float cz, float xA, float yA, float zA, float xB, float yB, float zB) {
			vertex(cx, cy, cz, cx, cz, 0, 1, 0);
			vertex(xA, yA, zA, xA, zA, 0, 1, 0);
			vertex(xB, yB, zB, xB, zB, 0, 1, 0);
			vertex(xB, yB, zB, xB, zB, 0, 1, 0);
		}
	}
}
