package openblocks.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import openblocks.registry.OBComponents;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code ItemSlimalyzer}: lights up (and pings once) while it is in a slime chunk, in an inventory or
 * lying on the ground. The world seed is only known on the server, so the state is kept in {@code OBComponents.SLIMALYZER_ACTIVE}.
 */
public class SlimalyzerItem extends Item {

	public SlimalyzerItem(Properties properties) {
		super(properties);
	}

	public static boolean isActive(ItemStack stack) {
		return stack.has(OBComponents.SLIMALYZER_ACTIVE.get());
	}

	/**
	 * Same check as vanilla slime spawning, in any dimension (like legacy).
	 */
	public static boolean isInSlimeChunk(ServerLevel level, Entity entity) {
		final ChunkPos chunk = entity.chunkPosition();
		return WorldgenRandom.seedSlimeChunk(chunk.x, chunk.z, level.getSeed(), 987234911L).nextInt(10) == 0;
	}

	private static void update(ItemStack stack, Level level, Entity entity) {
		if (!(level instanceof ServerLevel serverLevel)) return;

		final boolean inSlimeChunk = isInSlimeChunk(serverLevel, entity);
		if (isActive(stack) == inSlimeChunk) return;

		if (inSlimeChunk) {
			level.playSound(null, entity.blockPosition(), OBSounds.SLIMALYZER_SIGNAL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
			stack.set(OBComponents.SLIMALYZER_ACTIVE.get(), Unit.INSTANCE);
		} else {
			stack.remove(OBComponents.SLIMALYZER_ACTIVE.get());
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		update(stack, level, entity);
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		update(stack, entity.level(), entity);
		return false;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
	}
}
