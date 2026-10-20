package openblocks.integration.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import openblocks.block.entity.ProjectorBlockEntity;
import openblocks.common.HeightMapData;
import openblocks.common.HeightMaps;

/**
 * Legacy {@code AdapterProjector}: reads and edits the height map in the projector. Like legacy, layers and rows/columns
 * count from 0; heights are block y values now and dimensions are ids ({@code minecraft:overworld}) instead of numbers.
 */
class ProjectorPeripheral extends BlockEntityPeripheral<ProjectorBlockEntity> {

	ProjectorPeripheral(ProjectorBlockEntity projector) {
		super(projector, "openblocks_projector");
	}

	@LuaFunction(mainThread = true)
	@Nullable
	public final Integer getMapId() {
		return blockEntity.getMapId();
	}

	@LuaFunction(mainThread = true)
	public final Map<String, Object> getMapInfo() throws LuaException {
		final HeightMapData data = map();
		final Map<String, Object> result = new HashMap<>();
		result.put("center_x", data.getCenterX());
		result.put("center_z", data.getCenterZ());
		result.put("scale", data.getScale());
		if (data.getDimension() != null) result.put("dimension", data.getDimension().location().toString());
		final Map<Integer, Integer> layers = new LinkedHashMap<>();
		for (int i = 0; i < data.getLayers().size(); i++)
			layers.put(i, data.getLayers().get(i).alpha());
		result.put("layers", layers);
		return result;
	}

	/**
	 * Takes the keys of {@link #getMapInfo()} except {@code scale}; missing keys stay as they are, {@code layers} sets alphas.
	 */
	@LuaFunction(mainThread = true)
	public final void setMapInfo(Map<?, ?> properties) throws LuaException {
		final HeightMapData data = map();
		int centerX = data.getCenterX();
		int centerZ = data.getCenterZ();
		if (properties.get("center_x") != null) centerX = toInt(properties.get("center_x"), "center_x");
		if (properties.get("center_z") != null) centerZ = toInt(properties.get("center_z"), "center_z");

		ResourceKey<Level> dimension = data.getDimension();
		if (properties.get("dimension") instanceof String id) {
			final ResourceLocation location = ResourceLocation.tryParse(id);
			if (location == null) throw new LuaException("Invalid dimension " + id);
			dimension = ResourceKey.create(Registries.DIMENSION, location);
		} else if (properties.get("dimension") != null) {
			throw new LuaException("Dimension must be an id like minecraft:overworld");
		}

		final List<HeightMapData.Layer> layers = new ArrayList<>(data.getLayers());
		if (properties.get("layers") instanceof Map<?, ?> alphas) {
			for (Map.Entry<?, ?> entry : alphas.entrySet()) {
				final int index = toInt(entry.getKey(), "layer");
				checkIndex(index, layers.size(), "layer");
				final HeightMapData.Layer layer = layers.get(index);
				layers.set(index, new HeightMapData.Layer(checkRange(toInt(entry.getValue(), "alpha"), 0, 255, "alpha"), layer.heights(), layer.colors()));
			}
		} else if (properties.get("layers") != null) {
			throw new LuaException("Layers must be a table of layer index to alpha");
		}

		data.setCenter(centerX, centerZ);
		data.setDimension(dimension);
		data.setLayers(layers);
		markUpdated(data);
	}

	@LuaFunction(mainThread = true)
	public final int getRotation() {
		return blockEntity.getRotation();
	}

	/**
	 * Quarter turns, positive clockwise.
	 */
	@LuaFunction(mainThread = true)
	public final void rotate(int delta) {
		blockEntity.rotate(delta);
	}

	@LuaFunction(mainThread = true)
	public final MethodResult getPoint(int row, int column, int layer) throws LuaException {
		final HeightMapData.Layer data = layer(map(), layer);
		final int index = pixel(row, column);
		return MethodResult.of(data.heights()[index], Byte.toUnsignedInt(data.colors()[index]));
	}

	/**
	 * Color is a map color id (0 = no pixel), like the legacy color argument.
	 */
	@LuaFunction(mainThread = true)
	public final void setPoint(int row, int column, int layer, int height, int color) throws LuaException {
		final HeightMapData data = map();
		final HeightMapData.Layer target = layer(data, layer);
		final int index = pixel(row, column);
		// heights travel to clients as shorts
		checkRange(height, Short.MIN_VALUE, Short.MAX_VALUE, "height");
		// vanilla has 64 map color ids
		checkRange(color, 0, 63, "color");
		if (MapColor.byId(color) == MapColor.NONE && color != 0) throw new LuaException("Unknown map color " + color);
		target.heights()[index] = height;
		target.colors()[index] = (byte)color;
		markUpdated(data);
	}

	@LuaFunction(mainThread = true)
	public final void clearMap() throws LuaException {
		final HeightMapData data = map();
		data.setLayers(List.of());
		markUpdated(data);
	}

	/**
	 * Replaces a layer with an empty, opaque one.
	 */
	@LuaFunction(mainThread = true)
	public final void clearLayer(int layer) throws LuaException {
		final HeightMapData data = map();
		final List<HeightMapData.Layer> layers = new ArrayList<>(data.getLayers());
		checkIndex(layer, layers.size(), "layer");
		layers.set(layer, new HeightMapData.Layer(255));
		data.setLayers(layers);
		markUpdated(data);
	}

	/**
	 * Adds an empty, opaque layer and returns its index.
	 */
	@LuaFunction(mainThread = true)
	public final int appendLayer() throws LuaException {
		final HeightMapData data = map();
		final List<HeightMapData.Layer> layers = new ArrayList<>(data.getLayers());
		layers.add(new HeightMapData.Layer(255));
		data.setLayers(layers);
		markUpdated(data);
		return layers.size() - 1;
	}

	private HeightMapData map() throws LuaException {
		final Integer mapId = blockEntity.getMapId();
		if (mapId == null || !(blockEntity.getLevel() instanceof ServerLevel level)) throw new LuaException("No map in projector");
		final HeightMapData data = HeightMaps.get(level.getServer(), mapId);
		if (data == null) throw new LuaException("Map not loaded");
		return data;
	}

	private void markUpdated(HeightMapData data) {
		final Integer mapId = blockEntity.getMapId();
		if (mapId != null) HeightMaps.markUpdated(mapId, data);
	}

	private static HeightMapData.Layer layer(HeightMapData data, int layer) throws LuaException {
		checkIndex(layer, data.getLayers().size(), "layer");
		return data.getLayers().get(layer);
	}

	private static int pixel(int row, int column) throws LuaException {
		checkIndex(row, HeightMapData.SIZE, "row");
		checkIndex(column, HeightMapData.SIZE, "column");
		return row * HeightMapData.SIZE + column;
	}

	private static void checkIndex(int index, int size, String name) throws LuaException {
		if (index < 0 || index >= size) throw new LuaException(name + " must be between 0 and " + (size - 1));
	}

	private static int checkRange(int value, int min, int max, String name) throws LuaException {
		if (value < min || value > max) throw new LuaException(name + " must be between " + min + " and " + max);
		return value;
	}

	private static int toInt(Object value, String name) throws LuaException {
		if (value instanceof Number number) return number.intValue();
		throw new LuaException(name + " must be a number");
	}
}
