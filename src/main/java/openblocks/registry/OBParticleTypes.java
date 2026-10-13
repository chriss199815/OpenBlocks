package openblocks.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.particle.LiquidSprayOptions;

public class OBParticleTypes {

	public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, OpenBlocks.MODID);

	public static final DeferredHolder<ParticleType<?>, ParticleType<LiquidSprayOptions>> LIQUID_SPRAY = PARTICLE_TYPES.register("liquid_spray", () -> new ParticleType<>(false) {
		@Override
		public MapCodec<LiquidSprayOptions> codec() {
			return LiquidSprayOptions.CODEC;
		}

		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, LiquidSprayOptions> streamCodec() {
			return LiquidSprayOptions.STREAM_CODEC;
		}
	});
}
