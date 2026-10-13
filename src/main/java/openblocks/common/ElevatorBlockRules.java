package openblocks.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.OBConfig;
import openblocks.OpenBlocks;
import openblocks.api.ElevatorCheckEvent;

/**
 * Parses {@code specialBlockRules} and {@code overrides} from the elevator config. Parsed lists are cached until the config value changes.
 */
public class ElevatorBlockRules {

	public enum Action {
		IGNORE, ABORT, INCREMENT
	}

	private record Rule(Predicate<BlockState> matcher, Action action) {}

	private record ColorOverride(Predicate<BlockState> matcher, DyeColor color) {}

	private static List<? extends String> rulesSource;
	private static List<Rule> rules = List.of();

	private static List<? extends String> overridesSource;
	private static List<ColorOverride> overrides = List.of();

	private static Predicate<BlockState> parseMatcher(String desc) {
		if (desc.startsWith("#")) {
			final TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(desc.substring(1)));
			return state -> state.is(tag);
		}

		final ResourceLocation id = ResourceLocation.parse(desc);
		if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Unknown block " + id);
		final Block block = BuiltInRegistries.BLOCK.get(id);
		return state -> state.is(block);
	}

	private static synchronized List<Rule> getRules() {
		final List<? extends String> source = OBConfig.ELEVATOR.specialBlockRules.get();
		if (source != rulesSource) {
			final List<Rule> parsed = new ArrayList<>();
			for (String entry : source) {
				try {
					final int split = entry.lastIndexOf(':');
					if (split < 0) throw new IllegalArgumentException("Missing action");
					final Action action = Action.valueOf(entry.substring(split + 1).trim().toUpperCase(Locale.ROOT));
					parsed.add(new Rule(parseMatcher(entry.substring(0, split).trim()), action));
				} catch (Exception e) {
					OpenBlocks.LOG.warn("Invalid elevator rule '{}': {}", entry, e.getMessage());
				}
			}
			rules = List.copyOf(parsed);
			rulesSource = source;
		}
		return rules;
	}

	private static synchronized List<ColorOverride> getOverrides() {
		final List<? extends String> source = OBConfig.ELEVATOR.overrides.get();
		if (source != overridesSource) {
			final List<ColorOverride> parsed = new ArrayList<>();
			for (String entry : source) {
				try {
					final String[] parts = entry.split(";");
					if (parts.length > 2) throw new IllegalArgumentException("Expected <block>[;<color>]");
					final DyeColor color = parts.length == 2? DyeColor.byName(parts[1].trim(), null) : DyeColor.WHITE;
					if (color == null) throw new IllegalArgumentException("Unknown color " + parts[1]);
					parsed.add(new ColorOverride(parseMatcher(parts[0].trim()), color));
				} catch (Exception e) {
					OpenBlocks.LOG.warn("Invalid elevator override '{}': {}", entry, e.getMessage());
				}
			}
			overrides = List.copyOf(parsed);
			overridesSource = source;
		}
		return overrides;
	}

	private static boolean isPassable(BlockGetter level, BlockPos pos, BlockState state) {
		return OBConfig.ELEVATOR.ignoreHalfBlocks.get() && !state.isCollisionShapeFullBlock(level, pos);
	}

	public static Action getActionForBlock(BlockGetter level, BlockPos pos, BlockState state) {
		for (Rule rule : getRules())
			if (rule.matcher.test(state)) return rule.action;

		return isPassable(level, pos, state)? Action.IGNORE : Action.INCREMENT;
	}

	public static void configureEvent(ElevatorCheckEvent evt) {
		for (ColorOverride override : getOverrides()) {
			if (override.matcher.test(evt.getState())) {
				evt.setColor(override.color);
				evt.setRotation(null);
				return;
			}
		}
	}
}
