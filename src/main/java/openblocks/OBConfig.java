package openblocks;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Port of the legacy {@code Config} class. Values are added here only when the feature that uses them is ported.
 */
public class OBConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final Elevator ELEVATOR = new Elevator(BUILDER);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static class Elevator {
		public final ModConfigSpec.IntValue travelDistance;
		public final ModConfigSpec.BooleanValue ignoreAllBlocks;
		public final ModConfigSpec.BooleanValue ignoreHalfBlocks;
		public final ModConfigSpec.IntValue maxPassThrough;
		public final ModConfigSpec.BooleanValue centerOnBlock;
		public final ModConfigSpec.BooleanValue irregularBlocksArePassable;
		public final ModConfigSpec.DoubleValue xpDrainRatio;
		public final ModConfigSpec.ConfigValue<List<? extends String>> specialBlockRules;
		public final ModConfigSpec.ConfigValue<List<? extends String>> overrides;

		private Elevator(ModConfigSpec.Builder builder) {
			builder.push("elevator");
			travelDistance = builder
					.comment("The range of the elevator")
					.defineInRange("searchDistance", 20, 1, 256);
			ignoreAllBlocks = builder
					.comment("Disable limit of blocks between elevators (equivalent to maxPassThrough = infinity)")
					.define("ignoreAllBlocks", false);
			ignoreHalfBlocks = builder
					.comment("The elevator will ignore half blocks when counting the blocks it can pass through")
					.define("ignoreHalfBlocks", false);
			maxPassThrough = builder
					.comment("The maximum amount of blocks the elevator can pass through before the teleport fails")
					.defineInRange("maxPassThrough", 4, 0, 256);
			centerOnBlock = builder
					.comment("Should elevator move player to center of block after teleporting")
					.define("centerOnBlock", false);
			irregularBlocksArePassable = builder
					.comment("The elevator will try to pass through blocks that have custom collision boxes")
					.define("irregularBlocksArePassable", true);
			xpDrainRatio = builder
					.comment("XP consumed by elevator (total amount = ratio * distance)")
					.defineInRange("xpDrainRatio", 0.0, 0.0, Double.MAX_VALUE);
			specialBlockRules = builder
					.comment("Defines blocks that are handled specially by elevators. Entries are in form <block id>:<action> or #<block tag>:<action>.",
							"Possible actions: abort (elevator can't pass block), increment (counts for maxPassThrough limit) and ignore")
					.defineListAllowEmpty("specialBlockRules", List.of(), () -> "minecraft:glass:ignore", e -> e instanceof String);
			overrides = builder
					.comment("Use to configure blocks as elevators. Entries are in form <block id>[;<color>] or #<block tag>[;<color>], color defaults to white.",
							"Examples: 'minecraft:white_wool' - white wool acts as white elevator, '#minecraft:wool;yellow' - any wool acts as yellow elevator")
					.defineListAllowEmpty("overrides", List.of(), () -> "minecraft:white_wool", e -> e instanceof String);
			builder.pop();
		}
	}
}
