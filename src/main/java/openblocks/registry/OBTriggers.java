package openblocks.registry;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.advancements.BrickDroppedTrigger;
import openblocks.advancements.DevNullStackedTrigger;

/**
 * Advancement criterion triggers; replaces legacy {@code Criterions}.
 */
public class OBTriggers {

	public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, OpenBlocks.MODID);

	public static final DeferredHolder<CriterionTrigger<?>, DevNullStackedTrigger> DEV_NULL_STACKED = TRIGGERS.register("dev_null_stacked", DevNullStackedTrigger::new);
	public static final DeferredHolder<CriterionTrigger<?>, BrickDroppedTrigger> BRICK_DROPPED = TRIGGERS.register("brick_dropped", BrickDroppedTrigger::new);
}
