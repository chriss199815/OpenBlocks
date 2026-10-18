package openblocks.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import openblocks.item.GlyphItem;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code EntityGlyph}: a glyph hanging on a wall. Eight by eight pixels, its center at the given pixel of
 * the wall face (counted from the bottom left corner as seen from the front), so it may stick out of the block. Like
 * paintings, it needs a wall behind it and no other hanging entity in its place.
 */
public class Glyph extends HangingEntity {

	private static final EntityDataAccessor<Integer> DATA_CHAR = SynchedEntityData.defineId(Glyph.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_OFFSET_X = SynchedEntityData.defineId(Glyph.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_OFFSET_Y = SynchedEntityData.defineId(Glyph.class, EntityDataSerializers.BYTE);

	public static final int SIZE_PIXELS = 8;
	private static final double DEPTH = 0.5 / 16;

	private static final String TAG_FACING = "Facing";
	private static final String TAG_CHAR = "Char";
	private static final String TAG_OFFSET_X = "OffsetX";
	private static final String TAG_OFFSET_Y = "OffsetY";

	public Glyph(EntityType<? extends Glyph> type, Level level) {
		super(type, level);
	}

	public Glyph(Level level, BlockPos pos, Direction facing, char ch, int offsetX, int offsetY) {
		super(OBEntities.GLYPH.get(), level, pos);
		entityData.set(DATA_CHAR, (int)ch);
		entityData.set(DATA_OFFSET_X, (byte)offsetX);
		entityData.set(DATA_OFFSET_Y, (byte)offsetY);
		setDirection(facing);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_CHAR, (int)GlyphItem.DEFAULT_CHAR);
		builder.define(DATA_OFFSET_X, (byte)8);
		builder.define(DATA_OFFSET_Y, (byte)8);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (key == DATA_OFFSET_X || key == DATA_OFFSET_Y) recalculateBoundingBox();
	}

	public char getChar() {
		final int ch = entityData.get(DATA_CHAR);
		return GlyphItem.isValid(ch)? (char)ch : GlyphItem.DEFAULT_CHAR;
	}

	@Override
	protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
		final Direction right = direction.getCounterClockWise();
		final double u = entityData.get(DATA_OFFSET_X) / 16.0 - 0.5;
		final double v = entityData.get(DATA_OFFSET_Y) / 16.0 - 0.5;
		// against the wall, which is half a block behind the center of pos
		final Vec3 center = Vec3.atCenterOf(pos)
				.relative(direction, DEPTH / 2 - 0.5)
				.relative(right, u)
				.add(0, v, 0);
		final double half = SIZE_PIXELS / 16.0 / 2;
		final double halfX = direction.getAxis() == Direction.Axis.X? DEPTH / 2 : half;
		final double halfZ = direction.getAxis() == Direction.Axis.Z? DEPTH / 2 : half;
		return new AABB(center.x - halfX, center.y - half, center.z - halfZ, center.x + halfX, center.y + half, center.z + halfZ);
	}

	public ItemStack getStack() {
		return GlyphItem.create(getChar(), 1);
	}

	@Override
	public void dropItem(@Nullable Entity breaker) {
		if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return;
		playSound(SoundEvents.PAINTING_BREAK, 1, 1);
		if (breaker instanceof Player player && player.hasInfiniteMaterials()) return;
		spawnAtLocation(getStack());
	}

	@Override
	public void playPlacementSound() {
		playSound(SoundEvents.PAINTING_PLACE, 1, 1);
	}

	@Override
	public ItemStack getPickResult() {
		return getStack();
	}

	@Override
	public void moveTo(double x, double y, double z, float yaw, float pitch) {
		setPos(x, y, z);
	}

	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
		setPos(x, y, z);
	}

	@Override
	public Vec3 trackingPosition() {
		return Vec3.atLowerCornerOf(pos);
	}

	// like paintings, the position is the block in front of the wall, which the center may be outside of
	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
		return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), getPos());
	}

	@Override
	public void recreateFromPacket(ClientboundAddEntityPacket packet) {
		super.recreateFromPacket(packet);
		setDirection(Direction.from3DDataValue(packet.getData()));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte(TAG_FACING, (byte)direction.get2DDataValue());
		tag.putInt(TAG_CHAR, getChar());
		tag.putByte(TAG_OFFSET_X, entityData.get(DATA_OFFSET_X));
		tag.putByte(TAG_OFFSET_Y, entityData.get(DATA_OFFSET_Y));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		entityData.set(DATA_CHAR, tag.getInt(TAG_CHAR));
		entityData.set(DATA_OFFSET_X, tag.getByte(TAG_OFFSET_X));
		entityData.set(DATA_OFFSET_Y, tag.getByte(TAG_OFFSET_Y));
		super.readAdditionalSaveData(tag);
		setDirection(Direction.from2DDataValue(tag.getByte(TAG_FACING)));
	}
}
