package openblocks.integration.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import openblocks.block.entity.GuideBlockEntity;
import openblocks.common.GuideRedstoneMode;
import openblocks.common.GuideSettings;
import openblocks.common.GuideShape;

/**
 * Legacy {@code @ScriptCallable} methods of {@code TileEntityGuide}, for the building guide and the enhanced one. Sizes use
 * the legacy names; shapes and redstone modes are their lowercase names ({@code sphere}, {@code powered}, ...).
 */
class GuidePeripheral extends BlockEntityPeripheral<GuideBlockEntity> {

	private static final Map<String, Direction> SIZE_KEYS = new LinkedHashMap<>();

	static {
		SIZE_KEYS.put("negX", Direction.WEST);
		SIZE_KEYS.put("negY", Direction.DOWN);
		SIZE_KEYS.put("negZ", Direction.NORTH);
		SIZE_KEYS.put("posX", Direction.EAST);
		SIZE_KEYS.put("posY", Direction.UP);
		SIZE_KEYS.put("posZ", Direction.SOUTH);
	}

	GuidePeripheral(GuideBlockEntity guide) {
		super(guide, "openblocks_guide");
	}

	@LuaFunction(mainThread = true)
	public final Map<String, Integer> getSize() {
		final GuideSettings settings = blockEntity.getSettings();
		final Map<String, Integer> result = new LinkedHashMap<>();
		SIZE_KEYS.forEach((key, halfAxis) -> result.put(key, settings.size(halfAxis)));
		return result;
	}

	/**
	 * Unlike legacy, sizes may be left out (they stay as they are) and 0 is allowed, like in the GUI.
	 */
	@LuaFunction(mainThread = true)
	public final void setSize(Map<?, ?> size) throws LuaException {
		GuideSettings settings = blockEntity.getSettings();
		for (Map.Entry<?, ?> entry : size.entrySet()) {
			final Direction halfAxis = SIZE_KEYS.get(String.valueOf(entry.getKey()));
			if (halfAxis == null) throw new LuaException("Unknown size " + entry.getKey() + ", expected one of " + SIZE_KEYS.keySet());
			if (!(entry.getValue() instanceof Number number)) throw new LuaException("Size " + entry.getKey() + " must be a number");
			final int value = number.intValue();
			if (value < 0 || value > GuideSettings.MAX_SIZE) throw new LuaException("Size " + entry.getKey() + " must be between 0 and " + GuideSettings.MAX_SIZE);
			settings = settings.withSize(halfAxis, value);
		}
		blockEntity.setSettings(settings);
	}

	@LuaFunction(mainThread = true)
	public final String getShape() {
		return blockEntity.getSettings().shape().getSerializedName();
	}

	@LuaFunction(mainThread = true)
	public final void setShape(String name) throws LuaException {
		blockEntity.setSettings(blockEntity.getSettings().withShape(byName(GuideShape.values(), name)));
	}

	/**
	 * Legacy name of {@link #getShape()}.
	 */
	@LuaFunction(mainThread = true)
	public final String getCurrentMode() {
		return getShape();
	}

	@LuaFunction(mainThread = true)
	public final int getColor() {
		return blockEntity.getSettings().color();
	}

	@LuaFunction(mainThread = true)
	public final void setColor(int color) throws LuaException {
		if (color < 0 || color > 0xFFFFFF) throw new LuaException("Color must be between 0 and 0xFFFFFF");
		blockEntity.setSettings(blockEntity.getSettings().withColor(color));
	}

	@LuaFunction(mainThread = true)
	public final String getRedstoneMode() {
		return blockEntity.getSettings().redstoneMode().getSerializedName();
	}

	@LuaFunction(mainThread = true)
	public final void setRedstoneMode(String name) throws LuaException {
		blockEntity.setSettings(blockEntity.getSettings().withRedstoneMode(byName(GuideRedstoneMode.values(), name)));
	}

	@LuaFunction(mainThread = true)
	public final int getCount() {
		return blockEntity.getShape().size();
	}

	@LuaFunction(mainThread = true)
	public final void rotate(boolean clockwise) {
		if (clockwise) blockEntity.rotateClockwise();
		else blockEntity.rotateCounterClockwise();
	}

	private static <E extends Enum<E> & StringRepresentable> E byName(E[] values, String name) throws LuaException {
		final String lower = name.toLowerCase(Locale.ROOT);
		for (E value : values)
			if (value.getSerializedName().equals(lower)) return value;
		throw new LuaException("Unknown value " + name + ", expected one of " + Arrays.stream(values).map(E::getSerializedName).toList());
	}
}
