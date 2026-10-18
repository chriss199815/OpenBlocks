package openblocks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import openblocks.OpenBlocks;
import openblocks.block.entity.ImaginaryBlockEntity;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code ItemImaginationGlasses}: a helmet that decides which imaginary blocks its wearer sees, targets
 * and stands on. Crayon glasses carry the color of the crayon they were made from.
 */
public class ImaginationGlassesItem extends ArmorItem {

	public enum Type {
		PENCIL("pencil") {
			@Override
			boolean checkBlock(ImaginaryBlockEntity.Property property, ItemStack stack, ImaginaryBlockEntity block) {
				return block.isPencil() ^ block.isInverted();
			}
		},
		CRAYON("crayon") {
			@Override
			boolean checkBlock(ImaginaryBlockEntity.Property property, ItemStack stack, ImaginaryBlockEntity block) {
				return (!block.isPencil() && getColor(stack) == block.getColor()) ^ block.isInverted();
			}
		},
		TECHNICOLOR("technicolor") {
			@Override
			boolean checkBlock(ImaginaryBlockEntity.Property property, ItemStack stack, ImaginaryBlockEntity block) {
				return property == ImaginaryBlockEntity.Property.VISIBLE || block.isInverted();
			}
		},
		// legacy "BASTARD"
		SERIOUS("admin") {
			@Override
			boolean checkBlock(ImaginaryBlockEntity.Property property, ItemStack stack, ImaginaryBlockEntity block) {
				return true;
			}
		};

		private final ResourceLocation texture;

		Type(String texture) {
			this.texture = OpenBlocks.location("textures/models/glasses_" + texture + ".png");
		}

		abstract boolean checkBlock(ImaginaryBlockEntity.Property property, ItemStack stack, ImaginaryBlockEntity block);
	}

	private static final ResourceLocation CRAYON_OVERLAY = OpenBlocks.location("textures/models/glasses_crayon_overlay.png");

	public final Type type;

	public ImaginationGlassesItem(Type type, Holder<ArmorMaterial> material, Properties properties) {
		super(material, ArmorItem.Type.HELMET, properties.durability(ArmorItem.Type.HELMET.getDurability(7)));
		this.type = type;
	}

	public static ItemStack createCrayonGlasses(int color) {
		final ItemStack stack = new ItemStack(OBItems.CRAYON_GLASSES.get());
		stack.set(OBComponents.CRAYON_COLOR, color);
		return stack;
	}

	/**
	 * Color of crayon glasses, white if missing.
	 */
	public static int getColor(ItemStack stack) {
		return stack.getOrDefault(OBComponents.CRAYON_COLOR, PaintColors.WHITE);
	}

	public boolean checkBlock(ImaginaryBlockEntity.Property property, ItemStack stack, ImaginaryBlockEntity block) {
		return type.checkBlock(property, stack, block);
	}

	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		// the dyeable layer only exists on crayon glasses
		return layer.dyeable()? CRAYON_OVERLAY : type.texture;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (type == Type.CRAYON) tooltip.add(Component.translatable("openblocks.misc.color", String.format("%06X", getColor(stack))).withStyle(ChatFormatting.GRAY));
	}
}
