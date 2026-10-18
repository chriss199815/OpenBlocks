package openblocks.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Port of legacy {@code IFlimFlamRegistry}/{@code FlimFlamRegistry}: the flim flams the flim flam enchantment can
 * deliver. Other mods may register their own (during mod construction or common setup).
 */
public class FlimFlamRegistry {

	private static final Map<String, Entry> FLIM_FLAMS = new LinkedHashMap<>();

	/**
	 * Port of legacy {@code FlimFlamDescriptionSimple}.
	 *
	 * @param cost luck it takes; negative, so bad luck gets better
	 * @param weight how likely it is chosen among the possible ones
	 * @param safe true if it does not kill (or only very rarely), see config {@code tomfoolery.safeOnly}
	 * @param silent true if the player gets no "something weird happens" message
	 */
	public record Entry(String name, int cost, int weight, int lowerLuck, int upperLuck, boolean safe, boolean silent, IFlimFlamAction action) {

		public boolean canApply(int luck) {
			return lowerLuck <= luck && luck <= upperLuck;
		}

		public Entry markSafe() {
			return replace(new Entry(name, cost, weight, lowerLuck, upperLuck, true, silent, action));
		}

		public Entry markSilent() {
			return replace(new Entry(name, cost, weight, lowerLuck, upperLuck, safe, true, action));
		}

		/**
		 * Luck range the flim flam can happen in; by default up to its cost.
		 */
		public Entry setRange(int a, int b) {
			return replace(new Entry(name, cost, weight, Math.min(a, b), Math.max(a, b), safe, silent, action));
		}

		private static Entry replace(Entry entry) {
			FLIM_FLAMS.put(entry.name(), entry);
			return entry;
		}
	}

	public static synchronized Entry register(String name, int cost, int weight, IFlimFlamAction action) {
		final Entry entry = cost < 0
				? new Entry(name, cost, weight, Integer.MIN_VALUE, cost, false, false, action)
				: new Entry(name, cost, weight, cost, Integer.MAX_VALUE, false, false, action);
		FLIM_FLAMS.put(name, entry);
		return entry;
	}

	public static List<Entry> getFlimFlams() {
		return Collections.unmodifiableList(new ArrayList<>(FLIM_FLAMS.values()));
	}

	@Nullable
	public static Entry get(String name) {
		return FLIM_FLAMS.get(name);
	}

	public static List<String> getNames() {
		return List.copyOf(FLIM_FLAMS.keySet());
	}
}
