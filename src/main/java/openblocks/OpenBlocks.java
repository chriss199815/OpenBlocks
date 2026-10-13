package openblocks;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import openblocks.data.OBDataGenerators;
import openblocks.network.OBNetwork;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBCreativeTabs;
import openblocks.registry.OBItems;
import openblocks.registry.OBSounds;
import org.slf4j.Logger;

@Mod(OpenBlocks.MODID)
public class OpenBlocks {

	public static final String MODID = "openblocks";
	public static final Logger LOG = LogUtils.getLogger();

	public OpenBlocks(IEventBus modBus, ModContainer container) {
		OBBlocks.BLOCKS.register(modBus);
		OBItems.ITEMS.register(modBus);
		OBBlockEntities.BLOCK_ENTITIES.register(modBus);
		OBCreativeTabs.TABS.register(modBus);
		OBSounds.SOUNDS.register(modBus);

		modBus.addListener(OBNetwork::register);
		modBus.addListener(OBDataGenerators::gatherData);

		container.registerConfig(ModConfig.Type.COMMON, OBConfig.SPEC);
	}

	public static ResourceLocation location(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
