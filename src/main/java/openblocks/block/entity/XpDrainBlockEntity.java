package openblocks.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import openblocks.common.FluidXpUtils;
import openblocks.registry.OBBlockEntities;
import openblocks.util.XpUtils;

/**
 * Port of legacy {@code TileEntityXPDrain}: pulls XP from players and orbs on the grid into the fluid handler below.
 */
public class XpDrainBlockEntity extends BlockEntity {

	private static final int MAX_XP_PER_TICK = 4;

	public XpDrainBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.XP_DRAIN.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, XpDrainBlockEntity drain) {
		final List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.3, pos.getZ() + 1));
		final List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos));
		if (orbs.isEmpty() && players.isEmpty()) return;

		final BlockPos below = pos.below();
		if (!level.isLoaded(below)) return;
		final IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, below, Direction.UP);
		if (tank == null) return;

		for (ExperienceOrb orb : orbs)
			tryConsumeOrb(tank, orb);

		for (Player player : players)
			tryDrainPlayer(level, pos, tank, player);
	}

	private static void tryConsumeOrb(IFluidHandler tank, ExperienceOrb orb) {
		if (!orb.isAlive()) return;
		final FluidStack xp = FluidXpUtils.xpJuice(orb.getValue());
		if (tank.fill(xp, IFluidHandler.FluidAction.SIMULATE) == xp.getAmount()) {
			tank.fill(xp, IFluidHandler.FluidAction.EXECUTE);
			orb.discard();
		}
	}

	private static void tryDrainPlayer(Level level, BlockPos pos, IFluidHandler tank, Player player) {
		final int playerXp = XpUtils.getPlayerXp(player);
		if (playerXp <= 0) return;

		final int maxAccepted = tank.fill(FluidXpUtils.xpJuice(Math.min(MAX_XP_PER_TICK, playerXp)), IFluidHandler.FluidAction.SIMULATE);
		// rounding down, so we only use as much as we can
		final int acceptedXp = FluidXpUtils.fluidToXp(maxAccepted);
		if (acceptedXp <= 0) return;

		final int filled = tank.fill(FluidXpUtils.xpJuice(acceptedXp), IFluidHandler.FluidAction.EXECUTE);
		if (filled <= 0) return;

		if (level.getGameTime() % 4 == 0)
			level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.1f, 0.5f * ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.8f));

		player.giveExperiencePoints(-acceptedXp);
	}
}
