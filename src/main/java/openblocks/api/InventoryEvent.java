package openblocks.api;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} when OpenBlocks stores or restores a player inventory backup ({@code ob_inventory}
 * command, death backups), so mods with extra player inventories (e.g. accessory slots) can add them to the backup.
 */
public class InventoryEvent extends PlayerEvent {

	protected final Map<String, SubInventory> subInventories;

	protected InventoryEvent(Player player, Map<String, SubInventory> subInventories) {
		super(player);
		this.subInventories = subInventories;
	}

	public Map<String, SubInventory> getSubInventories() {
		return Map.copyOf(subInventories);
	}

	public static class SubInventory {
		private final Map<Integer, ItemStack> slots = new HashMap<>();

		public SubInventory addItemStack(int slot, ItemStack stack) {
			slots.put(slot, stack);
			return this;
		}

		public ItemStack getItemStack(int slot) {
			return slots.getOrDefault(slot, ItemStack.EMPTY);
		}

		public Map<Integer, ItemStack> asMap() {
			return Map.copyOf(slots);
		}
	}

	/**
	 * Fired when a backup is created; add contents with {@link #createSubInventory(String)}.
	 */
	public static class Store extends InventoryEvent {

		public Store(Player player) {
			super(player, new HashMap<>());
		}

		public SubInventory createSubInventory(String id) {
			final SubInventory result = new SubInventory();
			final SubInventory prev = subInventories.put(id, result);
			Preconditions.checkState(prev == null, "Sub inventory with id %s already exists", id);
			return result;
		}
	}

	/**
	 * Fired when a backup is restored to a player; put the stored contents back with {@link #getSubInventory(String)}.
	 */
	public static class Load extends InventoryEvent {

		public Load(Player player, Map<String, SubInventory> subInventories) {
			super(player, Map.copyOf(subInventories));
		}

		public SubInventory getSubInventory(String id) {
			return subInventories.get(id);
		}
	}
}
