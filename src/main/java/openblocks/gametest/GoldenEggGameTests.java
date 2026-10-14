package openblocks.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.GoldenEggBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.MiniMe;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBEntities;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class GoldenEggGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	private static GoldenEggBlockEntity placeEgg(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.GOLDEN_EGG.get());
		return (GoldenEggBlockEntity)helper.getBlockEntity(POS);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void eggHatchesMiniMeOfOwner(GameTestHelper helper) {
		final GoldenEggBlockEntity egg = placeEgg(helper);
		final GameProfile profile = new GameProfile(UUID.randomUUID(), "egg_owner");
		egg.setOwner(new ResolvableProfile(profile));
		egg.changeStage(GoldenEggBlockEntity.Stage.FALLING);

		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(OBBlocks.GOLDEN_EGG.get(), POS);
			final MiniMe miniMe = helper.findOneEntity(OBEntities.MINI_ME.get());
			helper.assertTrue(miniMe.getOwner() != null && profile.getId().equals(miniMe.getOwner().gameProfile().getId()), "Mini Me should belong to the egg owner");
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void floatingEggLiftsAndDropsBlocks(GameTestHelper helper) {
		final GoldenEggBlockEntity egg = placeEgg(helper);
		final BlockPos stone = POS.east();
		helper.setBlock(stone.below(), Blocks.DIRT);
		helper.setBlock(stone, Blocks.STONE);
		helper.assertTrue(egg.pickUpBlock(helper.getLevel(), helper.absolutePos(stone)), "Expected the egg to lift the stone");
		helper.assertBlockNotPresent(Blocks.STONE, stone);
		final FallingBlockEntity block = helper.findOneEntity(EntityType.FALLING_BLOCK);

		helper.runAfterDelay(10, () -> {
			helper.assertTrue(block.getY() > helper.absoluteVec(Vec3.atLowerCornerOf(stone)).y, "Lifted block should rise");
			egg.dropBlocks();
			helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.STONE, stone));
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void eggDoesNotLiftBlockEntities(GameTestHelper helper) {
		final GoldenEggBlockEntity egg = placeEgg(helper);
		helper.setBlock(POS.east(), Blocks.CHEST);
		helper.assertFalse(egg.pickUpBlock(helper.getLevel(), helper.absolutePos(POS.east())), "Chests should stay");
		helper.assertBlockPresent(Blocks.CHEST, POS.east());
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void miniMePicksUpOwner(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final Vec3 playerPos = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		player.teleportTo(playerPos.x, playerPos.y, playerPos.z);

		final MiniMe miniMe = helper.spawn(OBEntities.MINI_ME.get(), new Vec3(1.5, 2, 2.2));
		miniMe.setOwner(new ResolvableProfile(player.getGameProfile()));

		helper.succeedWhen(() -> {
			helper.assertTrue(player.getVehicle() == miniMe, "Mini Me should pick up its owner");
			player.stopRiding();
			player.getServer().getPlayerList().remove(player);
		});
	}
}
