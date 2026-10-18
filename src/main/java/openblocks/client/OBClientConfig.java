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
	public static final Guide GUIDE = new Guide(BUILDER);
	public static final Imaginary IMAGINARY = new Imaginary(BUILDER);
	public static final Glasses GLASSES = new Glasses(BUILDER);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static class Glasses {
		public final ModConfigSpec.DoubleValue opacity;
		public final ModConfigSpec.BooleanValue useTexture;

		private Glasses(ModConfigSpec.Builder builder) {
			builder.push("glasses");
			opacity = builder
					.comment("How much sonic glasses obscure the world: 0.0 - no visible change to world, 1.0 - world fully obscured")
					.defineInRange("opacity", 0.7, 0, 1);
			useTexture = builder
					.comment("Use texture for obscuring world")
					.define("useTexture", true);
			builder.pop();
		}
	}

	public static class Imaginary {
		public final ModConfigSpec.DoubleValue fadingSpeed;

		private Imaginary(ModConfigSpec.Builder builder) {
			builder.push("imaginary");
			// legacy changed the visibility by 0.0075 per frame, so the fading depended on the frame rate; this is the same at 60 fps
			fadingSpeed = builder
					.comment("Share of their full visibility imaginary blocks gain or lose per second when they appear or fade away")
					.defineInRange("fadingSpeed", 0.45, 0.01, 100);
			builder.pop();
		}
	}

	public static class Guide {
		public final ModConfigSpec.IntValue renderDistance;

		private Guide(ModConfigSpec.Builder builder) {
			builder.push("guide");
			// legacy renderDistanceSq took the square
			renderDistance = builder
					.comment("Maximum distance (in blocks) at which the shapes of building guides are drawn")
					.defineInRange("renderDistance", 256, 16, 1024);
			builder.pop();
		}
	}

	public static class Tomfoolery {
		public final ModConfigSpec.BooleanValue doItWhileTyping;
		public final ModConfigSpec.IntValue sillyLoreDisplay;

		private Tomfoolery(ModConfigSpec.Builder builder) {
			builder.push("tomfoolery");
			doItWhileTyping = builder
					.comment("You know, THAT thing! That you shouldn't do in public!")
					.define("doItWhileTyping", false);
			sillyLoreDisplay = builder
					.comment("0 - lore hidden, 1 - visible only with pressed ALT, 2 - always visible")
					.defineInRange("sillyLoreDisplay", 1, 0, 2);
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
