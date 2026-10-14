package openblocks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Port of legacy {@code BlockLadder} ("Jaded Ladder"): a trapdoor that can be climbed while open. Vanilla trapdoors only
 * do that above a real ladder, so this is still useful at the top of a ladder shaft.
 */
public class JadedLadderBlock extends TrapDoorBlock {

	public static final MapCodec<JadedLadderBlock> CODEC = simpleCodec(JadedLadderBlock::new);

	public JadedLadderBlock(Properties properties) {
		super(BlockSetType.OAK, properties);
	}

	@Override
	public MapCodec<? extends TrapDoorBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
		return state.getValue(OPEN);
	}
}
