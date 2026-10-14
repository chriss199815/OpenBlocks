package openblocks.item;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import openblocks.block.CanvasBlock;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code ItemSqueegee}: wipes paint and stencils off the clicked side of a canvas, or all sides when sneaking.
 * Once all paint is gone, the canvas turns back into the block it was.
 */
public class SqueegeeItem extends Item {

	public SqueegeeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Level level = context.getLevel();
		if (!(level.getBlockEntity(context.getClickedPos()) instanceof CanvasBlockEntity canvas)) return InteractionResult.FAIL;

		if (!level.isClientSide) {
			final Player player = context.getPlayer();
			final Direction[] sides = player != null && player.isSecondaryUseActive()? Direction.values() : new Direction[] { context.getClickedFace() };
			for (ItemStack stencil : canvas.removePaint(sides))
				Block.popResourceFromFace(level, context.getClickedPos(), context.getClickedFace(), stencil);
			CanvasBlock.restoreIfUnpainted(level, context.getClickedPos(), canvas);
			level.playSound(null, context.getClickedPos(), OBSounds.SQUEEGEE_USE.get(), SoundSource.PLAYERS, 1, 1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
