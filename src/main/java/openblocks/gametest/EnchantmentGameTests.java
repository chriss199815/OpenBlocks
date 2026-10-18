package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.api.FlimFlamRegistry;
import openblocks.common.flimflam.FlimFlamHandler;
import openblocks.common.flimflam.LuckState;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBComponents;
import openblocks.registry.OBEnchantments;
import openblocks.registry.OBItems;
import openblocks.util.XpUtils;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class EnchantmentGameTests {

	private static ItemStack enchanted(GameTestHelper helper, Item item, ResourceKey<Enchantment> key, int level) {
		final ItemStack stack = new ItemStack(item);
		stack.enchant(OBEnchantments.holder(helper.getLevel(), key), level);
		return stack;
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void enchantmentsAreOnTheEnchantingTable(GameTestHelper helper) {
		final var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		for (ResourceKey<Enchantment> key : List.of(OBEnchantments.EXPLOSIVE, OBEnchantments.LAST_STAND, OBEnchantments.FLIM_FLAM))
			helper.assertTrue(registry.getHolderOrThrow(key).is(EnchantmentTags.IN_ENCHANTING_TABLE), key.location() + " should be offered by the enchanting table");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void lastStandPaysWithExperience(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.CHEST, enchanted(helper, Items.LEATHER_CHESTPLATE, OBEnchantments.LAST_STAND, 2));
		player.giveExperiencePoints(1000);
		player.setHealth(2);

		player.hurt(helper.getLevel().damageSources().generic(), 10);
		helper.assertTrue(player.isAlive() && player.getHealth() == 1, "Last stand should leave the player at half a heart");
		helper.assertTrue(XpUtils.getPlayerXp(player) < 1000, "Last stand should cost experience");

		final Player poor = helper.makeMockPlayer(GameType.SURVIVAL);
		poor.setItemSlot(EquipmentSlot.CHEST, enchanted(helper, Items.LEATHER_CHESTPLATE, OBEnchantments.LAST_STAND, 2));
		poor.setHealth(2);
		poor.hurt(helper.getLevel().damageSources().generic(), 10);
		helper.assertFalse(poor.isAlive(), "Without experience last stand should not save the player");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void lastStandSavesFromDamageCommand(GameTestHelper helper) throws ReflectiveOperationException {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		// a new player is invulnerable for a few seconds
		final var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
		field.setAccessible(true);
		field.setInt(player, 0);
		player.setItemSlot(EquipmentSlot.FEET, enchanted(helper, Items.NETHERITE_BOOTS, OBEnchantments.LAST_STAND, 1));
		player.giveExperienceLevels(1000);
		final int before = XpUtils.getPlayerXp(player);
		final var server = helper.getLevel().getServer();
		server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "damage @s 100");
		helper.assertTrue(player.isAlive() && player.getHealth() == 1, "Last stand boots should save the player from /damage");
		helper.assertTrue(XpUtils.getPlayerXp(player) < before, "Last stand should cost experience");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void explosiveBootsTurnFallIntoJump(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.moveTo(helper.absoluteVec(new BlockPos(1, 2, 1).getCenter()));
		player.setItemSlot(EquipmentSlot.FEET, enchanted(helper, Items.IRON_BOOTS, OBEnchantments.EXPLOSIVE, 1));
		player.getInventory().setItem(0, new ItemStack(Items.GUNPOWDER, 3));
		final float health = player.getHealth();

		player.causeFallDamage(10, 1, helper.getLevel().damageSources().fall());
		helper.assertTrue(player.getHealth() == health, "The fall should not hurt");
		helper.assertTrue(player.getInventory().getItem(0).getCount() == 2, "The explosion should use one gunpowder");
		helper.assertTrue(player.getDeltaMovement().y > 0.42, "The player should be thrown up higher than a jump");

		final Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
		sneaking.setItemSlot(EquipmentSlot.FEET, enchanted(helper, Items.IRON_BOOTS, OBEnchantments.EXPLOSIVE, 1));
		sneaking.getInventory().setItem(0, new ItemStack(Items.GUNPOWDER, 3));
		sneaking.setShiftKeyDown(true);
		sneaking.causeFallDamage(10, 1, helper.getLevel().damageSources().fall());
		helper.assertTrue(sneaking.getInventory().getItem(0).getCount() == 3, "Sneaking should not trigger the explosion");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void flimFlamWeaponLowersLuck(GameTestHelper helper) {
		final Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
		attacker.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, Items.IRON_SWORD, OBEnchantments.FLIM_FLAM, 2));
		final Player victim = helper.makeMockPlayer(GameType.SURVIVAL);

		victim.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1);
		helper.assertTrue(LuckState.of(victim).getLuck() <= -2, "Two levels of flim flam should cost the victim two d20 rolls of luck");
		helper.assertTrue(LuckState.of(attacker).getLuck() == 0, "The attacker should keep their luck");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void luckCommand(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		final var server = helper.getLevel().getServer();
		// the mock player is not in the player list, so it runs the command on itself
		server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "luck @s -50");
		helper.assertTrue(LuckState.of(player).getLuck() == -50, "The luck command should change the luck");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void encaseFlimFlam(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		final BlockPos feet = helper.absolutePos(new BlockPos(1, 2, 1));
		player.moveTo(feet.getCenter().subtract(0, 0.5, 0));
		helper.assertTrue(FlimFlamHandler.execute(player, FlimFlamRegistry.get("encase")), "Encase should work in the open");

		helper.assertBlockPresent(Blocks.DIRT, new BlockPos(0, 2, 1));
		helper.assertBlockPresent(Blocks.DIRT, new BlockPos(1, 4, 1));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 2, 1));
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void loreFlimFlamAndEpicEraser(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(FlimFlamHandler.execute(player, FlimFlamRegistry.get("epic-lore")), "Lore should be added to an item");
		final ItemStack pickaxe = player.getMainHandItem();
		final List<String> lore = pickaxe.get(OBComponents.SILLY_LORE);
		helper.assertTrue(lore != null && !lore.isEmpty() && lore.stream().allMatch(line -> !line.isBlank()), "The item should have silly lore");

		final CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(OBItems.EPIC_ERASER.get()), pickaxe));
		final var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
		final ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(result.is(Items.IRON_PICKAXE) && !result.has(OBComponents.SILLY_LORE), "The eraser should remove the lore");
		final ItemStack eraser = recipe.getRemainingItems(input).get(0);
		helper.assertTrue(eraser.is(OBItems.EPIC_ERASER.get()) && eraser.getDamageValue() == 1, "The eraser should stay with one use less");
		helper.succeed();
	}
}
