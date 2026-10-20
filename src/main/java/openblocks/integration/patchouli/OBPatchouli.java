package openblocks.integration.patchouli;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import openblocks.OpenBlocks;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * The info book is a Patchouli book (replaces legacy {@code GuiInfoBook}); its content comes from {@code OBManualProvider}.
 * Patchouli is optional, so its classes are only touched through here after {@link #isLoaded()}.
 */
public class OBPatchouli {

	public static final String MODID = "patchouli";

	public static final ResourceLocation BOOK = OpenBlocks.location("manual");

	public static boolean isLoaded() {
		return ModList.get().isLoaded(MODID);
	}

	public static void openBook(ServerPlayer player) {
		PatchouliAPI.get().openBookGUI(player, BOOK);
	}
}
