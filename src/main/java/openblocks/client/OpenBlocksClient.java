package openblocks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.OpenBlocks;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
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
import openblocks.client.gui.PaintMixerScreen;
import openblocks.client.gui.ItemDropperScreen;
import openblocks.client.gui.VacuumHopperScreen;
import openblocks.client.gui.XpBottlerScreen;
import openblocks.client.particle.LiquidSprayParticle;
import openblocks.client.tank.TankBlockEntityRenderer;
import openblocks.client.tank.TankItemRenderer;
import openblocks.client.tank.TankModel;
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

	public OpenBlocksClient(IEventBus modBus) {
		modBus.addListener(OBColorHandlers::registerBlockColors);
		modBus.addListener(OBColorHandlers::registerItemColors);
		modBus.addListener(OBFluidRendering::registerClientExtensions);
		modBus.addListener(TankItemRenderer::registerClientExtensions);
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
		modBus.addListener(DevNullItemRenderer::registerAdditional);
		modBus.addListener(DevNullItemRenderer::registerClientExtensions);
		modBus.addListener(DevNullItemRenderer::registerItemDecorations);

		NeoForge.EVENT_BUS.addListener(ElevatorInputHandler::onMovementInput);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderPlayerPre);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderPlayerPost);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onRenderPlayerPre);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onRenderPlayerPost);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onComputeCameraAngles);
		NeoForge.EVENT_BUS.addListener(CursorHighlightRenderer::onRenderLevelStage);
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
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(OBBlockEntities.TANK.get(), TankBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.GRAVE.get(), GraveBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.AUTO_ENCHANTMENT_TABLE.get(), AutoEnchantmentTableRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.PAINT_MIXER.get(), PaintMixerRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.CANVAS.get(), CanvasRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.FAN.get(), FanRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.BEAR_TRAP.get(), BearTrapRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.SPRINKLER.get(), SprinklerRenderer::new);
		event.registerEntityRenderer(OBEntities.NO_FLY_XP_ORB.get(), ExperienceOrbRenderer::new);
		event.registerEntityRenderer(OBEntities.LUGGAGE.get(), LuggageRenderer::new);
	}
}
