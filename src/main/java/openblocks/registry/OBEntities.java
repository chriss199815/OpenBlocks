package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.entity.NoFlyXpOrb;

public class OBEntities {

	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, OpenBlocks.MODID);

	public static final DeferredHolder<EntityType<?>, EntityType<NoFlyXpOrb>> NO_FLY_XP_ORB = ENTITIES.register("xp_orb_no_fly",
			() -> EntityType.Builder.<NoFlyXpOrb>of(NoFlyXpOrb::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(6)
					.updateInterval(20)
					.build("xp_orb_no_fly"));
}
