package openblocks.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import openblocks.OpenBlocks;
import openblocks.advancements.BrickDroppedTrigger;
import openblocks.advancements.DevNullStackedTrigger;
import openblocks.item.DevNullItem;

/**
 * Port of the legacy advancement JSONs, which hang below the vanilla adventure tab.
 */
public class OBAdvancementProvider extends AdvancementProvider {

	public OBAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, List.of(new OpenBlocksAdvancements()));
	}

	private static class OpenBlocksAdvancements implements AdvancementSubProvider {

		private static final ResourceLocation ADVENTURE_ROOT = ResourceLocation.withDefaultNamespace("adventure/root");

		@Override
		public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
			// legacy tma2: "It's full of stars!"
			Advancement.Builder.advancement()
					.parent(ADVENTURE_ROOT)
					.display(Items.NETHER_STAR,
							Component.translatable("achievement.openblocks.stackOverflow"),
							Component.translatable("achievement.openblocks.stackOverflow.desc"),
							null, AdvancementType.TASK, true, true, false)
					.addCriterion("reached_the_limit", DevNullStackedTrigger.TriggerInstance.stacked(DevNullItem.STACK_LIMIT))
					.save(output, OpenBlocks.location("tma2").toString());

			// legacy oops: "Zoinks!"
			Advancement.Builder.advancement()
					.parent(ADVENTURE_ROOT)
					.display(Items.BRICK,
							Component.translatable("achievement.openblocks.droppedBrick"),
							Component.translatable("achievement.openblocks.droppedBrick.desc"),
							null, AdvancementType.TASK, true, true, true)
					.addCriterion("literally", BrickDroppedTrigger.TriggerInstance.dropped())
					.save(output, OpenBlocks.location("oops").toString());
		}
	}
}
