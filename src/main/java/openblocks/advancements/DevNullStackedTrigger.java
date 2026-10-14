package openblocks.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import openblocks.registry.OBTriggers;

/**
 * Port of legacy {@code TriggerDevNullStack}: fires when the player changes a /dev/null, with how deep it is nested.
 */
public class DevNullStackedTrigger extends SimpleCriterionTrigger<DevNullStackedTrigger.TriggerInstance> {

	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, int depth) {
		trigger(player, instance -> instance.matches(depth));
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player, int depth) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.INT.fieldOf("depth").forGetter(TriggerInstance::depth))
				.apply(instance, TriggerInstance::new));

		public static Criterion<TriggerInstance> stacked(int depth) {
			return OBTriggers.DEV_NULL_STACKED.get().createCriterion(new TriggerInstance(Optional.empty(), depth));
		}

		public boolean matches(int depth) {
			return this.depth <= depth;
		}
	}
}
