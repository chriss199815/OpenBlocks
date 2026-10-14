package openblocks.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The structure a golden eye is locked on. Replaces the legacy {@code Structure}/{@code X}/{@code Y}/{@code Z} NBT tags;
 * unlike legacy it also knows the dimension.
 */
public record GoldenEyeTarget(ResourceKey<Structure> structure, GlobalPos pos) {

	public static final Codec<GoldenEyeTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.STRUCTURE).fieldOf("structure").forGetter(GoldenEyeTarget::structure),
			GlobalPos.CODEC.fieldOf("pos").forGetter(GoldenEyeTarget::pos)
	).apply(instance, GoldenEyeTarget::new));

	public static final StreamCodec<ByteBuf, GoldenEyeTarget> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.STRUCTURE), GoldenEyeTarget::structure,
			GlobalPos.STREAM_CODEC, GoldenEyeTarget::pos,
			GoldenEyeTarget::new);

	/**
	 * Structures have no names in vanilla; ours are in the lang files, others fall back to their id.
	 */
	public static Component structureName(ResourceKey<Structure> structure) {
		final ResourceLocation id = structure.location();
		return Component.translatableWithFallback("structure." + id.getNamespace() + "." + id.getPath(), id.toString());
	}
}
