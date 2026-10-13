package openblocks;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.data.OBDataGenerators;
import openblocks.entity.NoFlyXpOrb;
import openblocks.network.OBNetwork;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBCapabilities;
import openblocks.registry.OBComponents;
import openblocks.registry.OBEntities;
import openblocks.registry.OBCreativeTabs;
import openblocks.registry.OBFluids;
import openblocks.registry.OBItems;
import openblocks.registry.OBParticleTypes;
import openblocks.registry.OBSounds;
import org.slf4j.Logger;

@Mod(OpenBlocks.MODID)
public class OpenBlocks {

	public static final String MODID = "openblocks";
	public static final Logger LOG = LogUtils.getLogger();

	public OpenBlocks(IEventBus modBus, ModContainer container) {
		OBFluids.FLUID_TYPES.register(modBus);
		OBFluids.FLUIDS.register(modBus);
		OBBlocks.BLOCKS.register(modBus);
		OBItems.ITEMS.register(modBus);
		OBComponents.COMPONENTS.register(modBus);
		OBBlockEntities.BLOCK_ENTITIES.register(modBus);
		OBEntities.ENTITIES.register(modBus);
		OBParticleTypes.PARTICLE_TYPES.register(modBus);
		OBCreativeTabs.TABS.register(modBus);
		OBSounds.SOUNDS.register(modBus);

		modBus.addListener(OBNetwork::register);
		modBus.addListener(OBCapabilities::register);
		modBus.addListener(OBDataGenerators::gatherData);

		NeoForge.EVENT_BUS.addListener(NoFlyXpOrb::onTargeting);

		container.registerConfig(ModConfig.Type.COMMON, OBConfig.SPEC);
	}

	public static ResourceLocation location(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
