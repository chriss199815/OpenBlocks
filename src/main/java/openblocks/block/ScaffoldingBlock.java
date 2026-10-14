package openblocks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.OBConfig;

/**
 * Port of legacy {@code BlockScaffolding}: a see-through block that breaks by itself on random ticks (and drops itself),
 * so temporary pillars disappear. Like glass, faces between two scaffolding blocks are hidden.
 */
public class ScaffoldingBlock extends TransparentBlock {

	public static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(ScaffoldingBlock::new);

	public ScaffoldingBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends TransparentBlock> codec() {
		return CODEC;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		final int rate = OBConfig.SCAFFOLDING.despawnRate.getAsInt();
		if (rate <= 0 || random.nextInt(rate) == 0)
			level.destroyBlock(pos, true);
	}
}
