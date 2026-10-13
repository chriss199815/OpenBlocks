package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.TankBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;
import openblocks.util.XpUtils;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class XpDrainGameTests {

	private static final BlockPos TANK = new BlockPos(1, 1, 1);
	private static final BlockPos DRAIN = TANK.above();

	private static TankBlockEntity setUp(GameTestHelper helper) {
		helper.setBlock(TANK, OBBlocks.TANK.get());
		helper.setBlock(DRAIN, OBBlocks.XP_DRAIN.get());
		return helper.getBlockEntity(TANK);
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void drainsPlayerIntoTank(GameTestHelper helper) {
		final TankBlockEntity tank = setUp(helper);

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(DRAIN).add(0, 1 / 16.0, 0));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.giveExperiencePoints(30);

		helper.succeedWhen(() -> {
			helper.assertTrue(XpUtils.getPlayerXp(player) == 0, "Expected player drained, has " + XpUtils.getPlayerXp(player));
			helper.assertTrue(tank.getFluid().getAmount() == FluidXpUtils.xpToFluid(30), "Expected 30 XP in tank, got " + tank.getFluid().getAmount());
			// only reached once all assertions pass
			player.getServer().getPlayerList().remove(player);
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void consumesOrbs(GameTestHelper helper) {
		final TankBlockEntity tank = setUp(helper);

		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(DRAIN).add(0, 0.1, 0));
		final ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), pos.x, pos.y, pos.z, 12);
		orb.setNoGravity(true);
		orb.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(orb);

		helper.succeedWhen(() -> {
			helper.assertTrue(orb.isRemoved(), "Expected orb to be consumed");
			helper.assertTrue(tank.getFluid().getAmount() == FluidXpUtils.xpToFluid(12), "Expected 12 XP in tank, got " + tank.getFluid().getAmount());
		});
	}
}
