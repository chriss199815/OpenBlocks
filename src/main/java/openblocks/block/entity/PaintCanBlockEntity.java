package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.item.Paint;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBComponents;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code TileEntityPaintCan}: holds the paint of a placed can; the item keeps it in {@link OBComponents#PAINT}.
 */
public class PaintCanBlockEntity extends BlockEntity {

	private static final String TAG_COLOR = "color";
	private static final String TAG_AMOUNT = "amount";

	private int color = PaintColors.WHITE;
	private int amount;

	public PaintCanBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.PAINT_CAN.get(), pos, state);
	}

	public int getColor() {
		return color;
	}

	public int getAmount() {
		return amount;
	}

	public void setPaint(Paint paint) {
		color = paint.color();
		amount = paint.amount();
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		color = tag.getInt(TAG_COLOR);
		amount = tag.getInt(TAG_AMOUNT);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt(TAG_COLOR, color);
		tag.putInt(TAG_AMOUNT, amount);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput componentInput) {
		super.applyImplicitComponents(componentInput);
		final Paint paint = componentInput.get(OBComponents.PAINT);
		if (paint != null) {
			color = paint.color();
			amount = paint.amount();
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(OBComponents.PAINT, new Paint(color, amount));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove(TAG_COLOR);
		tag.remove(TAG_AMOUNT);
	}

	// the color tints the model
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
