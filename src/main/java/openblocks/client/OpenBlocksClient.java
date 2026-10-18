package openblocks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.OpenBlocks;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import openblocks.client.canvas.CanvasModel;
import openblocks.client.canvas.CanvasRenderer;
import openblocks.client.canvas.CanvasTextures;
import openblocks.client.gui.AutoAnvilScreen;
import openblocks.client.gui.BigButtonScreen;
import openblocks.client.gui.AutoEnchantmentTableScreen;
import openblocks.client.gui.DevNullScreen;
import openblocks.client.gui.DispenserLayoutScreen;
import openblocks.client.gui.DonationStationScreen;
import openblocks.client.gui.DrawingTableScreen;
import openblocks.client.gui.GuideScreen;
import openblocks.client.gui.PaintMixerScreen;
import openblocks.client.gui.ProjectorScreen;
import openblocks.client.gui.ItemDropperScreen;
import openblocks.client.gui.VacuumHopperScreen;
import openblocks.client.gui.XpBottlerScreen;
import openblocks.client.particle.LiquidSprayParticle;
import openblocks.client.tank.TankBlockEntityRenderer;
import openblocks.client.tank.TankItemRenderer;
import openblocks.client.tank.TankModel;
import openblocks.entity.Cartographer;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBEntities;
import openblocks.menu.BlockPlacerMenu;
import openblocks.menu.SprinklerMenu;
import openblocks.registry.OBMenus;

/**
 * Client-only entry point; wires renderers, color handlers and input listeners.
 */
@Mod(value = OpenBlocks.MODID, dist = Dist.CLIENT)
public class OpenBlocksClient {

