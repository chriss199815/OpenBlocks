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
 * Port of legacy {@code TriggerBrickDropped}: fires when the player drops a brick.
 */
public class BrickDroppedTrigger extends SimpleCriterionTrigger<BrickDroppedTrigger.TriggerInstance> {

	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player) {
		trigger(player, instance -> true);
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
				.apply(instance, TriggerInstance::new));

		public static Criterion<TriggerInstance> dropped() {
			return OBTriggers.BRICK_DROPPED.get().createCriterion(new TriggerInstance(Optional.empty()));
		}
	}
}
