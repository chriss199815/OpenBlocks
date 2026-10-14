package openblocks.common;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import openblocks.util.SideBits;

/**
 * Side configuration of a machine; replaces OpenModsLib {@code IConfigurableGuiSlots}. Each auto slot has the sides it is accessible from
 * and an "auto" flag that makes the machine move things itself. Changed from the GUI through the vanilla container button packet.
 * Unlike legacy (no access until configured), all sides are open by default, so machines work without configuration like vanilla ones.
 */
public class AutoSlotConfig<E extends Enum<E>> {

	private final E[] slots;
	private final Map<E, Set<Direction>> sides;
	private final Set<E> autoSlots;
	private final Runnable onChange;

	public AutoSlotConfig(Class<E> type, Set<E> defaultAuto, Runnable onChange) {
		this.slots = type.getEnumConstants();
		this.sides = new EnumMap<>(type);
		for (E slot : slots)
			sides.put(slot, EnumSet.allOf(Direction.class));
		this.autoSlots = EnumSet.noneOf(type);
		autoSlots.addAll(defaultAuto);
		this.onChange = onChange;
	}

	public Set<Direction> getSides(E slot) {
		return sides.get(slot);
	}

	public boolean isOpen(E slot, Direction side) {
		return sides.get(slot).contains(side);
	}

	public boolean isAuto(E slot) {
		return autoSlots.contains(slot);
	}

	public void toggleSide(E slot, Direction side) {
		final Set<Direction> slotSides = sides.get(slot);
		if (!slotSides.remove(side)) slotSides.add(side);
		onChange.run();
	}

	public void toggleAuto(E slot) {
		if (!autoSlots.remove(slot)) autoSlots.add(slot);
		onChange.run();
	}

	public int toggleSideButtonId(E slot, Direction side) {
		return slot.ordinal() * 6 + side.get3DDataValue();
	}

	public int toggleAutoButtonId(E slot) {
		return slots.length * 6 + slot.ordinal();
	}

	/**
	 * Button ids from {@code 0} to {@link #getButtonCount()} - 1 belong to this config.
	 */
	public int getButtonCount() {
		return slots.length * 7;
	}

	public boolean handleButton(int id) {
		if (id < 0 || id >= getButtonCount()) return false;
		if (id < slots.length * 6) toggleSide(slots[id / 6], Direction.from3DDataValue(id % 6));
		else toggleAuto(slots[id - slots.length * 6]);
		return true;
	}

	public void save(CompoundTag tag) {
		for (E slot : slots) {
			final String key = key(slot);
			tag.putByte(key + "_sides", SideBits.write(getSides(slot)));
			tag.putBoolean(key + "_auto", isAuto(slot));
		}
	}

	public void load(CompoundTag tag) {
		for (E slot : slots) {
			final String key = key(slot);
			if (tag.contains(key + "_sides")) SideBits.read(getSides(slot), tag.getByte(key + "_sides"));
			if (tag.contains(key + "_auto")) {
				if (tag.getBoolean(key + "_auto")) autoSlots.add(slot);
				else autoSlots.remove(slot);
			}
		}
	}

	private static String key(Enum<?> slot) {
		return slot.name().toLowerCase(Locale.ROOT);
	}
}
