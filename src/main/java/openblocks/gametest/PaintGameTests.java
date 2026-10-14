package openblocks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.PaintCanBlockEntity;
import openblocks.block.entity.PaintMixerBlockEntity;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.item.Paint;
import openblocks.item.PaintBrushItem;
import openblocks.item.PaintCanItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.util.PaintColors;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class PaintGameTests {

	private static final BlockPos POS = new BlockPos(1, 2, 1);

	private static Player playerWith(GameTestHelper helper, ItemStack stack) {
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static void useOn(GameTestHelper helper, Player player, BlockPos pos, Direction side) {
		final BlockPos absolute = helper.absolutePos(pos);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), side, absolute, false)));
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void brushTakesPaintFromCan(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.PAINT_CAN.get());
		final PaintCanBlockEntity can = helper.getBlockEntity(POS);
		can.setPaint(new Paint(0x123456, 2));

		final Player player = playerWith(helper, new ItemStack(OBItems.PAINT_BRUSH.get()));
		helper.useBlock(POS, player);
		final Paint paint = player.getMainHandItem().get(OBComponents.PAINT);
		helper.assertTrue(paint != null && paint.color() == 0x123456 && paint.amount() == PaintBrushItem.MAX_USES, "Expected a full brush with the can color, got " + paint);
		helper.assertTrue(can.getAmount() == 1, "Expected 1 portion left, got " + can.getAmount());

		// the last portion leaves a bucket behind
		helper.useBlock(POS, player);
		helper.assertBlockNotPresent(OBBlocks.PAINT_CAN.get(), POS);
		helper.assertItemEntityPresent(Items.BUCKET, POS, 1);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void brushRecolorsDyedBlocks(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.WHITE_WOOL);
		final Player player = playerWith(helper, PaintBrushItem.create(DyeColor.RED));
		useOn(helper, player, POS, Direction.UP);
		helper.assertBlockPresent(Blocks.RED_WOOL, POS);
		final Paint paint = player.getMainHandItem().get(OBComponents.PAINT);
		helper.assertTrue(paint != null && paint.amount() == PaintBrushItem.MAX_USES - 1, "Expected one use spent, got " + paint);

		// any color uses the closest dye color
		player.setItemInHand(InteractionHand.MAIN_HAND, PaintBrushItem.create(0x000000));
		useOn(helper, player, POS, Direction.UP);
		helper.assertBlockPresent(Blocks.BLACK_WOOL, POS);
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3, timeoutTicks = PaintMixerBlockEntity.PROGRESS_TICKS + 40)
	public static void mixerTurnsMilkIntoPaint(GameTestHelper helper) {
		helper.setBlock(POS, OBBlocks.PAINT_MIXER.get());
		final PaintMixerBlockEntity mixer = helper.getBlockEntity(POS);
		mixer.getInventory().setStackInSlot(PaintMixerBlockEntity.SLOT_PAINT, new ItemStack(Items.MILK_BUCKET));
		mixer.getInventory().setStackInSlot(PaintMixerBlockEntity.SLOT_MAGENTA, new ItemStack(Items.MAGENTA_DYE, 2));
		mixer.getInventory().setStackInSlot(PaintMixerBlockEntity.SLOT_YELLOW, new ItemStack(Items.YELLOW_DYE, 2));
		mixer.startMixing(0xFF0000);
		helper.assertTrue(mixer.isWorking(), "Expected the mixer to start");

		helper.succeedWhen(() -> {
			final ItemStack result = mixer.getInventory().getStackInSlot(PaintMixerBlockEntity.SLOT_PAINT);
			helper.assertTrue(result.is(OBItems.PAINT_CAN.get()), "Expected a paint can, got " + result);
			helper.assertTrue(PaintCanItem.getColor(result) == 0xFF0000, "Expected red paint, got " + PaintColors.hex(PaintCanItem.getColor(result)));
			// red needs one magenta and one yellow; one of each was used up
			helper.assertTrue(mixer.getInventory().getStackInSlot(PaintMixerBlockEntity.SLOT_MAGENTA).getCount() <= 1, "Expected magenta dye to be used");
		});
	}
}
