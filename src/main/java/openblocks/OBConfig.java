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
	public static final ItemDropper ITEM_DROPPER = new ItemDropper(BUILDER);
	public static final BlockManipulator BLOCK_BREAKER = new BlockManipulator(BUILDER, "breaker", "block breaker");
	public static final BlockManipulator BLOCK_PLACER = new BlockManipulator(BUILDER, "placer", "block placer");
	public static final Canvas CANVAS = new Canvas(BUILDER);
	public static final Fan FAN = new Fan(BUILDER);
	public static final Sprinkler SPRINKLER = new Sprinkler(BUILDER);
	public static final DevNull DEV_NULL = new DevNull(BUILDER);
	public static final Cursor CURSOR = new Cursor(BUILDER);
	public static final Ladder LADDER = new Ladder(BUILDER);
	public static final Sponge SPONGE = new Sponge(BUILDER);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static class Ladder {
		public final ModConfigSpec.BooleanValue infiniteMode;

		private Ladder(ModConfigSpec.Builder builder) {
			builder.push("ladder");
			infiniteMode = builder
					.comment("If true, ladders will behave in old way: single item will place ladder all the way down, but it will not drop when broken")
					.define("infiniteMode", false);
			builder.pop();
		}
	}

	public static class Sponge {
		public final ModConfigSpec.IntValue range;
		public final ModConfigSpec.BooleanValue blockUpdate;

		private Sponge(ModConfigSpec.Builder builder) {
			builder.push("sponge");
			range = builder
					.comment("Sponge block range (distance from center)")
					.defineInRange("spongeRange", 3, 0, 16);
			blockUpdate = builder
					.comment("Should sponge block update neighbours after liquid removal?")
					.define("blockUpdate", false);
			builder.pop();
		}
	}

	public static class DevNull {
		public final ModConfigSpec.BooleanValue sneakClickToGui;
		public final ModConfigSpec.IntValue countColor;

		private DevNull(ModConfigSpec.Builder builder) {
			builder.push("devnull");
			sneakClickToGui = builder
					.comment("If true, /dev/null will require sneaking in addition to clicking air to open gui")
					.define("sneakClickToGui", true);
			countColor = builder
					.comment("Color of contained stack size in GUI")
					.defineInRange("countColor", 0xFFFF00, 0, 0xFFFFFF);
			builder.pop();
		}
	}

	public static class Cursor {
		public final ModConfigSpec.IntValue maxDistance;

		private Cursor(ModConfigSpec.Builder builder) {
			builder.push("cursor");
			maxDistance = builder
					.comment("Maximum distance cursor can reach (warning: increasing may cause crashes)")
					.defineInRange("cursorMaxDistance", 64, 0, Integer.MAX_VALUE);
			builder.pop();
		}
	}

	public static class Sprinkler {
		public final ModConfigSpec.IntValue fertilizeChance;
		public final ModConfigSpec.IntValue bonemealFertilizeChance;
		public final ModConfigSpec.IntValue effectiveRange;
		public final ModConfigSpec.IntValue internalTankCapacity;
		public final ModConfigSpec.IntValue bonemealConsumeRate;
		public final ModConfigSpec.IntValue waterConsumeRate;

		private Sprinkler(ModConfigSpec.Builder builder) {
			builder.push("sprinkler");
			fertilizeChance = builder
					.comment("1/chance crops will be fertilized without bonemeal")
					.defineInRange("fertilizeChance", 500, 1, Integer.MAX_VALUE);
			bonemealFertilizeChance = builder
					.comment("1/chance crops will be fertilized with bonemeal")
					.defineInRange("bonemealFertilizeChance", 200, 1, Integer.MAX_VALUE);
			effectiveRange = builder
					.comment("The range in each cardinal direction crops will be affected.")
					.defineInRange("effectiveRange", 4, 0, 16);
			internalTankCapacity = builder
					.comment("Capacity (in mB) of internal tank.")
					.defineInRange("internalTankCapacity", 50, 1, Integer.MAX_VALUE);
			bonemealConsumeRate = builder
					.comment("Consume rate bonemeal (ticks/item).")
					.defineInRange("bonemealConsumeRate", 600, 1, Integer.MAX_VALUE);
			waterConsumeRate = builder
					.comment("Consume rate of sprinkler (ticks/mB).")
					.defineInRange("waterConsumeRate", 20, 1, Integer.MAX_VALUE);
			builder.pop();
		}
	}

	public static class Fan {
		public final ModConfigSpec.DoubleValue force;
		public final ModConfigSpec.DoubleValue range;
		public final ModConfigSpec.BooleanValue isRedstoneActivated;

		private Fan(ModConfigSpec.Builder builder) {
			builder.push("fan");
			force = builder
					.comment("Maximum force applied every tick to entities nearby (linear decay)")
					.defineInRange("fanForce", 0.05, 0.0, 1.0);
			range = builder
					.comment("Range of fan in blocks")
					.defineInRange("fanRange", 10.0, 1.0, 64.0);
			isRedstoneActivated = builder
					.comment("Is fan force controlled by redstone current")
					.define("isRedstoneActivated", true);
			builder.pop();
		}
	}

	public static class Canvas {
		public final ModConfigSpec.BooleanValue paintbrushReplacesBlocks;
		public final ModConfigSpec.ConfigValue<List<? extends String>> replaceBlacklist;

		private Canvas(ModConfigSpec.Builder builder) {
			builder.push("canvas");
			paintbrushReplacesBlocks = builder
					.comment("If true, paintbrush will replace suitable blocks with canvas. Otherwise, it will only try to paint blocks")
					.define("paintbrushReplacesBlocks", true);
			replaceBlacklist = builder
					.comment("List of block ids that should not be replaceable by canvas (by using brush, stencil, etc)")
					.defineListAllowEmpty("replaceBlacklist", List.of(), () -> "minecraft:stone", e -> e instanceof String);
			builder.pop();
		}
	}

	public static class BlockManipulator {
		public final ModConfigSpec.IntValue cooldown;

		private BlockManipulator(ModConfigSpec.Builder builder, String category, String name) {
			builder.push(category);
			// replaces legacy "actionLimit" (actions per tick), which let it work every tick
			cooldown = builder
					.comment("Ticks the " + name + " waits after each action; while powered, it keeps working at this rate")
					.defineInRange("cooldown", 10, 1, 1200);
			builder.pop();
		}
	}

	public static class ItemDropper {
		public final ModConfigSpec.DoubleValue maxItemDropSpeed;

		private ItemDropper(ModConfigSpec.Builder builder) {
			builder.push("itemdropper");
			maxItemDropSpeed = builder
					.comment("Maximum speed that can be set in item dropper GUI")
					.defineInRange("maxItemDropSpeed", 4.0, 0.0, 10.0);
			builder.pop();
		}
	}

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
