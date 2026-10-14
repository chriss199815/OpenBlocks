package openblocks;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import openblocks.block.entity.BlockBreakerBlockEntity;
import openblocks.data.OBDataGenerators;
import openblocks.common.DonationInfo;
import openblocks.common.HangGliderHandler;
import openblocks.common.PlayerDeathHandler;
import openblocks.common.SleepingBagHandler;
import openblocks.entity.NoFlyXpOrb;
import openblocks.item.DevNullItem;
import openblocks.item.LuggageItem;
import openblocks.item.SpongeOnAStickItem;
import openblocks.network.OBNetwork;
import openblocks.registry.OBAttachments;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBCapabilities;
import openblocks.registry.OBComponents;
import openblocks.registry.OBEntities;
import openblocks.registry.OBCreativeTabs;
import openblocks.registry.OBFluids;
import openblocks.registry.OBGameRules;
import openblocks.registry.OBItems;
import openblocks.registry.OBMenus;
import openblocks.registry.OBParticleTypes;
import openblocks.registry.OBSounds;
import openblocks.registry.OBTriggers;
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
		OBMenus.MENUS.register(modBus);
		OBAttachments.ATTACHMENT_TYPES.register(modBus);
		OBGameRules.init();
		OBCreativeTabs.TABS.register(modBus);
		OBSounds.SOUNDS.register(modBus);
		OBTriggers.TRIGGERS.register(modBus);

		modBus.addListener(OBNetwork::register);
		modBus.addListener(OBCapabilities::register);
		modBus.addListener(OBEntities::registerAttributes);
		modBus.addListener(OBDataGenerators::gatherData);
		modBus.addListener(DonationInfo::onInterModProcess);
		modBus.addListener(SpongeOnAStickItem::setDurability);

		NeoForge.EVENT_BUS.addListener(NoFlyXpOrb::onTargeting);
		// after other mods changed the drops
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, BlockBreakerBlockEntity::onBlockDrops);
		// low priority and receiving cancelled events, like legacy, so other mods can claim drops first
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, true, PlayerDeathHandler::onLivingDrops);
		NeoForge.EVENT_BUS.addListener(HangGliderHandler::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(HangGliderHandler::onChangedDimension);
		NeoForge.EVENT_BUS.addListener(SleepingBagHandler::onCanContinueSleeping);
		NeoForge.EVENT_BUS.addListener(SleepingBagHandler::onWakeUp);
		NeoForge.EVENT_BUS.addListener(LuggageItem::onEntityJoinLevel);
		NeoForge.EVENT_BUS.addListener(DevNullItem::onItemPickup);

		container.registerConfig(ModConfig.Type.COMMON, OBConfig.SPEC);
	}

	public static ResourceLocation location(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
