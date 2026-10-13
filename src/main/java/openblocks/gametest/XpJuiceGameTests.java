package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.common.FluidXpUtils;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBFluids;
import openblocks.registry.OBItems;
import openblocks.util.XpUtils;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class XpJuiceGameTests {

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void bucketContainsXpJuice(GameTestHelper helper) {
		final FluidStack contents = FluidUtil.getFluidContained(new ItemStack(OBItems.XP_BUCKET.get())).orElse(FluidStack.EMPTY);
		helper.assertTrue(contents.is(OBFluids.XP_JUICE.get()), "Expected xp juice in bucket, got " + contents);
		helper.assertTrue(contents.getAmount() == FluidType.BUCKET_VOLUME, "Expected full bucket, got " + contents.getAmount());
		helper.assertTrue(FluidXpUtils.isXpFluid(contents), "xp juice must be in c:experience");
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void xpConversionRoundTrips(GameTestHelper helper) {
		final FluidStack juice = FluidXpUtils.xpJuice(50);
		helper.assertTrue(FluidXpUtils.fluidToXp(juice.getAmount()) == 50, "Expected 50 XP back, got " + FluidXpUtils.fluidToXp(juice.getAmount()));
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void bucketUsedInAirGivesXp(GameTestHelper helper) {
		final ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			final Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
			player.teleportTo(pos.x, pos.y, pos.z);
			// looking straight up, nothing in reach
			player.setXRot(-90);
			// mock player is creative, which would keep the full bucket
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.XP_BUCKET.get()));

			// like ServerPlayerGameMode.useItem, the returned stack replaces the held one
			player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject());

			final int expectedXp = FluidXpUtils.fluidToXp(FluidType.BUCKET_VOLUME);
			helper.assertTrue(XpUtils.getPlayerXp(player) == expectedXp, "Expected " + expectedXp + " XP, got " + XpUtils.getPlayerXp(player));
			helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Expected empty bucket, got " + player.getMainHandItem());
			helper.succeed();
		} finally {
			player.getServer().getPlayerList().remove(player);
		}
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void sourceBlockIsPlaceable(GameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, OBBlocks.XP_JUICE.get());
		helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).isSourceOfType(OBFluids.XP_JUICE.get()), "Expected xp juice source block");
		helper.succeed();
	}
}
