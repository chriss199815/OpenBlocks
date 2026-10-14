package openblocks.block.entity;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import openblocks.OBConfig;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityBlockBreaker}: breaks the block in front like a player with the right tool would,
 * and puts the drops into the inventory behind it, if there is one.
 */
public class BlockBreakerBlockEntity extends BlockManipulatorBlockEntity {

	/**
	 * Tried in this order, the first one that can harvest the block is used; legacy OpenModsLib {@code toolProbes}.
	 */
	private static final List<ItemStack> TOOLS = List.of(
			new ItemStack(Items.DIAMOND_PICKAXE),
			new ItemStack(Items.DIAMOND_SHOVEL),
			new ItemStack(Items.DIAMOND_AXE),
			new ItemStack(Items.DIAMOND_SWORD),
			new ItemStack(Items.SHEARS));

	/**
	 * The breaker currently breaking a block, so {@link #onBlockDrops} knows where the drops go.
	 */
	@Nullable
	private static BlockBreakerBlockEntity breaking;

	public BlockBreakerBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.BLOCK_BREAKER.get(), pos, state);
	}

	@Override
	protected int getCooldown() {
		return OBConfig.BLOCK_BREAKER.cooldown.get();
	}

	@Override
	protected boolean canWork(BlockState targetState, BlockPos target, Direction direction) {
		// unlike legacy, fluids are left alone instead of being deleted
		return !targetState.isAir() && !(targetState.getBlock() instanceof LiquidBlock) && targetState.getDestroySpeed(level, target) >= 0;
	}

	@Override
	protected void doWork(BlockState targetState, BlockPos target, Direction direction) {
		final ServerLevel serverLevel = (ServerLevel)level;
		final FakePlayer player = getFakePlayer(serverLevel);
		player.setPos(Vec3.atCenterOf(worldPosition));
		player.setItemInHand(InteractionHand.MAIN_HAND, selectTool(player, targetState, target));

		// the vanilla path fires BreakEvent and BlockDropsEvent, so protection and drop-changing mods work
		breaking = this;
		try {
			if (player.gameMode.destroyBlock(target))
				serverLevel.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, target, Block.getId(targetState));
		} finally {
			breaking = null;
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	private static ItemStack selectTool(FakePlayer player, BlockState state, BlockPos pos) {
		for (ItemStack tool : TOOLS) {
			player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
			if (state.canHarvestBlock(player.level(), pos, player)) return tool.copy();
		}
		return TOOLS.get(0).copy();
	}

	/**
	 * Moves the drops of a block broken by a breaker into the inventory behind it; what doesn't fit stays in the world.
	 */
	public static void onBlockDrops(BlockDropsEvent event) {
		final BlockBreakerBlockEntity breaker = breaking;
		if (breaker == null || !event.getPos().equals(breaker.worldPosition.relative(breaker.getFacing()))) return;

		final Direction back = breaker.getFacing().getOpposite();
		final IItemHandler target = event.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, breaker.worldPosition.relative(back), back.getOpposite());
		if (target == null) return;

		event.getDrops().removeIf(drop -> {
			final ItemStack leftover = ItemHandlerHelper.insertItem(target, drop.getItem(), false);
			if (leftover.isEmpty()) return true;
			drop.setItem(leftover);
			return false;
		});
	}
}
