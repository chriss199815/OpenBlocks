package openblocks.block.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import openblocks.common.CanvasSide;
import openblocks.item.StencilItem;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code TileEntityCanvas}: the block that was turned into this canvas (none for placed canvas blocks), and the paint on each side.
 */
public class CanvasBlockEntity extends BlockEntity {

	public static final ModelProperty<BlockState> PAINTED_BLOCK = new ModelProperty<>();

	@Nullable
	private BlockState paintedBlock;

	private final Map<Direction, CanvasSide> sides = new EnumMap<>(Direction.class);

	public CanvasBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.CANVAS.get(), pos, state);
	}

	@Nullable
	public BlockState getPaintedBlock() {
		return paintedBlock;
	}

	public void setPaintedBlock(@Nullable BlockState state) {
		paintedBlock = state;
		changed();
	}

	public CanvasSide getSide(Direction side) {
		return sides.getOrDefault(side, CanvasSide.EMPTY);
	}

	private void setSide(Direction side, CanvasSide value) {
		if (value.isEmpty()) sides.remove(side);
		else sides.put(side, value);
		changed();
	}

	public boolean applyPaint(Direction side, int color) {
		final CanvasSide before = getSide(side);
		final CanvasSide after = before.paint(color);
		if (after.equals(before)) return false;
		setSide(side, after);
		return true;
	}

	public boolean putStencil(Direction side, ItemStack stencil) {
		if (!(stencil.getItem() instanceof StencilItem item)) return false;
		final CanvasSide after = getSide(side).putStencil(item.getPattern());
		if (after == null) return false;
		setSide(side, after);
		return true;
	}

	public boolean rotateStencil(Direction side) {
		final CanvasSide after = getSide(side).rotateStencil();
		if (after == null) return false;
		setSide(side, after);
		return true;
	}

	/**
	 * Takes the stencil off the side.
	 */
	public ItemStack removeStencil(Direction side) {
		final CanvasSide before = getSide(side);
		if (before.cover().isEmpty()) return ItemStack.EMPTY;
		setSide(side, before.removeStencil());
		return new ItemStack(OBItems.STENCILS.get(before.cover().get().pattern()).get());
	}

	/**
	 * Wipes the paint and stencils off the given sides; returns the stencils.
	 */
	public List<ItemStack> removePaint(Direction... sidesToClear) {
		final List<ItemStack> stencils = new ArrayList<>();
		for (Direction side : sidesToClear) {
			stencils.add(removeStencil(side));
			setSide(side, CanvasSide.EMPTY);
		}
		stencils.removeIf(ItemStack::isEmpty);
		return stencils;
	}

	public boolean isUnpainted() {
		return sides.values().stream().noneMatch(CanvasSide::hasPaint);
	}

	public List<ItemStack> getStencils() {
		final List<ItemStack> result = new ArrayList<>();
		for (CanvasSide side : sides.values())
			side.cover().ifPresent(cover -> result.add(new ItemStack(OBItems.STENCILS.get(cover.pattern()).get())));
		return result;
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public ModelData getModelData() {
		return paintedBlock != null? ModelData.builder().with(PAINTED_BLOCK, paintedBlock).build() : ModelData.EMPTY;
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		final BlockState previous = paintedBlock;
		paintedBlock = tag.contains("painted_block")? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("painted_block")) : null;
		sides.clear();
		final CompoundTag sidesTag = tag.getCompound("sides");
		for (Direction side : Direction.values())
			if (sidesTag.contains(side.getSerializedName()))
				CanvasSide.CODEC.parse(NbtOps.INSTANCE, sidesTag.get(side.getSerializedName())).result().ifPresent(value -> sides.put(side, value));

		// the chunk shows the painted block, so it has to be rebuilt when it changes
		if (level != null && level.isClientSide && previous != paintedBlock) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (paintedBlock != null) tag.put("painted_block", NbtUtils.writeBlockState(paintedBlock));
		final CompoundTag sidesTag = new CompoundTag();
		sides.forEach((side, value) -> CanvasSide.CODEC.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> sidesTag.put(side.getSerializedName(), encoded)));
		tag.put("sides", sidesTag);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		loadAdditional(packet.getTag(), registries);
	}
}
