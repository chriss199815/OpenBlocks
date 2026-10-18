package openblocks.item;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import openblocks.block.ImaginaryBlock;
import openblocks.block.entity.ImaginaryBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code ItemImaginary}; the pencil and the crayon (legacy item damage 0 and 1) are separate items.
 * Each drawn block costs the uses of its {@link ImaginaryPlacementMode}; crafting with it (e.g. glasses) costs one use.
 * Not a {@code BlockItem}, as both items place the same block.
 */
public class ImaginaryItem extends Item {

	public static final float DEFAULT_USES = 10;
	public static final float CRAFTING_COST = 1;

	private final boolean crayon;

	public ImaginaryItem(boolean crayon, Properties properties) {
		super(properties.component(OBComponents.IMAGINARY_USES, DEFAULT_USES));
		this.crayon = crayon;
	}

	/**
	 * A crayon of the given color, or a pencil without one.
	 */
	public static ItemStack create(@Nullable Integer color, ImaginaryPlacementMode mode, float uses) {
		final ItemStack stack = new ItemStack(color != null? OBItems.CRAYON.get() : OBItems.PENCIL.get());
		if (color != null) stack.set(OBComponents.CRAYON_COLOR, color);
		stack.set(OBComponents.IMAGINARY_MODE, mode);
		stack.set(OBComponents.IMAGINARY_USES, uses);
		return stack;
	}

	public static float getUses(ItemStack stack) {
		return stack.getOrDefault(OBComponents.IMAGINARY_USES, 0f);
	}

	public static ImaginaryPlacementMode getMode(ItemStack stack) {
		return stack.getOrDefault(OBComponents.IMAGINARY_MODE, ImaginaryPlacementMode.BLOCK);
	}

	/**
	 * The crayon color (white if missing), or {@code null} for pencils.
	 */
	@Nullable
	public static Integer getColor(ItemStack stack) {
		if (!isCrayon(stack)) return null;
		return stack.getOrDefault(OBComponents.CRAYON_COLOR, PaintColors.WHITE);
	}

	public static boolean isCrayon(ItemStack stack) {
		return stack.getItem() instanceof ImaginaryItem item && item.crayon;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Player player = context.getPlayer();
		final ItemStack stack = context.getItemInHand();
		// unlike legacy (only when clicking into the air), sneaking always switches the mode, also when looking at a block
		if (player != null && player.isSecondaryUseActive()) return cycleMode(context.getLevel(), player, stack);

		final float uses = getUses(stack);
		final ImaginaryPlacementMode mode = getMode(stack);
		if (uses < mode.cost) return InteractionResult.FAIL;

		final BlockPlaceContext placeContext = new BlockPlaceContext(context);
		if (!placeContext.canPlace()) return InteractionResult.FAIL;

		final Level level = context.getLevel();
		final BlockPos pos = placeContext.getClickedPos();
		final BlockState state = OBBlocks.IMAGINARY.get().defaultBlockState()
				.setValue(ImaginaryBlock.FACING, placeContext.getHorizontalDirection())
				.setValue(ImaginaryBlock.SHAPE, mode.shape)
				.setValue(ImaginaryBlock.TYPE, crayon? ImaginaryBlock.Type.CRAYON : ImaginaryBlock.Type.PENCIL);
		if (level.isClientSide) return InteractionResult.SUCCESS;

		if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) return InteractionResult.FAIL;
		if (level.getBlockEntity(pos) instanceof ImaginaryBlockEntity imaginary) imaginary.setup(getColor(stack), mode.inverted);

		final SoundType sound = state.getSoundType(level, pos, player);
		level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));

		if (player == null || !player.getAbilities().instabuild) {
			final float usesLeft = Math.max(uses - mode.cost, 0);
			if (usesLeft <= 0) stack.shrink(1);
			else stack.set(OBComponents.IMAGINARY_USES, usesLeft);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
		if (player.isSecondaryUseActive()) return new InteractionResultHolder<>(cycleMode(level, player, stack), stack);
		return InteractionResultHolder.pass(stack);
	}

	private static InteractionResult cycleMode(Level level, Player player, ItemStack stack) {
		if (!level.isClientSide) {
			final ImaginaryPlacementMode mode = getMode(stack).next();
			stack.set(OBComponents.IMAGINARY_MODE, mode);
			// unlike legacy in the action bar instead of the chat
			player.displayClientMessage(Component.translatable("openblocks.misc.mode", mode.displayName()), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public boolean hasCraftingRemainingItem(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack stack) {
		final float uses = getUses(stack) - CRAFTING_COST;
		if (uses <= 0) return ItemStack.EMPTY;
		final ItemStack result = stack.copyWithCount(1);
		result.set(OBComponents.IMAGINARY_USES, uses);
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("openblocks.misc.uses", String.format(Locale.ROOT, "%.2f", getUses(stack))).withStyle(ChatFormatting.GRAY));
		final Integer color = getColor(stack);
		if (color != null) tooltip.add(Component.translatable("openblocks.misc.color", String.format("%06X", color)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("openblocks.misc.mode", getMode(stack).displayName()).withStyle(ChatFormatting.GRAY));
	}
}
