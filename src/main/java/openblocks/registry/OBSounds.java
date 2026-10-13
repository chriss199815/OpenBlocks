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

	private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
		return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(OpenBlocks.location(id)));
	}
}
