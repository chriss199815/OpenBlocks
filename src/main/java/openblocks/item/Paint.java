package openblocks.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * Paint carried by a paint can or brush: its color and how many uses are left. Replaces the legacy {@code color}/{@code amount} NBT tags.
 */
public record Paint(int color, int amount) {

	public static final Codec<Paint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("color").forGetter(Paint::color),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("amount").forGetter(Paint::amount)
	).apply(instance, Paint::new));

	public static final StreamCodec<ByteBuf, Paint> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, Paint::color,
			ByteBufCodecs.VAR_INT, Paint::amount,
			Paint::new);

	public Paint withAmount(int newAmount) {
		return new Paint(color, newAmount);
	}
}
