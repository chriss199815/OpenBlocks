package openblocks.item;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code ItemSonicGlasses}: a helmet that shows its wearer where sounds come from (see
 * {@code SonicGlassesRenderer}).
 */
public class SonicGlassesItem extends ArmorItem {

	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/glasses.png");

	public SonicGlassesItem(Holder<ArmorMaterial> material, Properties properties) {
		// like legacy, the durability of an iron helmet
		super(material, ArmorItem.Type.HELMET, properties.durability(ArmorItem.Type.HELMET.getDurability(15)));
	}

	public static boolean isWearing(@Nullable Entity entity) {
		return entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SonicGlassesItem;
	}

	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return TEXTURE;
	}
}
