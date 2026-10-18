package openblocks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import openblocks.OpenBlocks;
import openblocks.item.SonicGlassesItem;
import openblocks.registry.OBItems;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Client side of the sonic glasses (legacy {@code SoundEventsManager}): armor model, and in first person view the
 * world darkened behind the glasses, with an icon (see {@link SoundIcons}) where a sound plays. Icons are seen through
 * walls, sized by the volume, and fade out after the sound ends (the higher the pitch, the slower).
 * <p>
 * Unlike legacy, sounds are taken from a sound listener (like subtitles), so only sounds that can be heard get an
 * icon, and sounds without position (music, menu clicks) none. Legacy icons also did not actually fade, they vanished.
 */
public class SonicGlassesRenderer {

	private static final ResourceLocation OBSIDIAN = OpenBlocks.location("textures/misc/glasses_obsidian.png");

	private static SonicGlassesModel model;

	private static final List<HeardSound> sounds = new ArrayList<>();

	private static class HeardSound {
		private final SoundInstance sound;
		private final SoundIcons.Icon icon;
		private final float size;
		private final int ttl;

		private boolean playing = true;
		private int ticks;

		private HeardSound(SoundInstance sound, SoundIcons.Icon icon, float size, float ttl) {
			this.sound = sound;
			this.icon = icon;
			this.size = size;
			this.ttl = Mth.floor(20 * ttl);
		}

		private void update(SoundManager manager) {
			if (playing) playing = manager.isActive(sound);
			else ticks++;
		}

		private boolean isAlive() {
			return ticks <= ttl;
		}

		private float getAlpha(float partialTick) {
			return playing? 1 : Mth.clamp(1 - (ticks + partialTick) / ttl, 0, 1);
		}
	}

	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(SonicGlassesModel.LAYER, SonicGlassesModel::createLayer);
	}

	public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
		model = new SonicGlassesModel(event.getEntityModels().bakeLayer(SonicGlassesModel.LAYER));
	}

	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			@Override
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
				return model;
			}
		}, OBItems.SONIC_GLASSES.get());
	}

	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> Minecraft.getInstance().getSoundManager().addListener(SonicGlassesRenderer::onPlaySound));
	}

	private static void onPlaySound(SoundInstance sound, WeighedSoundEvents accessor, float range) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (!SonicGlassesItem.isWearing(minecraft.getCameraEntity()) || sound.isRelative()) return;

		final Vec3 pos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
		if (minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(pos) > range) return;

		sounds.add(new HeardSound(sound, SoundIcons.get(sound.getLocation()), (float)Math.log(sound.getVolume() + 1), sound.getPitch()));
	}

	public static void onClientTick(ClientTickEvent.Pre event) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (!SonicGlassesItem.isWearing(minecraft.getCameraEntity())) {
			sounds.clear();
			return;
		}

		final SoundManager manager = minecraft.getSoundManager();
		sounds.removeIf(sound -> {
			sound.update(manager);
			return !sound.isAlive();
		});
	}

	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

		final Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.options.getCameraType().isFirstPerson() || !SonicGlassesItem.isWearing(minecraft.getCameraEntity())) return;

		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		dimWorld(minecraft);
		renderIcons(event.getPoseStack(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
	}

	private static void dimWorld(Minecraft minecraft) {
		final float opacity = OBClientConfig.GLASSES.opacity.get().floatValue();
		if (opacity <= 0) return;
		final boolean useTexture = OBClientConfig.GLASSES.useTexture.get();

		final Matrix4f projection = RenderSystem.getProjectionMatrix();
		final VertexSorting sorting = RenderSystem.getVertexSorting();
		RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
		final Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.identity();
		RenderSystem.applyModelViewMatrix();

		final BufferBuilder builder;
		if (useTexture) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, OBSIDIAN);
			RenderSystem.setShaderColor(1, 1, 1, opacity);
			// like legacy, the texture is repeated with a texel per four pixels
			final float maxU = minecraft.getWindow().getWidth() / 1024f;
			final float maxV = minecraft.getWindow().getHeight() / 1024f;
			builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.addVertex(-1, -1, 0).setUv(0, 0);
			builder.addVertex(1, -1, 0).setUv(maxU, 0);
			builder.addVertex(1, 1, 0).setUv(maxU, maxV);
			builder.addVertex(-1, 1, 0).setUv(0, maxV);
		} else {
			RenderSystem.setShader(GameRenderer::getPositionShader);
			// legacy cleared the screen to black when fully obscured
			if (opacity >= 1) RenderSystem.setShaderColor(0, 0, 0, 1);
			else RenderSystem.setShaderColor(0.085f, 0.074f, 0.129f, opacity);
			builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			builder.addVertex(-1, -1, 0);
			builder.addVertex(1, -1, 0);
			builder.addVertex(1, 1, 0);
			builder.addVertex(-1, 1, 0);
		}
		BufferUploader.drawWithShader(builder.buildOrThrow());
		RenderSystem.setShaderColor(1, 1, 1, 1);

		modelView.popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(projection, sorting);
	}

	private static void renderIcons(PoseStack poseStack, Camera camera, float partialTick) {
		if (sounds.isEmpty()) return;

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		final Vec3 cameraPos = camera.getPosition();
		for (HeardSound sound : sounds) {
			poseStack.pushPose();
			poseStack.translate(sound.sound.getX() - cameraPos.x, sound.sound.getY() - cameraPos.y, sound.sound.getZ() - cameraPos.z);
			poseStack.mulPose(camera.rotation());
			sound.icon.draw(poseStack.last(), builder, sound.size, sound.getAlpha(partialTick));
			poseStack.popPose();
		}

		final MeshData mesh = builder.build();
		if (mesh != null) BufferUploader.drawWithShader(mesh);
	}
}
