package openblocks.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;

/**
 * The block a cursor is linked to, and the side it was clicked on. Replaces the legacy {@code dimension}/coordinates/{@code side} NBT tags.
 */
public record CursorTarget(GlobalPos pos, Direction side) {

	public static final Codec<CursorTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.fieldOf("pos").forGetter(CursorTarget::pos),
			Direction.CODEC.fieldOf("side").forGetter(CursorTarget::side)
	).apply(instance, CursorTarget::new));

	public static final StreamCodec<ByteBuf, CursorTarget> STREAM_CODEC = StreamCodec.composite(
			GlobalPos.STREAM_CODEC, CursorTarget::pos,
			Direction.STREAM_CODEC, CursorTarget::side,
			CursorTarget::new);
}
