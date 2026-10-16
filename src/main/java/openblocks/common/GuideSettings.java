package openblocks.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

/**
 * Size, shape, marker color and redstone mode of a building guide, kept in the item when it is broken; replaces the
 * legacy {@code NegX}…{@code PosZ}, {@code Mode} and {@code Color} tags. The sizes are in the local space of the guide:
 * how far the shape reaches from it along each half axis.
 */
public record GuideSettings(int negX, int negY, int negZ, int posX, int posY, int posZ, GuideShape shape, int color, GuideRedstoneMode redstoneMode) {

	public static final int DEFAULT_SIZE = 8;
	// legacy had no limit; bigger shapes have too many markers to draw
	public static final int MAX_SIZE = 64;
	public static final int DEFAULT_COLOR = 0xFFFFFF;

	public static final GuideSettings DEFAULT = new GuideSettings(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE,
			GuideShape.SPHERE, DEFAULT_COLOR, GuideRedstoneMode.ALWAYS);

	private static final Codec<Integer> SIZE_CODEC = ExtraCodecs.intRange(0, MAX_SIZE);

	public static final Codec<GuideSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SIZE_CODEC.fieldOf("neg_x").forGetter(GuideSettings::negX),
			SIZE_CODEC.fieldOf("neg_y").forGetter(GuideSettings::negY),
			SIZE_CODEC.fieldOf("neg_z").forGetter(GuideSettings::negZ),
			SIZE_CODEC.fieldOf("pos_x").forGetter(GuideSettings::posX),
			SIZE_CODEC.fieldOf("pos_y").forGetter(GuideSettings::posY),
			SIZE_CODEC.fieldOf("pos_z").forGetter(GuideSettings::posZ),
			GuideShape.CODEC.fieldOf("shape").forGetter(GuideSettings::shape),
			Codec.INT.fieldOf("color").forGetter(GuideSettings::color),
			GuideRedstoneMode.CODEC.optionalFieldOf("redstone_mode", GuideRedstoneMode.ALWAYS).forGetter(GuideSettings::redstoneMode))
			.apply(instance, GuideSettings::new));

	public static final StreamCodec<ByteBuf, GuideSettings> STREAM_CODEC = StreamCodec.of((buf, settings) -> {
		for (Direction axis : Direction.values())
			ByteBufCodecs.VAR_INT.encode(buf, settings.size(axis));
		GuideShape.STREAM_CODEC.encode(buf, settings.shape);
		ByteBufCodecs.INT.encode(buf, settings.color);
		GuideRedstoneMode.STREAM_CODEC.encode(buf, settings.redstoneMode);
	}, buf -> {
		final int[] sizes = new int[Direction.values().length];
		for (Direction axis : Direction.values())
			sizes[axis.ordinal()] = ByteBufCodecs.VAR_INT.decode(buf);
		return new GuideSettings(sizes[Direction.WEST.ordinal()], sizes[Direction.DOWN.ordinal()], sizes[Direction.NORTH.ordinal()],
				sizes[Direction.EAST.ordinal()], sizes[Direction.UP.ordinal()], sizes[Direction.SOUTH.ordinal()],
				GuideShape.STREAM_CODEC.decode(buf), ByteBufCodecs.INT.decode(buf), GuideRedstoneMode.STREAM_CODEC.decode(buf));
	});

	/**
	 * How far the shape reaches along a local half axis.
	 */
	public int size(Direction halfAxis) {
		return switch (halfAxis) {
			case WEST -> negX;
			case DOWN -> negY;
			case NORTH -> negZ;
			case EAST -> posX;
			case UP -> posY;
			case SOUTH -> posZ;
		};
	}

	/**
	 * Sets how far the shape reaches along a local half axis, limited to 0..{@link #MAX_SIZE}.
	 */
	public GuideSettings withSize(Direction halfAxis, int size) {
		final int s = Mth.clamp(size, 0, MAX_SIZE);
		return switch (halfAxis) {
			case WEST -> new GuideSettings(s, negY, negZ, posX, posY, posZ, shape, color, redstoneMode);
			case DOWN -> new GuideSettings(negX, s, negZ, posX, posY, posZ, shape, color, redstoneMode);
			case NORTH -> new GuideSettings(negX, negY, s, posX, posY, posZ, shape, color, redstoneMode);
			case EAST -> new GuideSettings(negX, negY, negZ, s, posY, posZ, shape, color, redstoneMode);
			case UP -> new GuideSettings(negX, negY, negZ, posX, s, posZ, shape, color, redstoneMode);
			case SOUTH -> new GuideSettings(negX, negY, negZ, posX, posY, s, shape, color, redstoneMode);
		};
	}

	public GuideSettings withShape(GuideShape shape) {
		return new GuideSettings(negX, negY, negZ, posX, posY, posZ, shape, color, redstoneMode);
	}

	public GuideSettings withColor(int color) {
		return new GuideSettings(negX, negY, negZ, posX, posY, posZ, shape, color & 0xFFFFFF, redstoneMode);
	}

	public GuideSettings withRedstoneMode(GuideRedstoneMode redstoneMode) {
		return new GuideSettings(negX, negY, negZ, posX, posY, posZ, shape, color, redstoneMode);
	}

	/**
	 * Whether both give the same marker positions (color and redstone mode do not matter).
	 */
	public boolean sameShape(GuideSettings other) {
		return negX == other.negX && negY == other.negY && negZ == other.negZ
				&& posX == other.posX && posY == other.posY && posZ == other.posZ && shape == other.shape;
	}

	/**
	 * The box like legacy shows it: {@code (-negX,-negY,-negZ):(posX,posY,posZ)}.
	 */
	public Object[] boxArgs() {
		return new Object[] { -negX, -negY, -negZ, posX, posY, posZ };
	}

	public void addToTooltip(Consumer<Component> tooltip) {
		tooltip.accept(Component.translatable("openblocks.misc.box", boxArgs()).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("openblocks.misc.color", String.format("%06X", color)).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("openblocks.misc.shape", shape.displayName()).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("openblocks.gui.guide.redstone", redstoneMode.displayName()).withStyle(ChatFormatting.GRAY));
	}
}
