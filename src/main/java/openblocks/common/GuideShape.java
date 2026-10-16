package openblocks.common;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import openblocks.util.shapes.ShapeGenerator;
import openblocks.util.shapes.ShapeGenerators;
import openblocks.util.shapes.ShapeGeometry.Octant;

/**
 * Port of legacy {@code GuideShape}: the shapes a building guide can show, in the order the mode buttons cycle them.
 */
public enum GuideShape implements StringRepresentable {
	SPHERE(ShapeGenerators.sphere(Octant.ALL)),
	CYLINDER(ShapeGenerators.cylinder()),
	CUBOID(ShapeGenerators.cuboid(false)),
	FULL_CUBOID(ShapeGenerators.cuboid(true)),
	DOME(ShapeGenerators.sphere(Octant.SOUTH)),
	TRIANGLE(ShapeGenerators.polygon(3)),
	PENTAGON(ShapeGenerators.polygon(5)),
	HEXAGON(ShapeGenerators.polygon(6)),
	OCTAGON(ShapeGenerators.polygon(8)),
	AXES(ShapeGenerators.axes()),
	PLANES(ShapeGenerators.planes());

	public static final Codec<GuideShape> CODEC = StringRepresentable.fromEnum(GuideShape::values);
	public static final StreamCodec<ByteBuf, GuideShape> STREAM_CODEC = ByteBufCodecs.idMapper(
			ByIdMap.continuous(GuideShape::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP), GuideShape::ordinal);

	private final ShapeGenerator generator;

	GuideShape(ShapeGenerator generator) {
		this.generator = generator;
	}

	public ShapeGenerator generator() {
		return generator;
	}

	public GuideShape next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public GuideShape previous() {
		return values()[(ordinal() + values().length - 1) % values().length];
	}

	public Component displayName() {
		return Component.translatable("openblocks.misc.shape." + getSerializedName());
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
