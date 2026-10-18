package openblocks.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import openblocks.OpenBlocks;
import openblocks.entity.Magnet;

/**
 * Port of legacy {@code ItemCraneBackpack}: a chest piece with a crane arm; while worn, a magnet hangs from the arm
 * (see {@code CraneRenderer} for model and line). Legacy {@code doCraneCollisionCheck} (off by default) is gone.
 */
public class CraneBackpackItem extends ArmorItem {

	private static final ResourceLocation TEXTURE = OpenBlocks.location("textures/models/crane.png");

	public CraneBackpackItem(Holder<ArmorMaterial> material, Properties properties) {
		// like legacy, the durability of an iron chestplate
		super(material, ArmorItem.Type.CHESTPLATE, properties.durability(ArmorItem.Type.CHESTPLATE.getDurability(15)));
	}

	public static boolean isWearing(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CraneBackpackItem;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (!level.isClientSide && entity instanceof Player player && player.getItemBySlot(EquipmentSlot.CHEST) == stack) Magnet.ensureExists(player);
	}

	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return TEXTURE;
	}
}
