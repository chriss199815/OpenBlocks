package openblocks;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Port of the legacy {@code Config} class. Values are added here only when the feature that uses them is ported.
 */
public class OBConfig {

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec SPEC = BUILDER.build();
}
