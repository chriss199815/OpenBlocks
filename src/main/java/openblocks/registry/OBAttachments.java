package openblocks.registry;

import java.util.function.Supplier;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import openblocks.OpenBlocks;
import openblocks.common.CraneState;
import openblocks.common.PedometerState;
import openblocks.common.flimflam.LuckState;

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

	/**
	 * Present while the player sleeps in a sleeping bag instead of a bed; the value is the player's yaw when lying down, which replaces
	 * the bed's facing for body and camera. Synced, so other clients draw the bag.
	 */
	public static final Supplier<AttachmentType<Float>> SLEEPING_BAG = ATTACHMENT_TYPES.register("sleeping_bag",
			() -> AttachmentType.builder(() -> 0.0f)
					.sync(ByteBufCodecs.FLOAT)
					.build());

	/**
	 * Pedometer measurement of the player; only used on their own client, never saved or synced (like the legacy capability).
	 */
	public static final Supplier<AttachmentType<PedometerState>> PEDOMETER = ATTACHMENT_TYPES.register("pedometer",
			() -> AttachmentType.builder(PedometerState::new).build());

	/**
	 * Crane of a player wearing the crane backpack (legacy {@code CraneRegistry}); on both sides, never saved or synced.
	 */
	public static final Supplier<AttachmentType<CraneState>> CRANE = ATTACHMENT_TYPES.register("crane",
			() -> AttachmentType.builder(CraneState::new).build());

	/**
	 * Luck of the player for flim flams; replaces the legacy {@code Luck} capability.
	 */
	public static final Supplier<AttachmentType<LuckState>> LUCK = ATTACHMENT_TYPES.register("luck",
			() -> AttachmentType.builder(LuckState::new)
					.serialize(LuckState.CODEC)
					.build());

	/**
	 * Bricks the player can drop, filled by eating tasty clay; replaces the legacy {@code bowels} capability. Saved, but
	 * emptied on death (the bricks drop).
	 */
	public static final Supplier<AttachmentType<Integer>> BOWELS = ATTACHMENT_TYPES.register("bowels",
			() -> AttachmentType.builder(() -> 0)
					.serialize(ExtraCodecs.NON_NEGATIVE_INT)
					.build());
}
