package openblocks.block.entity;

import javax.annotation.Nullable;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.OBConfig;
import openblocks.menu.BlockPlacerMenu;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityBlockPlacer}: places the first item of its inventory in front of it, like a player right-clicking.
 */
public class BlockPlacerBlockEntity extends BlockManipulatorBlockEntity implements MenuProvider {

	public static final int SLOTS = 9;

	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			// a powered placer places new items on its next tick, like legacy
			setChanged();
		}
	};

	public BlockPlacerBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.BLOCK_PLACER.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Override
	protected int getCooldown() {
		return OBConfig.BLOCK_PLACER.cooldown.get();
	}

	@Override
	protected boolean canWork(BlockState targetState, BlockPos target, Direction direction) {
		return findSlot() >= 0 && (targetState.isAir() || targetState.canBeReplaced());
	}

	private int findSlot() {
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			if (!inventory.getStackInSlot(slot).isEmpty()) return slot;
		return -1;
	}

	@Override
	protected void doWork(BlockState targetState, BlockPos target, Direction direction) {
		final int slot = findSlot();
		if (slot < 0) return;

		final ServerLevel serverLevel = (ServerLevel)level;
		final FakePlayer player = getFakePlayer(serverLevel);
		// vanilla blocks like pistons face the player, so the player stands beyond the target, looking back at the placer;
		// two blocks away, since some blocks take the player's height into account
		player.setPos(Vec3.atBottomCenterOf(target.relative(direction, 2)));
		player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target));
		player.setItemInHand(InteractionHand.MAIN_HAND, inventory.getStackInSlot(slot).copy());

		try {
			final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), direction.getOpposite(), target, false);
			player.gameMode.useItemOn(player, serverLevel, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);

			final ItemStack result = player.getMainHandItem();
			if (!ItemStack.matches(result, inventory.getStackInSlot(slot))) inventory.setStackInSlot(slot, result.copy());
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.blockplacer");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new BlockPlacerMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		inventory.deserializeNBT(registries, tag.getCompound("inventory"));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
	}
}
