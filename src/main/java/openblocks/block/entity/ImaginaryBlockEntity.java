package openblocks.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.item.ImaginationGlassesItem;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityImaginary}: the crayon color (none for pencil blocks) and whether the block is
 * inverted. Shape and facing are in the block state.
 */
public class ImaginaryBlockEntity extends BlockEntity {

	private static final String TAG_COLOR = "Color";
	private static final String TAG_INVERTED = "IsInverted";

	public enum Property {
		VISIBLE,
		SELECTABLE,
		SOLID
	}

	@Nullable
	private Integer color;
	private boolean inverted;

	// client only, see ImaginaryRenderer
	public float visibility;
	public long lastVisibilityUpdate;

	public ImaginaryBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.IMAGINARY.get(), pos, state);
	}

	public void setup(@Nullable Integer color, boolean inverted) {
		this.color = color;
		this.inverted = inverted;
		setChanged();
	}

	@Nullable
	public Integer getColor() {
		return color;
	}

	public boolean isPencil() {
		return color == null;
	}

	public boolean isInverted() {
		return inverted;
	}

	/**
	 * Like legacy: spectators see everything, pencil blocks are solid for every player; otherwise the glasses decide,
	 * without glasses only inverted blocks are there.
	 */
	public boolean is(Property property, Player player) {
		if (property == Property.VISIBLE && player.isSpectator()) return true;
		if (property == Property.SOLID && isPencil()) return true;

		final ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		if (helmet.getItem() instanceof ImaginationGlassesItem glasses) return glasses.checkBlock(property, helmet, this);
		return inverted;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (color != null) tag.putInt(TAG_COLOR, color);
		tag.putBoolean(TAG_INVERTED, inverted);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		color = tag.contains(TAG_COLOR)? tag.getInt(TAG_COLOR) : null;
		inverted = tag.getBoolean(TAG_INVERTED);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
