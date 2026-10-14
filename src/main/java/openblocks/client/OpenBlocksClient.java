package openblocks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.OpenBlocks;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import openblocks.client.gui.ItemDropperScreen;
import openblocks.client.gui.VacuumHopperScreen;
import openblocks.client.gui.XpBottlerScreen;
import openblocks.client.particle.LiquidSprayParticle;
import openblocks.client.tank.TankBlockEntityRenderer;
import openblocks.client.tank.TankItemRenderer;
import openblocks.client.tank.TankModel;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBEntities;
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
		modBus.addListener(OpenBlocksClient::registerRenderers);
		modBus.addListener(LiquidSprayParticle::register);
		modBus.addListener(OpenBlocksClient::registerScreens);
		modBus.addListener(SleepingBagLayer::registerLayerDefinitions);
		modBus.addListener(SleepingBagLayer::addLayers);

		NeoForge.EVENT_BUS.addListener(ElevatorInputHandler::onMovementInput);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderPlayerPre);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderPlayerPost);
		NeoForge.EVENT_BUS.addListener(HangGliderRenderer::onRenderLevelStage);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onRenderPlayerPre);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onRenderPlayerPost);
		NeoForge.EVENT_BUS.addListener(SleepingBagLayer::onComputeCameraAngles);
	}

	private static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(OBMenus.XP_BOTTLER.get(), XpBottlerScreen::new);
		event.register(OBMenus.VACUUM_HOPPER.get(), VacuumHopperScreen::new);
		event.register(OBMenus.ITEM_DROPPER.get(), ItemDropperScreen::new);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(OBBlockEntities.TANK.get(), TankBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(OBBlockEntities.GRAVE.get(), GraveBlockEntityRenderer::new);
		event.registerEntityRenderer(OBEntities.NO_FLY_XP_ORB.get(), ExperienceOrbRenderer::new);
	}
}
