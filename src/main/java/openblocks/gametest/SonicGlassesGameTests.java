package openblocks.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.SonicGlassesItem;
import openblocks.registry.OBItems;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class SonicGlassesGameTests {

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sonicGlassesAreWornOnTheHead(GameTestHelper helper) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final ItemStack glasses = new ItemStack(OBItems.SONIC_GLASSES.get());
		helper.assertTrue(player.getEquipmentSlotForItem(glasses) == EquipmentSlot.HEAD, "Sonic glasses should go on the head");
		helper.assertFalse(SonicGlassesItem.isWearing(player), "A player without glasses should not wear them");
		player.setItemSlot(EquipmentSlot.HEAD, glasses);
		helper.assertTrue(SonicGlassesItem.isWearing(player), "A player with glasses on the head should wear them");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sonicGlassesRecipe(GameTestHelper helper) {
		final ItemStack iron = new ItemStack(Items.IRON_INGOT);
		final ItemStack obsidian = new ItemStack(Items.OBSIDIAN);
		final CraftingInput input = CraftingInput.of(3, 2, List.of(iron, new ItemStack(Items.IRON_HELMET), iron, obsidian, new ItemStack(Items.STICK), obsidian));
		final ItemStack result = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
				.orElse(ItemStack.EMPTY);
		helper.assertTrue(result.is(OBItems.SONIC_GLASSES.get()), "Iron, an iron helmet, obsidian and a stick should make sonic glasses");
		helper.succeed();
	}
}
