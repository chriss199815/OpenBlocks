package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntitySky}: holds no data, it only exists so the sky can be drawn by a block entity renderer.
 */
public class SkyBlockEntity extends BlockEntity {

	public SkyBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.SKY.get(), pos, state);
	}
}
