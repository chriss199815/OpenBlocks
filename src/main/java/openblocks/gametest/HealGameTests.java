package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.HealBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class HealGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void healBlockHealsSurvivalPlayers(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.HEAL.get());
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(POS.above()));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.setHealth(10);

		helper.succeedWhen(() -> {
			helper.assertTrue(player.hasEffect(MobEffects.REGENERATION) || player.getHealth() > 10, "Expected the player to get regeneration");
			player.getServer().getPlayerList().remove(player);
		});
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void healBlockSkipsCreativePlayers(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.HEAL.get());
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(POS.above()));
		player.teleportTo(pos.x, pos.y, pos.z);

		helper.runAfterDelay(HealBlockEntity.INTERVAL * 2, () -> {
			helper.assertFalse(player.hasEffect(MobEffects.REGENERATION), "Creative players should not be healed");
			player.getServer().getPlayerList().remove(player);
			helper.succeed();
		});
	}
}