	public OpenBlocksClient(IEventBus modBus, ModContainer container) {
		modBus.addListener(OBColorHandlers::registerBlockColors);
		modBus.addListener(OBColorHandlers::registerItemColors);
		modBus.addListener(OBFluidRendering::registerClientExtensions);
		modBus.addListener(TankItemRenderer::registerClientExtensions);
		modBus.addListener(TrophyRenderer::registerClientExtensions);
		modBus.addListener(TankModel::registerAdditional);
		modBus.addListener(TankModel::modifyBakingResult);
		modBus.addListener(VacuumHopperModel::registerAdditional);
		modBus.addListener(VacuumHopperModel::modifyBakingResult);
		modBus.addListener(HangGliderRenderer::modifyBakingResult);
		modBus.addListener(PaintMixerRenderer::registerAdditional);
		modBus.addListener(FanRenderer::registerAdditional);
		modBus.addListener(BearTrapRenderer::registerAdditional);
		modBus.addListener(SprinklerRenderer::registerAdditional);
		modBus.addListener(CanvasModel::modifyBakingResult);
		modBus.addListener((ModelEvent.BakingCompleted event) -> CanvasTextures.clear());
		modBus.addListener(OpenBlocksClient::registerRenderers);
		modBus.addListener(LiquidSprayParticle::register);
		modBus.addListener(OpenBlocksClient::registerScreens);
		modBus.addListener(SleepingBagLayer::registerLayerDefinitions);
		modBus.addListener(SleepingBagLayer::addLayers);
		modBus.addListener(LuggageRenderer::registerLayerDefinitions);
		modBus.addListener(LuggageRenderer::registerItemProperties);
		modBus.addListener(OBItemProperties::register);
		modBus.addListener(DevNullItemRenderer::registerAdditional);
		modBus.addListener(DevNullItemRenderer::registerClientExtensions);
		modBus.addListener(DevNullItemRenderer::registerItemDecorations);
		modBus.addListener(SkyBlockRenderer::registerShaders);
		modBus.addListener(BrickDropInputHandler::registerKeyMappings);
		modBus.addListener(CannonRenderer::registerLayerDefinitions);
		modBus.addListener(CannonRenderer::registerClientExtensions);
		modBus.addListener(ProjectorRenderer::registerAdditional);
		modBus.addListener(CartographerRenderer::registerLayerDefinitions);
		modBus.addListener(ImaginaryRenderer::registerClientExtensions);
		modBus.addListener(CraneRenderer::registerLayerDefinitions);
		modBus.addListener(CraneRenderer::onAddLayers);
		modBus.addListener(CraneRenderer::registerClientExtensions);
		modBus.addListener(MagnetRenderer::registerLayerDefinitions);
		modBus.addListener(GlyphRenderer::registerAdditional);
		modBus.addListener(GlyphRenderer::registerClientExtensions);
		modBus.addListener(SonicGlassesRenderer::registerLayerDefinitions);
		modBus.addListener(SonicGlassesRenderer::onAddLayers);
		modBus.addListener(SonicGlassesRenderer::registerClientExtensions);
		modBus.addListener(SonicGlassesRenderer::onClientSetup);

		NeoForge.EVENT_BUS.addListener(ElevatorInputHandler::onMovementInput);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderPlayerPre);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderPlayerPost);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onRenderPlayerPre);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onRenderPlayerPost);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onComputeCameraAngles);
		NeoForge.EVENT_BUS.addListener(CursorHighlightRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(SkyBlockRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(BrickDropInputHandler::onKey);
		NeoForge.EVENT_BUS.addListener(HeightMapCache::onItemTooltip);
		NeoForge.EVENT_BUS.addListener(HeightMapCache::onLoggingOut);
		NeoForge.EVENT_BUS.addListener(GuideRenderer::onRenderHighlight);
		NeoForge.EVENT_BUS.addListener(CraneRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(GlyphRenderer::onRenderHighlight);
		NeoForge.EVENT_BUS.addListener(SonicGlassesRenderer::onClientTick);
		NeoForge.EVENT_BUS.addListener(SonicGlassesRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(SillyLoreTooltip::onItemTooltip);

		// not earlier: loading the entity class needs its registered data serializer
		modBus.addListener((FMLClientSetupEvent event) -> Cartographer.clientHighlight = CartographerRenderer::isHighlighted);

		container.registerConfig(ModConfig.Type.CLIENT, OBClientConfig.SPEC);
	}

	private static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(OBMenus.XP_BOTTLER.get(), XpBottlerScreen::new);
		event.register(OBMenus.VACUUM_HOPPER.get(), VacuumHopperScreen::new);
		event.register(OBMenus.ITEM_DROPPER.get(), ItemDropperScreen::new);
		event.register(OBMenus.BLOCK_PLACER.get(), DispenserLayoutScreen<BlockPlacerMenu>::new);
		event.register(OBMenus.SPRINKLER.get(), DispenserLayoutScreen<SprinklerMenu>::new);
		event.register(OBMenus.BIG_BUTTON.get(), BigButtonScreen::new);
		event.register(OBMenus.AUTO_ANVIL.get(), AutoAnvilScreen::new);
		event.register(OBMenus.AUTO_ENCHANTMENT_TABLE.get(), AutoEnchantmentTableScreen::new);
		event.register(OBMenus.DONATION_STATION.get(), DonationStationScreen::new);
		event.register(OBMenus.DRAWING_TABLE.get(), DrawingTableScreen::new);
		event.register(OBMenus.PAINT_MIXER.get(), PaintMixerScreen::new);
		event.register(OBMenus.DEV_NULL.get(), DevNullScreen::new);
		event.register(OBMenus.PROJECTOR.get(), ProjectorScreen::new);
		event.register(OBMenus.GUIDE.get(), GuideScreen::new);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(OBBlockEntities.TANK.get(), TankBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.TROPHY.get(), TrophyRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.GRAVE.get(), GraveBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.AUTO_ENCHANTMENT_TABLE.get(), AutoEnchantmentTableRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.PAINT_MIXER.get(), PaintMixerRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.CANVAS.get(), CanvasRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.FAN.get(), FanRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.BEAR_TRAP.get(), BearTrapRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.SPRINKLER.get(), SprinklerRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.SKY.get(), SkyBlockRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.GOLDEN_EGG.get(), GoldenEggRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.CANNON.get(), CannonRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.PROJECTOR.get(), ProjectorRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.GUIDE.get(), GuideRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.BUILDER_GUIDE.get(), GuideRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.IMAGINARY.get(), ImaginaryRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), VillageHighlighterRenderer::new);
		event.registerEntityRenderer(OBEntities.NO_FLY_XP_ORB.get(), ExperienceOrbRenderer::new);
		event.registerEntityRenderer(OBEntities.LUGGAGE.get(), LuggageRenderer::new);
		event.registerEntityRenderer(OBEntities.MINI_ME.get(), MiniMeRenderer::new);
		event.registerEntityRenderer(OBEntities.CARTOGRAPHER.get(), CartographerRenderer::new);
		event.registerEntityRenderer(OBEntities.MAGNET.get(), MagnetRenderer::new);
		event.registerEntityRenderer(OBEntities.MOUNTED_BLOCK.get(), MountedBlockRenderer::new);
		event.registerEntityRenderer(OBEntities.GLYPH.get(), GlyphRenderer::new);
		// like legacy, drawn like a snowball
		event.registerEntityRenderer(OBEntities.GOLDEN_EYE.get(), ThrownItemRenderer::new);
	}
}
