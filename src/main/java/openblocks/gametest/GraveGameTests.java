package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.GraveBlock;
import openblocks.block.entity.GraveBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class GraveGameTests {

	private static final BlockPos DEATH_POS = new BlockPos(1, 3, 1);
	private static final BlockPos GRAVE_POS = new BlockPos(1, 1, 1);

	private static List<ItemEntity> itemsAround(GameTestHelper helper) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8));
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void deathSpawnsGraveWithLoot(GameTestHelper helper) {
		helper.setBlock(DEATH_POS.below(), Blocks.STONE);

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(DEATH_POS));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.getInventory().add(new ItemStack(Items.DIAMOND, 5));
		player.getInventory().add(new ItemStack(Items.IRON_PICKAXE));

		player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);

		helper.succeedWhen(() -> {
			helper.assertBlockPresent(OBBlocks.GRAVE.get(), DEATH_POS);
			final GraveBlockEntity grave = helper.getBlockEntity(DEATH_POS);
			helper.assertTrue(grave.getUsername().equals(player.getGameProfile().getName()), "Expected player name on grave, got " + grave.getUsername());
			helper.assertTrue(grave.getLoot().stream().anyMatch(s -> s.is(Items.DIAMOND) && s.getCount() == 5), "Expected diamonds in grave, got " + grave.getLoot());
			helper.assertTrue(grave.getLoot().stream().anyMatch(s -> s.is(Items.IRON_PICKAXE)), "Expected pickaxe in grave, got " + grave.getLoot());
			helper.assertTrue(itemsAround(helper).isEmpty(), "Items must go into the grave, not drop");
			helper.assertFalse(helper.getBlockState(DEATH_POS).getValue(GraveBlock.HAS_BASE), "Grave on stone has no dirt mound");
			player.getServer().getPlayerList().remove(player);
		});
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void deathInAirLandsOnGround(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setNoGravity(true);
		final Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(DEATH_POS));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.getInventory().add(new ItemStack(Items.DIAMOND));

		player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);

		// the test world floor is directly below the structure
		final BlockPos ground = new BlockPos(DEATH_POS.getX(), 0, DEATH_POS.getZ());
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(OBBlocks.GRAVE.get(), ground);
			helper.assertBlockNotPresent(Blocks.DIRT, DEATH_POS.below());
			helper.assertBlockNotPresent(OBBlocks.GRAVE.get(), DEATH_POS);
			player.getServer().getPlayerList().remove(player);
		});
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void shovelReleasesLoot(GameTestHelper helper) {
		helper.setBlock(GRAVE_POS, OBBlocks.GRAVE.get());
		final GraveBlockEntity grave = helper.getBlockEntity(GRAVE_POS);
		grave.setLoot(List.of(new ItemStack(Items.EMERALD, 3)));

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
			final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(GRAVE_POS)), Direction.UP, helper.absolutePos(GRAVE_POS), false);
			helper.getBlockState(GRAVE_POS).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);

			helper.assertTrue(grave.getLoot().isEmpty(), "Grave must be empty after digging");
			helper.assertItemEntityCountIs(Items.EMERALD, GRAVE_POS, 2.0, 3);
			helper.assertTrue(player.getMainHandItem().getDamageValue() == 2, "Shovel must take 2 damage, has " + player.getMainHandItem().getDamageValue());
			helper.assertBlockPresent(OBBlocks.GRAVE.get(), GRAVE_POS);
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void breakingReleasesLootButNotGrave(GameTestHelper helper) {
		helper.setBlock(GRAVE_POS, OBBlocks.GRAVE.get());
		final GraveBlockEntity grave = helper.getBlockEntity(GRAVE_POS);
		grave.setLoot(List.of(new ItemStack(Items.EMERALD, 3)));

		helper.getLevel().destroyBlock(helper.absolutePos(GRAVE_POS), true);

		helper.assertItemEntityCountIs(Items.EMERALD, GRAVE_POS, 2.0, 3);
		helper.assertItemEntityCountIs(OBBlocks.GRAVE.get().asItem(), GRAVE_POS, 2.0, 0);
		helper.succeed();
	}
}
