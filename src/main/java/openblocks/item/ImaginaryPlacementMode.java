package openblocks.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import openblocks.block.ImaginaryBlock;

/**
 * Shape a pencil or crayon draws and how many uses it costs; legacy {@code ItemImaginary.PlacementMode}. The order is
 * the cycle order and the {@code openblocks:mode} item property.
 */
public enum ImaginaryPlacementMode implements StringRepresentable {
	BLOCK(1.0f, "block", "block", false, ImaginaryBlock.Shape.BLOCK),
	PANEL(0.5f, "panel", "panel", false, ImaginaryBlock.Shape.PANEL),
	HALF_PANEL(0.5f, "half_panel", "half", false, ImaginaryBlock.Shape.HALF_PANEL),
	STAIRS(0.75f, "stairs", "stairs", false, ImaginaryBlock.Shape.STAIRS),

	INVERTED_BLOCK(1.5f, "inverted_block", "inverted_block", true, ImaginaryBlock.Shape.BLOCK),
	INVERTED_PANEL(1.0f, "inverted_panel", "inverted_panel", true, ImaginaryBlock.Shape.PANEL),
	INVERTED_HALF_PANEL(1.0f, "inverted_half_panel", "inverted_half", true, ImaginaryBlock.Shape.HALF_PANEL),
	INVERTED_STAIRS(1.25f, "inverted_stairs", "inverted_stairs", true, ImaginaryBlock.Shape.STAIRS);

	public static final Codec<ImaginaryPlacementMode> CODEC = StringRepresentable.fromEnum(ImaginaryPlacementMode::values);
	private static final IntFunction<ImaginaryPlacementMode> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, ImaginaryPlacementMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

	public final float cost;
	private final String name;
	/**
	 * Suffix of the item model and its overlay texture.
	 */
	public final String overlay;
	public final boolean inverted;
	public final ImaginaryBlock.Shape shape;

	ImaginaryPlacementMode(float cost, String name, String overlay, boolean inverted, ImaginaryBlock.Shape shape) {
		this.cost = cost;
		this.name = name;
		this.overlay = overlay;
		this.inverted = inverted;
		this.shape = shape;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public Component displayName() {
		return Component.translatable("openblocks.misc.mode." + name);
	}

	public ImaginaryPlacementMode next() {
		return BY_ID.apply((ordinal() + 1) % values().length);
	}

	public static ImaginaryPlacementMode of(ImaginaryBlock.Shape shape, boolean inverted) {
		for (ImaginaryPlacementMode mode : values())
			if (mode.shape == shape && mode.inverted == inverted) return mode;
		return BLOCK;
	}
}
