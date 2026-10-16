package openblocks.common;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * When a building guide shows its shape. Legacy had one setting for all guides ({@code guide.redstoneSensitivity});
 * here each guide has its own, the config only gives the mode of new guides.
 */
public enum GuideRedstoneMode implements StringRepresentable {
	ALWAYS,
	POWERED,
	UNPOWERED;

	public static final Codec<GuideRedstoneMode> CODEC = StringRepresentable.fromEnum(GuideRedstoneMode::values);
	public static final StreamCodec<ByteBuf, GuideRedstoneMode> STREAM_CODEC = ByteBufCodecs.idMapper(
			ByIdMap.continuous(GuideRedstoneMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP), GuideRedstoneMode::ordinal);

	/**
	 * Legacy {@code redstoneSensitivity}: 0 - always, 1 - powered, -1 - unpowered.
	 */
	public static GuideRedstoneMode fromSensitivity(int sensitivity) {
		return sensitivity == 0? ALWAYS : sensitivity > 0? POWERED : UNPOWERED;
	}

	public boolean isEnabled(boolean powered) {
		return switch (this) {
			case ALWAYS -> true;
			case POWERED -> powered;
			case UNPOWERED -> !powered;
		};
	}

	public GuideRedstoneMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public Component displayName() {
		return Component.translatable("openblocks.gui.guide.redstone." + getSerializedName());
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
