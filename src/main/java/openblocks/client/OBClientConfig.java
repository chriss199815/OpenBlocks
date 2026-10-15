package openblocks.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side part of the legacy {@code Config} class. Values are added here only when the feature that uses them is ported.
 */
public class OBClientConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final SkyBlock SKY_BLOCK = new SkyBlock(BUILDER);
	public static final Projector PROJECTOR = new Projector(BUILDER);
	public static final Tomfoolery TOMFOOLERY = new Tomfoolery(BUILDER);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static class Tomfoolery {
		public final ModConfigSpec.BooleanValue doItWhileTyping;

		private Tomfoolery(ModConfigSpec.Builder builder) {
			builder.push("tomfoolery");
			doItWhileTyping = builder
					.comment("You know, THAT thing! That you shouldn't do in public!")
					.define("doItWhileTyping", false);
			builder.pop();
		}
	}

	public static class Projector {
		public final ModConfigSpec.BooleanValue renderHolographicCone;
		public final ModConfigSpec.IntValue brightness;

		private Projector(ModConfigSpec.Builder builder) {
			builder.push("projector");
			renderHolographicCone = builder
					.comment("Projector will render a holographic cone whenever active")
					.define("renderHolographicCone", true);
			// legacy took a raw lightmap value from 0 to 255
			brightness = builder
					.comment("Light level (0 to 15) the projector's cone is rendered with. Set -1 to use the light of the world.",
							"Keep in mind that the light of the world means that the cone will render as light blue during the day and dark blue during the night.")
					.defineInRange("brightness", -1, -1, 15);
			builder.pop();
		}
	}

	public static class SkyBlock {
		public final ModConfigSpec.BooleanValue renderingEnabled;

		// legacy optifineOverride is gone, there is no Optifine
		private SkyBlock(ModConfigSpec.Builder builder) {
			builder.push("skyblock");
			renderingEnabled = builder
					.comment("Enables skyblock rendering. Disable when there are graphic glitches or performance problems (active sky blocks then look inactive)")
					.define("renderingEnabled", true);
			builder.pop();
		}
	}
}
