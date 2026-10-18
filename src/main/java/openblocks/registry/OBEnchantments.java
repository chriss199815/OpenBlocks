package openblocks.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import openblocks.OpenBlocks;

/**
 * Enchantments are data driven since 1.21: their definitions are generated into the data pack from {@link #bootstrap}
 * (see {@code OBDatapackProvider}); what they do is coded in event handlers. Unlike legacy, there are no config options
 * to disable them (that is up to data packs now).
 */
public class OBEnchantments {

	public static final ResourceKey<Enchantment> EXPLOSIVE = key("explosive");
	public static final ResourceKey<Enchantment> LAST_STAND = key("last_stand");
	public static final ResourceKey<Enchantment> FLIM_FLAM = key("flim_flam");

	/**
	 * Items flim flam is offered for at the enchanting table (armor and swords, like legacy); any other enchantable item
	 * takes it from a book.
	 */
	public static final TagKey<Item> FLIM_FLAM_PRIMARY = TagKey.create(Registries.ITEM, OpenBlocks.location("enchantable/flim_flam"));

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, OpenBlocks.location(name));
	}

	public static void bootstrap(BootstrapContext<Enchantment> context) {
		final HolderGetter<Item> items = context.lookup(Registries.ITEM);

		// legacy: level 1 from 15, level 2 from 25, level 3 not from the table; the maximum cost of level 3 is below its
		// minimum, so the table never offers it
		context.register(EXPLOSIVE, Enchantment.enchantment(Enchantment.definition(
				items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE), 2, 3,
				Enchantment.dynamicCost(15, 10), Enchantment.dynamicCost(26, 4), 4, EquipmentSlotGroup.ARMOR)).build(EXPLOSIVE.location()));

		context.register(LAST_STAND, Enchantment.enchantment(Enchantment.definition(
				items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE), 5, 2,
				Enchantment.dynamicCost(15, 10), Enchantment.dynamicCost(25, 10), 2, EquipmentSlotGroup.ARMOR)).build(LAST_STAND.location()));

		context.register(FLIM_FLAM, Enchantment.enchantment(Enchantment.definition(
				items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE), items.getOrThrow(FLIM_FLAM_PRIMARY), 2, 4,
				Enchantment.dynamicCost(41, 10), Enchantment.dynamicCost(51, 10), 4, EquipmentSlotGroup.ARMOR, EquipmentSlotGroup.MAINHAND)).build(FLIM_FLAM.location()));
	}

	public static Holder<Enchantment> holder(Level level, ResourceKey<Enchantment> key) {
		return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
	}

	public static int getLevel(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
		return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key)
				.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
				.orElse(0);
	}

	/**
	 * Sum of the levels on the worn armor.
	 */
	public static int getArmorLevels(LivingEntity entity, ResourceKey<Enchantment> key) {
		int sum = 0;
		for (ItemStack stack : entity.getArmorSlots())
			sum += getLevel(entity.level(), key, stack);
		return sum;
	}
}
