package openblocks.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} before a grave is created, so other mods can decide per item whether it goes into the grave,
 * is dropped normally or deleted. Cancelling it prevents the grave; all items are then dropped normally.
 */
public class GraveDropsEvent extends Event implements ICancellableEvent {

	public enum Action {
		STORE, DELETE, DROP
	}

	public static class ItemAction {
		public final ItemEntity item;
		public Action action;

		public ItemAction(ItemEntity item, Action action) {
			this.item = Objects.requireNonNull(item);
			this.action = Objects.requireNonNull(action);
		}
	}

	public final Player player;

	public final List<ItemAction> drops = new ArrayList<>();

	public GraveDropsEvent(Player player) {
		this.player = player;
	}

	public void addItem(ItemEntity item) {
		drops.add(new ItemAction(item, Action.STORE));
	}

	public void addItem(ItemEntity item, Action action) {
		drops.add(new ItemAction(item, action));
	}
}
