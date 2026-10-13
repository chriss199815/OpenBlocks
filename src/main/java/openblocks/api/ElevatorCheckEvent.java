package openblocks.api;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Posted on the game bus for every block checked by an elevator. Setting a color turns the block into an elevator.
 */
public class ElevatorCheckEvent extends BlockEvent {

	private final Player player;

	@Nullable
	private DyeColor color;

	@Nullable
	private Direction rotation;

	public ElevatorCheckEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
		super(level, pos, state);
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public boolean isElevator() {
		return color != null;
	}

	@Nullable
	public DyeColor getColor() {
		return color;
	}

	public void setColor(@Nullable DyeColor color) {
		this.color = color;
	}

	@Nullable
	public Direction getRotation() {
		return rotation;
	}

	public void setRotation(@Nullable Direction rotation) {
		this.rotation = rotation;
	}
}
