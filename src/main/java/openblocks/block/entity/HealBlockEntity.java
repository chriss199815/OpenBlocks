package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityHealBlock}: once a second, players in the block or standing on it get one tick of
 * regeneration and saturation (1 health and 1 food point), creative players are skipped.
 */
public class HealBlockEntity extends BlockEntity {

	public static final int INTERVAL = 20;

	public HealBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.HEAL.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, HealBlockEntity heal) {
		if (level.getGameTime() % INTERVAL != 0) return;

		// one tick is just enough to heal once, without the effect lingering (so no particles, like legacy)
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
			if (player.getAbilities().instabuild) continue;
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1, 10));
			player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1));
		}
	}
}
