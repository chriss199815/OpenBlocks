package openblocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.util.shapes.ShapeGeometry;

/**
 * Server to client: the builder guide placed a block; shows portal and block particles along the line from the guide to
 * it. Replaces the legacy {@code IGuideAnimationTrigger} RPC.
 */
public record GuideAnimationPayload(BlockPos guide, BlockPos target, int stateId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<GuideAnimationPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("guide_animation"));

	public static final StreamCodec<RegistryFriendlyByteBuf, GuideAnimationPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, GuideAnimationPayload::guide,
			BlockPos.STREAM_CODEC, GuideAnimationPayload::target,
			ByteBufCodecs.VAR_INT, GuideAnimationPayload::stateId,
			GuideAnimationPayload::new);

	@Override
	public CustomPacketPayload.Type<GuideAnimationPayload> type() {
		return TYPE;
	}

	// only called on the client
	public static void handle(GuideAnimationPayload payload, IPayloadContext context) {
		final Level level = context.player().level();
		final RandomSource random = level.getRandom();
		final BlockState state = Block.stateById(payload.stateId());
		final BlockParticleOption blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, state);
		final BlockPos from = payload.guide();
		final BlockPos to = payload.target();
		ShapeGeometry.line3D(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), (x, y, z) -> {
			for (int i = 0; i < 5; i++) {
				final double px = x + 0.5 + 0.3 * random.nextFloat();
				final double py = y + 0.5 + 0.3 * random.nextFloat();
				final double pz = z + 0.5 + 0.3 * random.nextFloat();
				level.addParticle(ParticleTypes.PORTAL, px, py, pz, 0, 0, 0);
				if (!state.isAir()) level.addParticle(blockParticle, px, py, pz, 0, 0, 0);
			}
		});
	}
}
