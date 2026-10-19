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
	public static final DeferredHolder<SoundEvent, SoundEvent> TARGET_OPEN = register("target.open");
	public static final DeferredHolder<SoundEvent, SoundEvent> TARGET_CLOSE = register("target.close");
	public static final DeferredHolder<SoundEvent, SoundEvent> PEDOMETER_USE = register("pedometer.use");
	public static final DeferredHolder<SoundEvent, SoundEvent> SLIMALYZER_SIGNAL = register("slimalyzer.signal");
	public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_ACTIVATE = register("cannon.activate");
	public static final DeferredHolder<SoundEvent, SoundEvent> PLAYER_WHOOPS = register("best.feature.ever.fart");
	public static final DeferredHolder<SoundEvent, SoundEvent> CRAYON_PLACE = register("crayon.place");
	public static final DeferredHolder<SoundEvent, SoundEvent> MOSQUITO = register("annoying.mosquito");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_CLOCK = register("annoying.alarmclock");
	public static final DeferredHolder<SoundEvent, SoundEvent> VIBRATE = register("annoying.vibrate");
	public static final DeferredHolder<SoundEvent, SoundEvent> VARIO = register("hangglider.vario");

	private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
		return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(OpenBlocks.location(id)));
	}
}
