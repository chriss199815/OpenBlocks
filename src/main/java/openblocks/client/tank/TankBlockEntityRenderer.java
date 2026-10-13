package openblocks.client.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import openblocks.block.entity.TankBlockEntity;

/**
 * Port of legacy {@code TileEntityTankRenderer} + {@code TankRenderLogic}. Instead of keeping shared connection objects between tanks,
 * neighbours are looked up every frame: walls between tanks of the same fluid are skipped and corner heights are averaged, so the
 * surface of connected tanks is continuous.
 */
public class TankBlockEntityRenderer implements BlockEntityRenderer<TankBlockEntity> {

	private static final float UPPER_CLAMP_LIMIT = 0.95f;
	private static final float UPPER_CLAMP_VALUE = 1.0f;
	private static final float LOWER_CLAMP_LIMIT = 0.05f;
	private static final float LOWER_CLAMP_VALUE = 0.05f;

	private static final float PHASE_PER_DISTANCE = 0.5f;
	private static final float WAVE_AMPLITUDE = 0.025f;
	private static final float WAVE_FREQUENCY = 0.1f;

	public TankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(TankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		final FluidStack fluid = tank.getFluid();
		final Level level = tank.getLevel();
		if (fluid.isEmpty() || level == null) return;

		final float time = level.getGameTime() + partialTick;
		final BlockPos pos = tank.getBlockPos();

		final float nw = cornerHeight(level, pos, fluid, Direction.NORTH, Direction.WEST, time);
		final float ne = cornerHeight(level, pos, fluid, Direction.NORTH, Direction.EAST, time);
		final float se = cornerHeight(level, pos, fluid, Direction.SOUTH, Direction.EAST, time);
		final float sw = cornerHeight(level, pos, fluid, Direction.SOUTH, Direction.WEST, time);
		final float center = renderHeight(time, phase(pos.getX(), pos.getY(), pos.getZ()), (float)tank.getFluidRatio());

		final boolean[] walls = new boolean[6];
		for (Direction side : Direction.Plane.HORIZONTAL)
			walls[side.get3DDataValue()] = !holdsSameFluid(level, pos.relative(side), fluid);

		// vertical neighbours only merge when the lower tank is full, otherwise its surface is visible
		final TankBlockEntity above = getTank(level, pos.above());
		walls[Direction.UP.get3DDataValue()] = !(above != null && isSameFluid(above, fluid) && tank.getFluidRatio() >= 1);
		final TankBlockEntity below = getTank(level, pos.below());
		walls[Direction.DOWN.get3DDataValue()] = !(below != null && isSameFluid(below, fluid) && below.getFluidRatio() >= 1);

		TankFluidRenderer.render(poseStack, buffers, fluid, new TankFluidRenderer.Shape(nw, ne, se, sw, center, walls), light);
	}

	/**
	 * Average level of all tanks with this fluid that share the corner; replaces legacy {@code DiagonalConnection}.
	 */
	private static float cornerHeight(Level level, BlockPos pos, FluidStack fluid, Direction a, Direction b, float time) {
		float sum = 0;
		int count = 0;
		for (BlockPos p : new BlockPos[] { pos, pos.relative(a), pos.relative(b), pos.relative(a).relative(b) }) {
			final TankBlockEntity tank = getTank(level, p);
			if (tank != null && isSameFluid(tank, fluid)) {
				sum += (float)tank.getFluidRatio();
				count++;
			}
		}

		final float x = pos.getX() + (a.getStepX() + b.getStepX() + 1) / 2.0f;
		final float z = pos.getZ() + (a.getStepZ() + b.getStepZ() + 1) / 2.0f;
		return renderHeight(time, phase(x, pos.getY(), z), clampLevel(sum / count));
	}

	@Nullable
	private static TankBlockEntity getTank(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof TankBlockEntity tank? tank : null;
	}

	private static boolean isSameFluid(TankBlockEntity tank, FluidStack fluid) {
		return FluidStack.isSameFluidSameComponents(tank.getFluid(), fluid);
	}

	private static boolean holdsSameFluid(Level level, BlockPos pos, FluidStack fluid) {
		final TankBlockEntity tank = getTank(level, pos);
		return tank != null && isSameFluid(tank, fluid);
	}

	private static float clampLevel(float level) {
		if (level <= LOWER_CLAMP_LIMIT) return LOWER_CLAMP_VALUE;
		if (level >= UPPER_CLAMP_LIMIT) return UPPER_CLAMP_VALUE;
		return level;
	}

	private static float phase(float x, float y, float z) {
		return (x + y + z) * PHASE_PER_DISTANCE;
	}

	private static float renderHeight(float time, float phase, float level) {
		// prevent seams when tank is in the middle of column
		if (level >= 1.0f) return 1.0f;
		level = clampLevel(level) + Mth.sin(time * WAVE_FREQUENCY + phase) * WAVE_AMPLITUDE;
		return Mth.clamp(level, 0.0f, 1.0f);
	}

	@Override
	public boolean shouldRenderOffScreen(TankBlockEntity tank) {
		return false;
	}
}
