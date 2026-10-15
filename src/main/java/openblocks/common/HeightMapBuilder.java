package openblocks.common;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code MapDataBuilder}: maps a height map chunk by chunk. Which chunks are done is kept in a bit set by
 * the caller (the cartographer). Only loaded chunks are mapped, nearest first.
 */
public class HeightMapBuilder {

	/**
	 * Blocks the cartographer looks through; replaces the legacy {@code cartographer.blockBlacklist} config.
	 */
	public static final TagKey<Block> INVISIBLE = TagKey.create(Registries.BLOCK, OpenBlocks.location("invisible_to_cartographer"));

	/**
	 * Legacy {@code MapDataBuilder.size()}: chunks along a side of the map.
	 */
	public static int chunksPerSide(int scale) {
		return 4 << scale;
	}

	/**
	 * Centers the map on the chunk containing the given block.
	 */
	public static void start(ServerLevel level, HeightMapData data, int x, int z) {
		data.reset(level.dimension(), (x >> 4) << 4, (z >> 4) << 4);
	}

	/**
	 * Maps the unfinished loaded chunk nearest to the given position.
	 *
	 * @return false when no unfinished chunk is loaded
	 */
	public static boolean mapNextChunk(ServerLevel level, HeightMapData data, BitSet finished, double x, double z) {
		if (data.getLayers().isEmpty()) return false;

		final int side = chunksPerSide(data.getScale());
		final int firstChunkX = (data.getCenterX() >> 4) - side / 2;
		final int firstChunkZ = (data.getCenterZ() >> 4) - side / 2;

		int nearestBit = -1;
		LevelChunk nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (int bit = finished.nextClearBit(0); bit < side * side; bit = finished.nextClearBit(bit + 1)) {
			final ChunkPos pos = new ChunkPos(firstChunkX + bit / side, firstChunkZ + bit % side);
			final double dx = pos.getMinBlockX() + 7.5 - x;
			final double dz = pos.getMinBlockZ() + 7.5 - z;
			final double distance = dx * dx + dz * dz;
			if (distance >= nearestDistance) continue;
			final LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
			if (chunk != null) {
				nearest = chunk;
				nearestBit = bit;
				nearestDistance = distance;
			}
		}

		if (nearest == null) return false;
		final int pixelsPerChunk = HeightMapData.SIZE / side;
		mapChunk(level, data, nearest, nearestBit / side * pixelsPerChunk, nearestBit % side * pixelsPerChunk, pixelsPerChunk);
		finished.set(nearestBit);
		return true;
	}

	public static boolean isFinished(HeightMapData data, BitSet finished) {
		final int side = chunksPerSide(data.getScale());
		return finished.nextClearBit(0) >= side * side;
	}

	private static void mapChunk(ServerLevel level, HeightMapData data, LevelChunk chunk, int mapX, int mapY, int pixelsPerChunk) {
		final HeightMapData.Layer ground = data.getLayers().get(HeightMapData.LAYER_TERRAIN);
		final HeightMapData.Layer liquid = data.getLayers().get(HeightMapData.LAYER_LIQUIDS);
		final int blocksPerPixel = 16 / pixelsPerChunk;

		for (int px = 0; px < pixelsPerChunk; px++)
			for (int py = 0; py < pixelsPerChunk; py++) {
				final PixelSummary pixel = new PixelSummary();
				pixel.average(level, chunk, px * blocksPerPixel, py * blocksPerPixel, blocksPerPixel);

				final int index = (mapY + py) * HeightMapData.SIZE + mapX + px;
				ground.colors()[index] = (byte)pixel.groundColor;
				ground.heights()[index] = pixel.groundHeight;
				liquid.colors()[index] = (byte)pixel.liquidColor;
				liquid.heights()[index] = pixel.liquidHeight;
			}
	}

	/**
	 * Legacy {@code BlockCount}: most common color and average height of the top solid blocks and, when more than half
	 * of the columns have one, of the top liquids.
	 */
	private static class PixelSummary {
		int groundColor;
		int groundHeight;
		int liquidColor;
		int liquidHeight;

		void average(ServerLevel level, LevelChunk chunk, int startX, int startZ, int size) {
			final int[] groundColors = new int[64];
			final int[] liquidColors = new int[64];
			long groundHeightSum = 0;
			int groundCount = 0;
			long liquidHeightSum = 0;
			int liquidCount = 0;

			final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = startX; x < startX + size; x++)
				for (int z = startZ; z < startZ + size; z++) {
					boolean foundLiquid = false;
					for (int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z); y >= level.getMinBuildHeight(); y--) {
						pos.set(chunk.getPos().getBlockX(x), y, chunk.getPos().getBlockZ(z));
						final BlockState state = chunk.getBlockState(pos);
						final MapColor color = getMapColor(level, state, pos);
						if (color == null) continue;

						if (state.getBlock() instanceof LiquidBlock) {
							if (!foundLiquid) {
								foundLiquid = true;
								liquidHeightSum += y;
								liquidColors[color.id]++;
								liquidCount++;
							}
						} else {
							groundHeightSum += y;
							groundColors[color.id]++;
							groundCount++;
							break;
						}
					}
				}

			if (groundCount > 0) {
				groundColor = mostCommon(groundColors);
				groundHeight = (int)(groundHeightSum / groundCount);
			}

			if (liquidCount > size * size / 2) {
				liquidColor = mostCommon(liquidColors);
				liquidHeight = (int)(liquidHeightSum / liquidCount);
			}
		}

		@Nullable
		private static MapColor getMapColor(ServerLevel level, BlockState state, BlockPos pos) {
			if (state.isAir() || state.is(INVISIBLE)) return null;
			final MapColor color = state.getMapColor(level, pos);
			return color == MapColor.NONE? null : color;
		}

		private static int mostCommon(int[] counts) {
			int result = 0;
			for (int i = 1; i < counts.length; i++)
				if (counts[i] > counts[result]) result = i;
			return result;
		}
	}
}
