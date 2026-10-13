package openblocks;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Port of the legacy {@code Config} class. Values are added here only when the feature that uses them is ported.
 */
public class OBConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final Elevator ELEVATOR = new Elevator(BUILDER);
	public static final Xp XP = new Xp(BUILDER);
	public static final Tanks TANKS = new Tanks(BUILDER);
	public static final Graves GRAVES = new Graves(BUILDER);
	public static final HangGlider HANG_GLIDER = new HangGlider(BUILDER);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static class HangGlider {
		public final ModConfigSpec.BooleanValue enableThermal;

		private HangGlider(ModConfigSpec.Builder builder) {
			builder.push("hangglider");
			enableThermal = builder
					.comment("Enable a whole new level of hanggliding experience through thermal lift")
					.define("enableThermal", true);
			builder.pop();
		}
	}

	public static class Graves {
		public final ModConfigSpec.BooleanValue destructiveGraves;
		public final ModConfigSpec.IntValue spawnRange;
		public final ModConfigSpec.BooleanValue spawnSkeletons;
		public final ModConfigSpec.DoubleValue skeletonSpawnRate;
		public final ModConfigSpec.BooleanValue requiresGraveInInv;
		public final ModConfigSpec.DoubleValue specialActionFrequency;
		public final ModConfigSpec.BooleanValue canSpawnBase;

		private Graves(ModConfigSpec.Builder builder) {
			builder.push("graves");
			destructiveGraves = builder
					.comment("Try to overwrite blocks with graves if no suitable place is found on first try")
					.define("destructiveGraves", false);
			spawnRange = builder
					.comment("Size of cube searched for spaces suitable for grave spawning")
					.defineInRange("spawnRange", 10, 0, 64);
			spawnSkeletons = builder
					.comment("Should grave randomly spawn skeletons")
					.define("spawnSkeletons", true);
			skeletonSpawnRate = builder
					.comment("Spawn rate, range: 0..1, default: about 1 per 50s")
					.defineInRange("skeletonSpawnRate", 0.002, 0.0, 1.0);
			requiresGraveInInv = builder
					.comment("Require gravestone to be in a player's inventory (it is consumed)")
					.define("requiresGraveInInv", false);
			specialActionFrequency = builder
					.comment("Frequency of special action on grave digging, 0..1")
					.defineInRange("specialActionFrequency", 0.03, 0.0, 1.0);
			canSpawnBase = builder
					.comment("Can grave spawn single block of dirt when it has no block under?")
					.define("canSpawnBase", true);
			builder.pop();
		}
	}

	public static class Tanks {
		public final ModConfigSpec.IntValue bucketsPerTank;
		public final ModConfigSpec.BooleanValue shouldTanksUpdate;
		public final ModConfigSpec.BooleanValue displayAllFilledTanks;
		public final ModConfigSpec.IntValue fluidDifferenceUpdateThreshold;
		public final ModConfigSpec.BooleanValue emitLight;

		private Tanks(ModConfigSpec.Builder builder) {
			builder.push("tanks");
			bucketsPerTank = builder
					.comment("The amount of buckets each tank can hold")
					.defineInRange("bucketsPerTank", 16, 1, 1024);
			shouldTanksUpdate = builder
					.comment("Should tanks try to balance liquid amounts with neighbours")
					.define("tankTicks", true);
			displayAllFilledTanks = builder
					.comment("Should filled tanks be searchable with creative menu")
					.define("displayAllFluids", true);
			fluidDifferenceUpdateThreshold = builder
					.comment("Minimal difference in fluid level between neighbors required for tank update (can be used for performance finetuning)")
					.defineInRange("fluidDifferenceUpdateThreshold", 0, 0, Integer.MAX_VALUE);
			emitLight = builder
					.comment("Tanks will emit light when they contain a liquid that glows (eg. lava)")
					.define("emitLight", true);
			builder.pop();
		}
	}

	public static class Xp {
		public final ModConfigSpec.IntValue liquidRatio;

		private Xp(ModConfigSpec.Builder builder) {
			builder.push("xp");
			liquidRatio = builder
					.comment("Storage in mB needed to store single XP point. Applies to all fluids in the 'c:experience' tag (replaces legacy 'additionalXpFluids').",
							"Other mods using 'c:experience' expect 20, so only change this if you know what you're doing")
					.defineInRange("xpToLiquidRatio", 20, 1, 1000);
			builder.pop();
		}
	}

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
