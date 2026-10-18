package openblocks.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.OBConfig;
import openblocks.common.CraneState;
import openblocks.entity.Magnet;

/**
 * Port of legacy {@code ItemCraneControl}: holding right click lowers or raises the magnet of the crane backpack (with
 * sneaking or, without {@code boringMode}, alternating), left click picks up or releases. The model shows the lift
 * direction and whether something is below or on the magnet (item properties {@code openblocks:lift} and
 * {@code openblocks:magnet}).
 */
public class CraneControlItem extends Item {

	public enum LiftState {
		IDLE, DOWN, UP
	}

	public enum MagnetState {
		IDLE, DETECTING, LOCKED
	}

	public CraneControlItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final CraneState state = CraneState.of(player);
		state.extending = OBConfig.CRANE.shiftControl.getAsBoolean()? player.isShiftKeyDown() : !state.extending;
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		if (!level.isClientSide && entity instanceof Player player && CraneBackpackItem.isWearing(player)) CraneState.of(player).updateLength();
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	// the swing reaches the server also when clicking into the air
	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		if (!entity.level().isClientSide && entity instanceof Player player) {
			final CraneState state = CraneState.of(player);
			final Magnet magnet = state.getMagnet();
			if (magnet != null && state.tryToggle(player.level().getGameTime())) magnet.toggle(player);
		}
		return true;
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		return true;
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return false;
	}

	public static LiftState getLiftState(ItemStack stack, @Nullable LivingEntity entity) {
		if (!(entity instanceof Player player) || !CraneBackpackItem.isWearing(player) || player.getUseItem() != stack) return LiftState.IDLE;
		return CraneState.of(player).extending? LiftState.DOWN : LiftState.UP;
	}

	public static MagnetState getMagnetState(@Nullable LivingEntity entity) {
		if (!(entity instanceof Player player) || !CraneBackpackItem.isWearing(player)) return MagnetState.IDLE;
		final Magnet magnet = CraneState.of(player).getMagnet();
		if (magnet == null) return MagnetState.IDLE;
		if (magnet.isLocked()) return MagnetState.LOCKED;
		return magnet.isAboveTarget()? MagnetState.DETECTING : MagnetState.IDLE;
	}
}
