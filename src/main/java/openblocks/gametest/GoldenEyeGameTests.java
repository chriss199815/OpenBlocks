package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.entity.GoldenEye;
import openblocks.item.GoldenEyeItem;
import openblocks.item.GoldenEyeTarget;
import openblocks.recipe.GoldenEyeRechargeRecipe;
import openblocks.registry.OBComponents;
import openblocks.registry.OBEntities;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class GoldenEyeGameTests {

	private static final GoldenEyeRechargeRecipe RECHARGE = new GoldenEyeRechargeRecipe(CraftingBookCategory.MISC);

	private static ItemStack eye(int damage) {
		final ItemStack stack = new ItemStack(OBItems.GOLDEN_EYE.get());
		stack.setDamageValue(damage);
		return stack;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void enderPearlsRechargeGoldenEye(GameTestHelper helper) {
		final CraftingInput input = CraftingInput.of(3, 1, List.of(eye(GoldenEyeItem.MAX_DAMAGE), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.ENDER_PEARL)));
		helper.assertTrue(RECHARGE.matches(input, helper.getLevel()), "Eye and pearls should match");
		final ItemStack result = RECHARGE.assemble(input, helper.getLevel().registryAccess());
		helper.assertValueEqual(result.getDamageValue(), GoldenEyeItem.MAX_DAMAGE - 2 * GoldenEyeRechargeRecipe.PEARL_RECHARGE, "damage after recharge");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void rechargeDoesNotOvercharge(GameTestHelper helper) {
		final CraftingInput input = CraftingInput.of(2, 1, List.of(eye(5), new ItemStack(Items.ENDER_PEARL)));
		helper.assertFalse(RECHARGE.matches(input, helper.getLevel()), "A pearl should not overcharge the eye");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	private static ServerPlayer throwEye(GameTestHelper helper, BlockPos structureOffset) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final Vec3 playerPos = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		player.teleportTo(playerPos.x, playerPos.y, playerPos.z);

		final ItemStack stack = eye(0);
		final BlockPos structure = BlockPos.containing(playerPos).offset(structureOffset);
		stack.set(OBComponents.GOLDEN_EYE_TARGET.get(), new GoldenEyeTarget(BuiltinStructures.STRONGHOLD, GlobalPos.of(helper.getLevel().dimension(), structure)));
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "The thrown eye should leave the hand");
		return player;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void thrownEyeFliesTowardsFarStructure(GameTestHelper helper) {
		final ServerPlayer player = throwEye(helper, new BlockPos(500, 0, 0));
		final GoldenEye eye = helper.findOneEntity(OBEntities.GOLDEN_EYE.get());
		helper.assertTrue(eye.getTarget().x > player.getX() + 5, "The eye should fly towards the structure (east)");
		eye.discard();
		player.getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	// a near structure makes the eye fly down, so it stays in the test area
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = 200)
	public static void thrownEyeDropsBackWithOneChargeLess(GameTestHelper helper) {
		final ServerPlayer player = throwEye(helper, new BlockPos(5, 0, 0));
		final GoldenEye eye = helper.findOneEntity(OBEntities.GOLDEN_EYE.get());
		helper.assertTrue(eye.getTarget().y < player.getEyeY() - 3, "The eye should fly down next to a structure");

		// otherwise the player would pick the eye up right away
		player.getServer().getPlayerList().remove(player);

		// the empty test structure has no floor, the eye lands on the world floor just below it
		final AABB area = helper.getBounds().expandTowards(0, -2, 0);
		helper.succeedWhen(() -> {
			final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(OBItems.GOLDEN_EYE.get()));
			helper.assertFalse(dropped.isEmpty(), "Expected the golden eye to drop");
			helper.assertValueEqual(dropped.getFirst().getItem().getDamageValue(), 1, "damage after one throw");
			dropped.forEach(ItemEntity::discard);
		});
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void emptyEyeCannotBeThrown(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		final ItemStack stack = eye(GoldenEyeItem.MAX_DAMAGE);
		stack.set(OBComponents.GOLDEN_EYE_TARGET.get(), new GoldenEyeTarget(BuiltinStructures.STRONGHOLD, GlobalPos.of(helper.getLevel().dimension(), BlockPos.ZERO)));
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		helper.assertTrue(player.getMainHandItem() == stack, "An empty eye should stay in the hand");
		helper.assertEntityNotPresent(OBEntities.GOLDEN_EYE.get());
		player.getServer().getPlayerList().remove(player);
		helper.succeed();
	}
}
