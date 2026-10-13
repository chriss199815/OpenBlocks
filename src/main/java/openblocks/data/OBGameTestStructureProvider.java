package openblocks.data;

import com.google.common.hash.Hashing;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import openblocks.OpenBlocks;

/**
 * Generates empty structure templates for GameTests, which set up their blocks in code.
 */
public class OBGameTestStructureProvider implements DataProvider {

	public static final String EMPTY_3X8X3 = "empty_3x8x3";

	private final PackOutput.PathProvider pathProvider;

	public OBGameTestStructureProvider(PackOutput output) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "structure");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return CompletableFuture.runAsync(() -> writeEmpty(cache, EMPTY_3X8X3, 3, 8, 3), Util.backgroundExecutor());
	}

	private void writeEmpty(CachedOutput cache, String name, int x, int y, int z) {
		final CompoundTag tag = new CompoundTag();
		final ListTag size = new ListTag();
		size.add(IntTag.valueOf(x));
		size.add(IntTag.valueOf(y));
		size.add(IntTag.valueOf(z));
		tag.put("size", size);
		tag.put("palette", new ListTag());
		tag.put("blocks", new ListTag());
		tag.put("entities", new ListTag());
		tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

		try {
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			NbtIo.writeCompressed(tag, bytes);
			final byte[] data = bytes.toByteArray();
			final Path path = pathProvider.file(OpenBlocks.location(name), "nbt");
			cache.writeIfNeeded(path, data, Hashing.sha1().hashBytes(data));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String getName() {
		return "OpenBlocks GameTest structures";
	}
}
