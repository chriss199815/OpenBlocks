package openblocks.common;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replaces the 1.12 {@code Block.recolorBlock} for vanilla blocks: in 1.21 each color is its own block,
 * so a dyed block is swapped for the same block in another color, keeping its properties.
 * Covers the blocks that had a color property in 1.12.
 */
public final class DyedBlocks {

	private static final List<String> FAMILIES = List.of("_wool", "_carpet", "_terracotta", "_concrete", "_concrete_powder", "_stained_glass", "_stained_glass_pane");

	private DyedBlocks() {}

	public static Optional<BlockState> recolor(BlockState state, DyeColor color) {
		final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (!id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) return Optional.empty();

		for (DyeColor current : DyeColor.values()) {
			final String prefix = current.getSerializedName();
			for (String family : FAMILIES) {
				if (!id.getPath().equals(prefix + family)) continue;
				if (current == color) return Optional.empty();
				final Block recolored = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + family));
				return Optional.of(recolored.withPropertiesOf(state));
			}
		}
		return Optional.empty();
	}
}
