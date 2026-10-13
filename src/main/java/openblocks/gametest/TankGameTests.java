package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.TankBlock;
import openblocks.block.entity.TankBlockEntity;
import openblocks.common.FluidXpUtils;
import openblocks.common.TankEdge;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.TankBlockItem;
import openblocks.registry.OBBlocks;
import openblocks.util.XpUtils;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class TankGameTests {

	private static TankBlockEntity placeTank(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, OBBlocks.TANK.get());
		return helper.getBlockEntity(pos);
	}

	private static FluidStack water(int amount) {
		return new FluidStack(Fluids.WATER, amount);
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void fillOverflowsIntoTankAbove(GameTestHelper helper) {
		final TankBlockEntity bottom = placeTank(helper, new BlockPos(1, 1, 1));
		final TankBlockEntity top = placeTank(helper, new BlockPos(1, 2, 1));
		final int capacity = bottom.getCapacity();

		final int filled = bottom.getFluidHandler().fill(water(capacity + 500), IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(filled == capacity + 500, "Expected whole amount to fit in column, filled " + filled);
		helper.assertTrue(bottom.getFluid().getAmount() == capacity, "Expected full bottom tank, got " + bottom.getFluid().getAmount());
		helper.assertTrue(top.getFluid().getAmount() == 500, "Expected 500 mB in top tank, got " + top.getFluid().getAmount());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void drainEmptiesColumnFromTop(GameTestHelper helper) {
		final TankBlockEntity bottom = placeTank(helper, new BlockPos(1, 1, 1));
		final TankBlockEntity top = placeTank(helper, new BlockPos(1, 2, 1));
		final int capacity = bottom.getCapacity();
		bottom.getFluidHandler().fill(water(capacity + 500), IFluidHandler.FluidAction.EXECUTE);

		final FluidStack drained = bottom.getFluidHandler().drain(700, IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(drained.getAmount() == 700, "Expected 700 mB drained, got " + drained.getAmount());
		helper.assertTrue(top.getFluid().isEmpty(), "Expected empty top tank, got " + top.getFluid().getAmount());
		helper.assertTrue(bottom.getFluid().getAmount() == capacity - 200, "Expected bottom tank to lose 200 mB, got " + bottom.getFluid().getAmount());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void rejectsDifferentFluid(GameTestHelper helper) {
		final TankBlockEntity tank = placeTank(helper, new BlockPos(1, 1, 1));
		tank.getFluidHandler().fill(water(1000), IFluidHandler.FluidAction.EXECUTE);

		final int filled = tank.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(filled == 0, "Lava must not mix with water, filled " + filled);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void fluidFallsIntoTankBelow(GameTestHelper helper) {
		final TankBlockEntity bottom = placeTank(helper, new BlockPos(1, 1, 1));
		final TankBlockEntity top = placeTank(helper, new BlockPos(1, 2, 1));
		// filling the top tank's own column doesn't reach the tank below, the tick moves it down
		top.getFluidHandler().fill(water(1000), IFluidHandler.FluidAction.EXECUTE);

		helper.succeedWhen(() -> {
			helper.assertTrue(top.getFluid().isEmpty(), "Expected empty top tank, got " + top.getFluid().getAmount());
			helper.assertTrue(bottom.getFluid().getAmount() == 1000, "Expected 1000 mB in bottom tank, got " + bottom.getFluid().getAmount());
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void neighboursBalance(GameTestHelper helper) {
		final TankBlockEntity left = placeTank(helper, new BlockPos(0, 1, 1));
		final TankBlockEntity right = placeTank(helper, new BlockPos(1, 1, 1));
		left.getFluidHandler().fill(water(2000), IFluidHandler.FluidAction.EXECUTE);

		helper.succeedWhen(() -> {
			helper.assertTrue(left.getFluid().getAmount() == 1000, "Expected 1000 mB in left tank, got " + left.getFluid().getAmount());
			helper.assertTrue(right.getFluid().getAmount() == 1000, "Expected 1000 mB in right tank, got " + right.getFluid().getAmount());
		});
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void dropKeepsContents(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 1, 1);
		final TankBlockEntity tank = placeTank(helper, pos);
		tank.getFluidHandler().fill(water(3000), IFluidHandler.FluidAction.EXECUTE);

		final List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), tank);
		helper.assertTrue(drops.size() == 1, "Expected single drop, got " + drops);
		final FluidStack contents = TankBlockItem.getContents(drops.get(0));
		helper.assertTrue(contents.is(Fluids.WATER) && contents.getAmount() == 3000, "Expected 3000 mB water in dropped tank, got " + contents);

		// and placing it again restores the contents
		helper.setBlock(pos, OBBlocks.TANK.get());
		final TankBlockEntity placed = helper.getBlockEntity(pos);
		placed.applyComponentsFromItemStack(drops.get(0));
		helper.assertTrue(placed.getFluid().getAmount() == 3000, "Expected 3000 mB after placing, got " + placed.getFluid().getAmount());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void filledTankItemAcceptsFluid(GameTestHelper helper) {
		final ItemStack stack = TankBlock.filledTank(OBBlocks.TANK.get(), water(1));
		helper.assertTrue(TankBlockItem.getContents(stack).getAmount() == TankBlockEntity.getTankCapacity(), "Expected full tank item");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void emptyHandDrainsXp(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 1, 1);
		final TankBlockEntity tank = placeTank(helper, pos);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(100), IFluidHandler.FluidAction.EXECUTE);

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), false);
			helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);

			// level 0 needs 7 XP to reach level 1
			helper.assertTrue(XpUtils.getPlayerXp(player) == 7, "Expected 7 XP, got " + XpUtils.getPlayerXp(player));
			helper.assertTrue(tank.getFluid().getAmount() == FluidXpUtils.xpToFluid(93), "Expected 93 XP left in tank, got " + tank.getFluid().getAmount());
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void bucketFillsTank(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 1, 1);
		final TankBlockEntity tank = placeTank(helper, pos);

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), false);
			helper.getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);

			helper.assertTrue(tank.getFluid().getAmount() == FluidType.BUCKET_VOLUME, "Expected one bucket in tank, got " + tank.getFluid().getAmount());
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sneakClickShowsContentsInsteadOfDrainingXp(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 1, 1);
		final TankBlockEntity tank = placeTank(helper, pos);
		tank.getFluidHandler().fill(FluidXpUtils.xpJuice(100), IFluidHandler.FluidAction.EXECUTE);

		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			player.setShiftKeyDown(true);
			final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), false);
			helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);

			helper.assertTrue(XpUtils.getPlayerXp(player) == 0, "Sneak-click must not drain XP, got " + XpUtils.getPlayerXp(player));
			helper.assertTrue(tank.getFluid().getAmount() == FluidXpUtils.xpToFluid(100), "Tank must be unchanged, got " + tank.getFluid().getAmount());
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void connectedTanksHideSharedEdges(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(0, 1, 1);
		placeTank(helper, pos);
		placeTank(helper, pos.east());

		final int edges = TankEdge.getVisibleEdges(helper.getLevel(), helper.absolutePos(pos));
		helper.assertTrue((edges & (1 << TankEdge.NE.ordinal())) == 0, "Edge towards connected tank must be hidden");
		helper.assertTrue((edges & (1 << TankEdge.NW.ordinal())) != 0, "Outer edge must be visible");
		helper.succeed();
	}
}
