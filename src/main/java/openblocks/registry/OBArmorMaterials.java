package openblocks.registry;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;

/**
 * Armor materials of the imagination glasses and the crane backpack. The textures come from the items'
 * {@code getArmorTexture}.
 */
public class OBArmorMaterials {

	public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, OpenBlocks.MODID);

	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> IMAGINATION_GLASSES = register("imagination_glasses",
			List.of(new ArmorMaterial.Layer(OpenBlocks.location("imagination_glasses"))));

	// the overlay is tinted with the crayon color, see ImaginaryRenderer.registerClientExtensions
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CRAYON_GLASSES = register("crayon_glasses",
			List.of(new ArmorMaterial.Layer(OpenBlocks.location("crayon_glasses")), new ArmorMaterial.Layer(OpenBlocks.location("crayon_glasses"), "_overlay", true)));

	// like legacy, the stats of iron armor
	public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CRANE_BACKPACK = ARMOR_MATERIALS.register("crane_backpack",
			() -> new ArmorMaterial(ArmorMaterials.IRON.value().defense(), ArmorMaterials.IRON.value().enchantmentValue(),
					SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Tags.Items.INGOTS_IRON), List.of(new ArmorMaterial.Layer(OpenBlocks.location("crane_backpack"))), 0, 0));

	// like legacy, the stats of golden armor
	private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String name, List<ArmorMaterial.Layer> layers) {
		return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(ArmorMaterials.GOLD.value().defense(), ArmorMaterials.GOLD.value().enchantmentValue(),
				SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(Tags.Items.INGOTS_GOLD), layers, 0, 0));
	}
}
