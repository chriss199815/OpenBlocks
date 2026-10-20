package openblocks.common;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Port of legacy {@code HeightMapData}: 64×64 pixels with a block height and a map color each, in two layers (terrain and
 * liquids). Saved per map like vanilla maps, see {@link HeightMaps}; the client keeps copies received over the network.
 * Heights are block y values (legacy stored them in a byte, which no longer fits the build height).
 */
public class HeightMapData extends SavedData {

	public static final int SIZE = 64;
	public static final int MAX_SCALE = 4;
	public static final Codec<Integer> SCALE_CODEC = Codec.intRange(0, MAX_SCALE);

	public static final int LAYER_TERRAIN = 0;
	public static final int LAYER_LIQUIDS = 1;
	private static final int[] LAYER_ALPHAS = { 255, 128 };

	public static final SavedData.Factory<HeightMapData> FACTORY = new SavedData.Factory<>(HeightMapData::new, HeightMapData::load, null);

	public static final StreamCodec<RegistryFriendlyByteBuf, HeightMapData> STREAM_CODEC = StreamCodec.of((buf, data) -> data.write(buf), HeightMapData::read);

	private static final StreamCodec<ByteBuf, Optional<ResourceKey<Level>>> DIMENSION_STREAM_CODEC = ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION));

	private static final String TAG_DIMENSION = "Dimension";
	private static final String TAG_CENTER_X = "CenterX";
	private static final String TAG_CENTER_Z = "CenterZ";
	private static final String TAG_SCALE = "Scale";
	private static final String TAG_LAYERS = "Layers";
	private static final String TAG_ALPHA = "Alpha";
	private static final String TAG_HEIGHTS = "Height";
	private static final String TAG_COLORS = "Color";

	/**
	 * Pixels are stored row by row: index {@code y * SIZE + x}, with x along world x and y along world z.
	 * Color 0 ({@code MapColor.NONE}) means there is no pixel.
	 */
	public record Layer(int alpha, int[] heights, byte[] colors) {
		public Layer(int alpha) {
			this(alpha, new int[SIZE * SIZE], new byte[SIZE * SIZE]);
		}
	}

	@Nullable
	private ResourceKey<Level> dimension;
	private int centerX;
	private int centerZ;
	private int scale;
	private List<Layer> layers = List.of();

	@Nullable
	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterZ() {
		return centerZ;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public List<Layer> getLayers() {
		return layers;
	}

	// for computers (legacy AdapterProjector), which may edit maps freely; callers mark the map updated

	public void setCenter(int centerX, int centerZ) {
		this.centerX = centerX;
		this.centerZ = centerZ;
	}

	public void setDimension(@Nullable ResourceKey<Level> dimension) {
		this.dimension = dimension;
	}

	public void setLayers(List<Layer> layers) {
		this.layers = List.copyOf(layers);
	}

	/**
	 * Legacy {@code MapDataBuilder.resetMap}: moves the map, keeping old pixels until they are mapped again.
	 */
	public void reset(ResourceKey<Level> dimension, int centerX, int centerZ) {
		this.dimension = dimension;
		this.centerX = centerX;
		this.centerZ = centerZ;
		if (layers.size() != LAYER_ALPHAS.length) {
			final List<Layer> newLayers = new ArrayList<>();
			for (int alpha : LAYER_ALPHAS)
				newLayers.add(new Layer(alpha));
			layers = newLayers;
		}
		setDirty();
	}

	/**
	 * Copy sent to clients; in single player, payloads are not serialized, and the client must not see later changes.
	 */
	public HeightMapData copy() {
		final HeightMapData result = new HeightMapData();
		result.dimension = dimension;
		result.centerX = centerX;
		result.centerZ = centerZ;
		result.scale = scale;
		result.layers = layers.stream().map(layer -> new Layer(layer.alpha(), layer.heights().clone(), layer.colors().clone())).toList();
		return result;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		if (dimension != null) tag.putString(TAG_DIMENSION, dimension.location().toString());
		tag.putInt(TAG_CENTER_X, centerX);
		tag.putInt(TAG_CENTER_Z, centerZ);
		tag.putByte(TAG_SCALE, (byte)scale);
		final ListTag layersTag = new ListTag();
		for (Layer layer : layers) {
			final CompoundTag layerTag = new CompoundTag();
			layerTag.putInt(TAG_ALPHA, layer.alpha());
			layerTag.putIntArray(TAG_HEIGHTS, layer.heights());
			layerTag.putByteArray(TAG_COLORS, layer.colors());
			layersTag.add(layerTag);
		}
		tag.put(TAG_LAYERS, layersTag);
		return tag;
	}

	private static HeightMapData load(CompoundTag tag, HolderLookup.Provider registries) {
		final HeightMapData data = new HeightMapData();
		final ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
		if (dimension != null && tag.contains(TAG_DIMENSION)) data.dimension = ResourceKey.create(Registries.DIMENSION, dimension);
		data.centerX = tag.getInt(TAG_CENTER_X);
		data.centerZ = tag.getInt(TAG_CENTER_Z);
		data.scale = Math.clamp(tag.getByte(TAG_SCALE), 0, MAX_SCALE);
		final List<Layer> layers = new ArrayList<>();
		for (Tag entry : tag.getList(TAG_LAYERS, Tag.TAG_COMPOUND)) {
			final CompoundTag layerTag = (CompoundTag)entry;
			final int[] heights = layerTag.getIntArray(TAG_HEIGHTS);
			final byte[] colors = layerTag.getByteArray(TAG_COLORS);
			// broken entries would crash the renderer
			if (heights.length == SIZE * SIZE && colors.length == SIZE * SIZE) layers.add(new Layer(layerTag.getInt(TAG_ALPHA), heights, colors));
		}
		data.layers = layers;
		return data;
	}

	private void write(RegistryFriendlyByteBuf buf) {
		DIMENSION_STREAM_CODEC.encode(buf, Optional.ofNullable(dimension));
		buf.writeInt(centerX);
		buf.writeInt(centerZ);
		buf.writeByte(scale);
		buf.writeVarInt(layers.size());
		for (Layer layer : layers) {
			buf.writeByte(layer.alpha());
			for (int height : layer.heights())
				buf.writeShort(height);
			buf.writeBytes(layer.colors());
		}
	}

	private static HeightMapData read(RegistryFriendlyByteBuf buf) {
		final HeightMapData data = new HeightMapData();
		data.dimension = DIMENSION_STREAM_CODEC.decode(buf).orElse(null);
		data.centerX = buf.readInt();
		data.centerZ = buf.readInt();
		data.scale = buf.readByte();
		final int count = buf.readVarInt();
		final List<Layer> layers = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			final Layer layer = new Layer(buf.readUnsignedByte());
			for (int pixel = 0; pixel < SIZE * SIZE; pixel++)
				layer.heights()[pixel] = buf.readShort();
			buf.readBytes(layer.colors());
			layers.add(layer);
		}
		data.layers = layers;
		return data;
	}
}
