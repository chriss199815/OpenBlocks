package openblocks.registry;

import java.util.function.Supplier;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import openblocks.OpenBlocks;

public class OBAttachments {

	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, OpenBlocks.MODID);

	/**
	 * Present while the player has a hang glider on their back; the value is the hand holding it. Replaces legacy {@code EntityHangGlider}.
	 * Not saved, so relogging or respawning takes the glider off (like the legacy entity).
	 */
	public static final Supplier<AttachmentType<InteractionHand>> HANG_GLIDER = ATTACHMENT_TYPES.register("hang_glider",
			() -> AttachmentType.builder(() -> InteractionHand.MAIN_HAND)
					.sync(NeoForgeStreamCodecs.enumCodec(InteractionHand.class))
					.build());
}
