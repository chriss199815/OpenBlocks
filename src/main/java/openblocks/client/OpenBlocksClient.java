package openblocks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.OpenBlocks;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
		modBus.addListener(OpenBlocksClient::registerRenderers);
		modBus.addListener(LiquidSprayParticle::register);
		modBus.addListener(OpenBlocksClient::registerScreens);

		NeoForge.EVENT_BUS.addListener(ElevatorInputHandler::onMovementInput);
	}

	private static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(OBMenus.XP_BOTTLER.get(), XpBottlerScreen::new);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(OBBlockEntities.TANK.get(), TankBlockEntityRenderer::new);
		event.registerEntityRenderer(OBEntities.NO_FLY_XP_ORB.get(), ExperienceOrbRenderer::new);
	}
}
