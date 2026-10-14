package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import openblocks.block.TrophyBlock;
import openblocks.block.entity.TrophyBlockEntity;
import openblocks.common.TrophyType;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code TileEntityTrophyRenderer}: draws the mob on the pedestal (the pedestal itself is the block model).
 * The item renderer draws both. The mobs are dummy entities of the client level, created once per type.
 */
public class TrophyRenderer implements BlockEntityRenderer<TrophyBlockEntity> {

	// the legacy offset; the pedestal is a bit higher, so feet sink in slightly
	private static final double BASE_HEIGHT = 0.2;

	private static final Map<TrophyType, Entity> ENTITIES = new EnumMap<>(TrophyType.class);
	@Nullable
	private static ClientLevel entityLevel;

	public TrophyRenderer(BlockEntityRendererProvider.Context context) {}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private ItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new ItemRenderer();
				return renderer;
			}
		}, OBItems.TROPHY.get());
	}

	@Override
	public void render(TrophyBlockEntity trophy, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		renderMob(trophy.getTrophyType(), trophy.getBlockState().getValue(TrophyBlock.FACING), partialTick, poseStack, buffers, light);
	}

	@Nullable
	private static Entity getEntity(TrophyType type) {
		final ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return null;
		if (level != entityLevel) {
			ENTITIES.clear();
			entityLevel = level;
		}
		return ENTITIES.computeIfAbsent(type, t -> t.getEntityType().create(level));
	}

	static void renderMob(TrophyType type, Direction facing, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		final Entity entity = getEntity(type);
		if (entity == null) return;

		final float yaw = facing.toYRot();
		entity.setYRot(yaw);
		entity.yRotO = yaw;
		if (entity instanceof LivingEntity living) {
			living.yBodyRot = living.yBodyRotO = yaw;
			living.yHeadRot = living.yHeadRotO = yaw;
		}

		poseStack.pushPose();
		poseStack.translate(0.5, BASE_HEIGHT + type.getVerticalOffset(), 0.5);
		final float scale = (float)type.getScale();
		poseStack.scale(scale, scale, scale);
		Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0, 0, 0, yaw, 0, poseStack, buffers, light);
		poseStack.popPose();
	}

	private static class ItemRenderer extends BlockEntityWithoutLevelRenderer {

		ItemRenderer() {
			super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(OBBlocks.TROPHY.get().defaultBlockState(), poseStack, buffers, light, overlay, ModelData.EMPTY, null);
			// the mob faces the viewer, like legacy
			renderMob(stack.getOrDefault(OBComponents.TROPHY, TrophyType.DEFAULT), Direction.SOUTH, 0, poseStack, buffers, light);
		}
	}
}
