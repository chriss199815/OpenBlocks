package openblocks.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What a cartographer carries: the map, whether it is mapping, in which dimension, and which chunks it has done. Saved
 * with the entity and on the item of a picked up cartographer.
 */
public record CartographerData(ItemStack map, boolean mapping, Optional<ResourceKey<Level>> dimension, List<Long> finishedChunks) {

	public static final CartographerData EMPTY = new CartographerData(ItemStack.EMPTY, false, Optional.empty(), List.of());

	public static final Codec<CartographerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("map", ItemStack.EMPTY).forGetter(CartographerData::map),
			Codec.BOOL.optionalFieldOf("mapping", false).forGetter(CartographerData::mapping),
			Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension").forGetter(CartographerData::dimension),
			Codec.LONG.listOf().optionalFieldOf("finished_chunks", List.of()).forGetter(CartographerData::finishedChunks))
			.apply(instance, CartographerData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CartographerData> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, CartographerData::map,
			ByteBufCodecs.BOOL, CartographerData::mapping,
			ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), CartographerData::dimension,
			ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), CartographerData::finishedChunks,
			CartographerData::new);

	public static CartographerData of(ItemStack map, boolean mapping, Optional<ResourceKey<Level>> dimension, BitSet finishedChunks) {
		return new CartographerData(map.copy(), mapping, dimension, LongStream.of(finishedChunks.toLongArray()).boxed().toList());
	}

	public BitSet finishedChunkBits() {
		return BitSet.valueOf(finishedChunks.stream().mapToLong(Long::longValue).toArray());
	}

	// item stacks do not compare by content, but components must
	@Override
	public boolean equals(Object obj) {
		return obj instanceof CartographerData other && ItemStack.matches(map, other.map) && mapping == other.mapping
				&& dimension.equals(other.dimension) && finishedChunks.equals(other.finishedChunks);
	}

	@Override
	public int hashCode() {
		int result = ItemStack.hashItemAndComponents(map);
		result = 31 * result + Boolean.hashCode(mapping);
		result = 31 * result + dimension.hashCode();
		return 31 * result + finishedChunks.hashCode();
	}
}
