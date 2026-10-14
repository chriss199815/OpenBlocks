package openblocks.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import openblocks.OpenBlocks;
import openblocks.entity.Luggage;
import openblocks.item.LuggageItem;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code EntityLuggageRenderer}; special luggage gets its own texture and the creeper charge swirl.
 */
public class LuggageRenderer extends MobRenderer<Luggage, LuggageModel> {

	/**
	 * Item model predicate that switches to the special luggage model, replacing the legacy {@code inventory} size predicate.
	 */
	public static final ResourceLocation SPECIAL_PROPERTY = OpenBlocks.location("special");

	private static final ResourceLocation TEXTURE_NORMAL = OpenBlocks.location("textures/models/luggage.png");
	private static final ResourceLocation TEXTURE_SPECIAL = OpenBlocks.location("textures/models/luggage_special.png");
	private static final ResourceLocation CHARGE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

	public LuggageRenderer(EntityRendererProvider.Context context) {
		super(context, new LuggageModel(context.bakeLayer(LuggageModel.LAYER)), 0.5f);
		addLayer(new EnergySwirlLayer<>(this) {
			@Override
			protected float xOffset(float tickCount) {
				return tickCount * 0.01f;
			}

			@Override
			protected ResourceLocation getTextureLocation() {
				return CHARGE_TEXTURE;
			}

			@Override
			protected EntityModel<Luggage> model() {
				return getParentModel();
			}
		});
	}

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(LuggageModel.LAYER, LuggageModel::createLayer);
	}

	public static void registerItemProperties(FMLClientSetupEvent event) {
		event.enqueueWork(() -> ItemProperties.register(OBItems.LUGGAGE.get(), SPECIAL_PROPERTY,
				(stack, level, entity, seed) -> LuggageItem.isSpecial(stack)? 1 : 0));
	}

	@Override
	public ResourceLocation getTextureLocation(Luggage luggage) {
		return luggage.isSpecial()? TEXTURE_SPECIAL : TEXTURE_NORMAL;
	}
}
