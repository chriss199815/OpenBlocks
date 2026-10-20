package openblocks.integration.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Peripheral of one block entity; methods are {@code @LuaFunction(mainThread = true)}, since they touch the world.
 */
abstract class BlockEntityPeripheral<T extends BlockEntity> implements IPeripheral {

	protected final T blockEntity;
	private final String type;

	BlockEntityPeripheral(T blockEntity, String type) {
		this.blockEntity = blockEntity;
		this.type = type;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Object getTarget() {
		return blockEntity;
	}

	@Override
	public boolean equals(@Nullable IPeripheral other) {
		return other instanceof BlockEntityPeripheral<?> peripheral && peripheral.blockEntity == blockEntity;
	}
}
