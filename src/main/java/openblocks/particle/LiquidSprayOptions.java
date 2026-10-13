package openblocks.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import openblocks.registry.OBParticleTypes;

/**
 * Droplet textured with a fluid; replaces legacy {@code FXLiquidSpray} (used by the XP shower and the sprinkler).
 */
public record LiquidSprayOptions(FluidStack fluid, float scale, float gravity) implements ParticleOptions {

	public static final MapCodec<LiquidSprayOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			FluidStack.CODEC.fieldOf("fluid").forGetter(LiquidSprayOptions::fluid),
			Codec.FLOAT.fieldOf("scale").forGetter(LiquidSprayOptions::scale),
			Codec.FLOAT.fieldOf("gravity").forGetter(LiquidSprayOptions::gravity)
	).apply(instance, LiquidSprayOptions::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, LiquidSprayOptions> STREAM_CODEC = StreamCodec.composite(
			FluidStack.STREAM_CODEC, LiquidSprayOptions::fluid,
			ByteBufCodecs.FLOAT, LiquidSprayOptions::scale,
			ByteBufCodecs.FLOAT, LiquidSprayOptions::gravity,
			LiquidSprayOptions::new);

	@Override
	public ParticleType<?> getType() {
		return OBParticleTypes.LIQUID_SPRAY.get();
	}
}
