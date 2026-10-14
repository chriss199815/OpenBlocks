package openblocks.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side part of the legacy {@code Config} class. Values are added here only when the feature that uses them is ported.
 */
public class OBClientConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final SkyBlock SKY_BLOCK = new SkyBlock(BUILDER);

	public static final ModConfigSpec SPEC = BUILDER.build();

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
