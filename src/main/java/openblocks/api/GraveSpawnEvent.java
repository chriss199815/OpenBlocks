package openblocks.api;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} before a grave block is placed. Listeners may move the grave, change its texts or cancel it
 * (the loot is then dropped normally). {@link #location} is {@code null} if no suitable place was found.
 */
public class GraveSpawnEvent extends PlayerEvent implements ICancellableEvent {

	public final List<ItemEntity> loot;

	public Component clickText;

	public String gravestoneText;

	@Nullable
	public BlockPos location;

	public GraveSpawnEvent(Player player, @Nullable BlockPos location, List<ItemEntity> loot, String gravestoneText, Component clickText) {
		super(player);
		this.loot = loot;
		this.gravestoneText = gravestoneText;
		this.clickText = clickText;
		this.location = location;
	}
}
