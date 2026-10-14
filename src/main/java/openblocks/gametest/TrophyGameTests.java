package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.TrophyBlockEntity;
import openblocks.common.TrophyDropHandler;
import openblocks.common.TrophyType;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.TrophyItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class TrophyGameTests {

	private static final BlockPos GROUND = new BlockPos(1, 1, 1);
	private static final BlockPos POS = GROUND.above();

	private static TrophyBlockEntity placeTrophy(GameTestHelper helper, Player player, ItemStack stack) {
		helper.setBlock(GROUND, Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		helper.placeAt(player, stack, GROUND, Direction.UP);
		return helper.getBlockEntity(POS);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void placedTrophyKeepsItsMob(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final TrophyBlockEntity trophy = placeTrophy(helper, player, TrophyItem.create(OBBlocks.TROPHY.get(), TrophyType.CREEPER));
		helper.assertTrue(trophy.getTrophyType() == TrophyType.CREEPER, "Expected a creeper trophy, got " + trophy.getTrophyType());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void cowTrophyDropsLeatherWithCooldown(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final TrophyBlockEntity trophy = placeTrophy(helper, player, TrophyItem.create(OBBlocks.TROPHY.get(), TrophyType.COW));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		// the leather drops at the player
		final Vec3 standing = helper.absoluteVec(Vec3.atBottomCenterOf(POS.east()));
		player.setPos(standing.x, standing.y, standing.z);

		helper.useBlock(POS, player);
		helper.useBlock(POS, player);
		helper.assertTrue(trophy.getCooldown() > 19000, "Expected the cooldown to start, got " + trophy.getCooldown());
		helper.assertItemEntityCountIs(Items.LEATHER, POS, 3, 1);

		// breaking keeps the cooldown, so the trophy can't be reset by replacing it
		helper.getLevel().destroyBlock(helper.absolutePos(POS), true, player);
		helper.runAfterDelay(1, () -> {
			final ItemStack drop = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())
					.stream().map(ItemEntity::getItem).filter(s -> s.is(OBBlocks.TROPHY.get().asItem())).findFirst().orElse(ItemStack.EMPTY);
			helper.assertTrue(drop.get(OBComponents.TROPHY) == TrophyType.COW, "Expected a cow trophy drop, got " + drop);
			helper.assertTrue(drop.getOrDefault(OBComponents.TROPHY_COOLDOWN, 0) > 19000, "Expected the cooldown in the dropped item");
			helper.succeed();
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void endermanTrophyHasCooldown(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final TrophyBlockEntity trophy = placeTrophy(helper, player, TrophyItem.create(OBBlocks.TROPHY.get(), TrophyType.ENDERMAN));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.useBlock(POS, player);
		helper.assertTrue(trophy.getCooldown() >= 3 * 60 * 20, "Expected a cooldown of several minutes, got " + trophy.getCooldown());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void trophyDropsWithoutPickaxe(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		placeTrophy(helper, player, TrophyItem.create(OBBlocks.TROPHY.get(), TrophyType.PIG));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.getLevel().destroyBlock(helper.absolutePos(POS), true, player);
		helper.assertItemEntityCountIs(OBBlocks.TROPHY.get().asItem(), POS, 1, 1);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void witchTrophyBlindsPlayer(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		placeTrophy(helper, player, TrophyItem.create(OBBlocks.TROPHY.get(), TrophyType.WITCH));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.useBlock(POS, player);
		helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "Expected blindness");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void creeperTrophyDoesNotBreakBlocks(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		placeTrophy(helper, player, TrophyItem.create(OBBlocks.TROPHY.get(), TrophyType.CREEPER));
		helper.setBlock(POS.east(), Blocks.DIRT);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.useBlock(POS, player);
		helper.assertBlockPresent(Blocks.DIRT, POS.east());
		helper.assertBlockPresent(OBBlocks.TROPHY.get(), POS);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void dropChanceGrowsWithLooting(GameTestHelper helper) {
		final RandomSource random = RandomSource.create(42);
		int plain = 0, looting = 0;
		for (int i = 0; i < 10000; i++) {
			if (TrophyDropHandler.shouldDrop(random, 0, 0.1)) plain++;
			if (TrophyDropHandler.shouldDrop(random, 3, 0.1)) looting++;
		}
		// expected about 1.25% (bias / 4 * chance) and 31%
		helper.assertTrue(plain > 50 && plain < 250, "Unexpected drops without looting: " + plain);
		helper.assertTrue(looting > 2800 && looting < 3500, "Unexpected drops with looting III: " + looting);
		helper.succeed();
	}
}
