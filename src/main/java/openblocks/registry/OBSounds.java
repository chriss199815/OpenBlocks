package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;

/**
 * Sound events for the entries in {@code sounds.json}. Only sounds of ported features are registered.
 */
public class OBSounds {

	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, OpenBlocks.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> ELEVATOR_ACTIVATE = register("elevator.activate");
	public static final DeferredHolder<SoundEvent, SoundEvent> BOTTLER_SIGNAL = register("bottler.signal");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRAVE_ROB = register("grave.rob");
	public static final DeferredHolder<SoundEvent, SoundEvent> SQUEEGEE_USE = register("squeegee.use");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEARTRAP_OPEN = register("beartrap.open");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEARTRAP_CLOSE = register("beartrap.close");
	public static final DeferredHolder<SoundEvent, SoundEvent> LUGGAGE_WALK = register("luggage.walk");
	public static final DeferredHolder<SoundEvent, SoundEvent> LUGGAGE_EAT_FOOD = register("luggage.eat.food");
	public static final DeferredHolder<SoundEvent, SoundEvent> LUGGAGE_EAT_ITEM = register("luggage.eat.item");

	private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
		return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(OpenBlocks.location(id)));
	}
}
