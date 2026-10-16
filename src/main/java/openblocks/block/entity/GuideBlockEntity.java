package openblocks.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import openblocks.OBConfig;
import openblocks.block.GuideBlock;
import openblocks.common.GuideButtons;
import openblocks.common.GuideOrientation;
import openblocks.common.GuideRedstoneMode;
import openblocks.common.GuideSettings;
import openblocks.menu.GuideMenu;
import openblocks.network.GuideAnimationPayload;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBComponents;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code TileEntityGuide}: keeps size, shape and color, and builds the list of marker positions from
 * them. The client builds the same list from the synced settings; when it changes, the old shape fades out while the
 * new one fades in (legacy grew and shrank the markers).
 */
public class GuideBlockEntity extends BlockEntity implements MenuProvider {

	private static final String TAG_SETTINGS = "settings";
	private static final String TAG_POWERED = "powered";

	// legacy sorted by x first, while the comment (and the placement order of the builder guide) asked for bottom to top
	private static final Comparator<BlockPos> ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getY)
			// then by angle, to make placement more intuitive
			.thenComparingDouble(p -> Math.atan2(p.getZ(), p.getX()))
			// then far ones first
			.thenComparing(Comparator.<BlockPos>comparingDouble(p -> (double)p.getX() * p.getX() + (double)p.getZ() * p.getZ()).reversed())
			.thenComparingInt(BlockPos::getX)
			.thenComparingInt(BlockPos::getZ);

	public static final int FADE_TICKS = 10;

	private GuideSettings settings = defaultSettings();

	private boolean powered;

	// marker positions relative to the guide, already rotated
	@Nullable
	private List<BlockPos> shape;

	@Nullable
	private List<BlockPos> previousShape;

	private long shapeChangeTime = Long.MIN_VALUE;

	@Nullable
	private AABB renderBox;

	// GPU buffers of GuideRenderer, freed with the block entity
	@Nullable
	private AutoCloseable renderData;

	public GuideBlockEntity(BlockPos pos, BlockState state) {
		this(OBBlockEntities.GUIDE.get(), pos, state);
	}

	protected GuideBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Settings of a new guide; the redstone mode comes from {@code guide.redstoneSensitivity}.
	 */
	public static GuideSettings defaultSettings() {
		return GuideSettings.DEFAULT.withRedstoneMode(GuideRedstoneMode.fromSensitivity(OBConfig.GUIDE.redstoneSensitivity.getAsInt()));
	}

	public GuideSettings getSettings() {
		return settings;
	}

	public void setSettings(GuideSettings settings) {
		if (settings.equals(this.settings)) return;
		final boolean shapeChanged = !settings.sameShape(this.settings);
		this.settings = settings;
		if (shapeChanged) onShapeChanged();
		setChanged();
		sync();
	}

	/**
	 * Whether the shape is shown (and the builder guide places blocks); legacy {@code shouldRender}, decided by
	 * redstone and the redstone mode of the guide.
	 */
	public boolean isEnabled() {
		return settings.redstoneMode().isEnabled(powered);
	}

	public void updateRedstone() {
		if (level == null || level.isClientSide) return;
		final boolean newPowered = level.hasNeighborSignal(worldPosition);
		if (newPowered != powered) {
			powered = newPowered;
			setChanged();
			sync();
		}
	}

	public GuideOrientation getOrientation() {
		return getBlockState().getValue(GuideBlock.ORIENTATION);
	}

	public List<BlockPos> getShape() {
		if (shape == null) shape = generateShape();
		return shape;
	}

	@Nullable
	public List<BlockPos> getPreviousShape() {
		return previousShape;
	}

	/**
	 * Ticks since the shape last changed on the client, for the fade.
	 */
	public float getTimeSinceChange(float partialTick) {
		if (level == null || shapeChangeTime == Long.MIN_VALUE) return FADE_TICKS;
		return Math.min(FADE_TICKS, level.getGameTime() - shapeChangeTime + partialTick);
	}

	@Nullable
	public AutoCloseable getRenderData() {
		return renderData;
	}

	public void setRenderData(@Nullable AutoCloseable renderData) {
		this.renderData = renderData;
	}

	private void onShapeChanged() {
		// only the client fades
		previousShape = level != null && level.isClientSide? shape : null;
		shape = null;
		renderBox = null;
		shapeChangeTime = level != null? level.getGameTime() : Long.MIN_VALUE;
	}

	private List<BlockPos> generateShape() {
		final Set<BlockPos> unique = new HashSet<>();
		settings.shape().generator().generate(-settings.negX(), -settings.negY(), -settings.negZ(), settings.posX(), settings.posY(), settings.posZ(), (x, y, z) -> {
			if (canAddCoord(x, y, z)) unique.add(new BlockPos(x, y, z));
		});

		final List<BlockPos> sorted = new ArrayList<>(unique);
		sorted.sort(ORDER);

		final GuideOrientation orientation = getOrientation();
		return sorted.stream().map(p -> orientation.toWorld(p.getX(), p.getY(), p.getZ())).toList();
	}

	protected boolean canAddCoord(int x, int y, int z) {
		return x != 0 || y != 0 || z != 0;
	}

	public AABB getRenderBox() {
		if (renderBox == null) {
			int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
			for (BlockPos p : getShape()) {
				minX = Math.min(minX, p.getX());
				minY = Math.min(minY, p.getY());
				minZ = Math.min(minZ, p.getZ());
				maxX = Math.max(maxX, p.getX());
				maxY = Math.max(maxY, p.getY());
				maxZ = Math.max(maxZ, p.getZ());
			}
			renderBox = new AABB(worldPosition.offset(minX, minY, minZ).getCenter(), worldPosition.offset(maxX, maxY, maxZ).getCenter()).inflate(0.5);
		}
		return renderBox;
	}

	public void press(ServerPlayer player, GuideButtons.Button button) {
		switch (button.action()) {
			case INCREMENT -> changeSize(player, settings.withSize(button.halfAxis(), settings.size(button.halfAxis()) + 1));
			case DECREMENT -> {
				final int size = settings.size(button.halfAxis());
				if (size > 0) changeSize(player, settings.withSize(button.halfAxis(), size - 1));
			}
			case COPY -> changeSize(player, settings.withSize(button.halfAxis().getOpposite(), settings.size(button.halfAxis())));
			case ROTATE_CW -> rotateClockwise();
			case ROTATE_CCW -> rotateCounterClockwise();
			case NEXT_SHAPE -> changeShape(player, settings.withShape(settings.shape().next()));
			case PREVIOUS_SHAPE -> changeShape(player, settings.withShape(settings.shape().previous()));
		}
	}

	private void changeSize(ServerPlayer player, GuideSettings newSettings) {
		setSettings(newSettings);
		player.displayClientMessage(Component.translatable("openblocks.misc.change_box_size", settings.boxArgs())
				.append(", ").append(blockCount()), true);
	}

	private void changeShape(ServerPlayer player, GuideSettings newSettings) {
		setSettings(newSettings);
		player.displayClientMessage(Component.translatable("openblocks.misc.change_mode", settings.shape().displayName())
				.append(", ").append(blockCount()), true);
	}

	private Component blockCount() {
		return Component.translatable("openblocks.misc.total_blocks", getShape().size());
	}

	/**
	 * Changes a size from the GUI; unlike the touch buttons without a message.
	 */
	public void adjustSize(Direction halfAxis, int delta) {
		setSettings(settings.withSize(halfAxis, settings.size(halfAxis) + delta));
	}

	public void rotateClockwise() {
		rotate(getOrientation().rotateClockwise());
	}

	public void rotateCounterClockwise() {
		rotate(getOrientation().rotateCounterClockwise());
	}

	private void rotate(GuideOrientation orientation) {
		if (level != null) level.setBlock(worldPosition, getBlockState().setValue(GuideBlock.ORIENTATION, orientation), Block.UPDATE_ALL);
	}

	/**
	 * Whether a click with this item does something; checked on both sides, so the client swings the arm.
	 */
	public boolean canUseItem(ItemStack stack) {
		return DyeColor.getColor(stack) != null;
	}

	public void useItem(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
		final DyeColor dye = DyeColor.getColor(stack);
		if (dye != null) setSettings(settings.withColor(PaintColors.rgb(dye)));
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new GuideMenu(containerId, this);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setBlockState(BlockState state) {
		final boolean rotated = !state.equals(getBlockState());
		super.setBlockState(state);
		if (rotated) onShapeChanged();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (renderData != null) {
			try {
				renderData.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			renderData = null;
		}
	}

	private void sync() {
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		final GuideSettings loaded = GuideSettings.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_SETTINGS)).result().orElse(GuideSettings.DEFAULT);
		if (!loaded.sameShape(settings)) onShapeChanged();
		settings = loaded;
		powered = tag.getBoolean(TAG_POWERED);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		GuideSettings.CODEC.encodeStart(NbtOps.INSTANCE, settings).ifSuccess(t -> tag.put(TAG_SETTINGS, t));
		tag.putBoolean(TAG_POWERED, powered);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput componentInput) {
		super.applyImplicitComponents(componentInput);
		settings = componentInput.getOrDefault(OBComponents.GUIDE, defaultSettings());
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(OBComponents.GUIDE, settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove(TAG_SETTINGS);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * Port of legacy {@code TileEntityBuilderGuide}: while enabled, a click with a block places it at the next free
	 * position of the shape, like the player would. In creative mode with obsidian above the guide, the whole shape is
	 * filled at once. Keeps the 3×3×3 space around the guide free, so it stays reachable.
	 */
	public static class Builder extends GuideBlockEntity {

		public Builder(BlockPos pos, BlockState state) {
			super(OBBlockEntities.BUILDER_GUIDE.get(), pos, state);
		}

		@Override
		protected boolean canAddCoord(int x, int y, int z) {
			return Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1;
		}

		// legacy needed redstone power here, even with redstoneSensitivity 0 (then it never placed anything)
		@Override
		public boolean canUseItem(ItemStack stack) {
			return (isEnabled() && stack.getItem() instanceof BlockItem) || super.canUseItem(stack);
		}

		@Override
		public void useItem(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
			if (isEnabled() && stack.getItem() instanceof BlockItem blockItem) {
				if (player.isCreative() && isInFillMode()) fill(player, stack, blockItem, hit);
				else placeNext(player, stack, hit);
			} else {
				super.useItem(player, stack, hit);
			}
		}

		private boolean isInFillMode() {
			return level != null && level.getBlockState(worldPosition.above()).is(Blocks.OBSIDIAN);
		}

		private void fill(ServerPlayer player, ItemStack stack, BlockItem blockItem, BlockHitResult hit) {
			for (BlockPos offset : getShape()) {
				final BlockPos target = worldPosition.offset(offset);
				if (!level.isLoaded(target) || level.isOutsideBuildHeight(target)) continue;
				final BlockState state = blockItem.getBlock().getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hitAt(target, hit)));
				if (state != null) level.setBlock(target, state, Block.UPDATE_ALL);
			}
		}

		private void placeNext(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
			for (BlockPos offset : getShape()) {
				final BlockPos target = worldPosition.offset(offset);
				if (level.isLoaded(target) && level.isEmptyBlock(target) && !level.isOutsideBuildHeight(target)) {
					final InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitAt(target, hit));
					if (result.consumesAction()) {
						PacketDistributor.sendToPlayersTrackingChunk((ServerLevel)level, new ChunkPos(worldPosition),
								new GuideAnimationPayload(worldPosition, target, Block.getId(level.getBlockState(target))));
						return;
					}
				}
			}
		}

		// like the click on the guide, moved to the target
		private BlockHitResult hitAt(BlockPos target, BlockHitResult hit) {
			final Vec3 offset = hit.getLocation().subtract(Vec3.atLowerCornerOf(worldPosition));
			return new BlockHitResult(Vec3.atLowerCornerOf(target).add(offset), hit.getDirection(), target, false);
		}
	}
}
