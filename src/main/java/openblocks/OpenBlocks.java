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
import openblocks.common.BrickDrops;
import openblocks.common.DonationInfo;
import openblocks.common.ExplosiveEnchantmentHandler;
import openblocks.common.HangGliderHandler;
import openblocks.common.InfoBookHandler;
import openblocks.common.HeightMaps;
import openblocks.common.LastStandEnchantmentHandler;
import openblocks.common.OBCommands;
import openblocks.common.PlayerDeathHandler;
import openblocks.common.PlayerInventoryStore;
import openblocks.common.SleepingBagHandler;
import openblocks.common.TrophyDropHandler;
import openblocks.common.flimflam.DefaultFlimFlams;
import openblocks.common.flimflam.FlimFlamHandler;
import openblocks.entity.NoFlyXpOrb;
import openblocks.integration.computercraft.OBComputerCraft;
import openblocks.item.DevNullItem;
import openblocks.item.LuggageItem;
import openblocks.item.SpongeOnAStickItem;
import openblocks.network.OBNetwork;
import openblocks.registry.OBArmorMaterials;
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
import openblocks.registry.OBRecipeSerializers;
import openblocks.registry.OBSounds;
import openblocks.registry.OBStats;
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
		OBArmorMaterials.ARMOR_MATERIALS.register(modBus);
		OBItems.ITEMS.register(modBus);
		OBComponents.COMPONENTS.register(modBus);
		OBBlockEntities.BLOCK_ENTITIES.register(modBus);
		OBEntities.ENTITIES.register(modBus);
		OBEntities.DATA_SERIALIZERS.register(modBus);
		OBParticleTypes.PARTICLE_TYPES.register(modBus);
		OBMenus.MENUS.register(modBus);
		OBAttachments.ATTACHMENT_TYPES.register(modBus);
		OBGameRules.init();
		OBCreativeTabs.TABS.register(modBus);
		OBSounds.SOUNDS.register(modBus);
		OBTriggers.TRIGGERS.register(modBus);
		OBRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
		OBStats.STATS.register(modBus);

		modBus.addListener(OBNetwork::register);
		modBus.addListener(OBCapabilities::register);
		if (OBComputerCraft.isLoaded()) modBus.addListener(OBComputerCraft::registerCapabilities);
		modBus.addListener(OBEntities::registerAttributes);
		modBus.addListener(OBDataGenerators::gatherData);
		modBus.addListener(DonationInfo::onInterModProcess);
		modBus.addListener(SpongeOnAStickItem::setDurability);
		modBus.addListener(BrickDrops::registerStat);

		NeoForge.EVENT_BUS.addListener(NoFlyXpOrb::onTargeting);
		// after other mods changed the drops
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, BlockBreakerBlockEntity::onBlockDrops);
		// low priority and receiving cancelled events, like legacy, so other mods can claim drops first
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, true, PlayerDeathHandler::onLivingDrops);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PlayerInventoryStore::onPlayerDeath);
		NeoForge.EVENT_BUS.addListener(HangGliderHandler::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(HangGliderHandler::onChangedDimension);
		NeoForge.EVENT_BUS.addListener(SleepingBagHandler::onCanContinueSleeping);
		NeoForge.EVENT_BUS.addListener(SleepingBagHandler::onWakeUp);
		NeoForge.EVENT_BUS.addListener(InfoBookHandler::onPlayerLoggedIn);
		NeoForge.EVENT_BUS.addListener(LuggageItem::onEntityJoinLevel);
		NeoForge.EVENT_BUS.addListener(DevNullItem::onItemPickup);
		NeoForge.EVENT_BUS.addListener(TrophyDropHandler::onLivingDrops);
		NeoForge.EVENT_BUS.addListener(BrickDrops::onLivingDrops);
		NeoForge.EVENT_BUS.addListener(HeightMaps::onServerTick);
		NeoForge.EVENT_BUS.addListener(FlimFlamHandler::onIncomingDamage);
		NeoForge.EVENT_BUS.addListener(FlimFlamHandler::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(ExplosiveEnchantmentHandler::onFall);
		NeoForge.EVENT_BUS.addListener(ExplosiveEnchantmentHandler::onIncomingDamage);
		NeoForge.EVENT_BUS.addListener(LastStandEnchantmentHandler::onDamage);
		NeoForge.EVENT_BUS.addListener(OBCommands::register);

		DefaultFlimFlams.register();

		container.registerConfig(ModConfig.Type.COMMON, OBConfig.SPEC);
	}

	public static ResourceLocation location(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
