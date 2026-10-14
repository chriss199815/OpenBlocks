package openblocks.item;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;
import openblocks.OpenBlocks;
import openblocks.entity.GoldenEye;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code ItemGoldenEye}: sneak-using locks it on the nearest structure, using it throws it like an eye of
 * ender, it shows the way for a few seconds and drops back. Every throw costs one charge, ender pearls recharge it.
 * The structures come from the tag {@code openblocks:golden_eye_located} (legacy: the structure generators of the
 * dimension) and are found with the vanilla locate search.
 */
public class GoldenEyeItem extends Item {

	public static final int MAX_DAMAGE = 100;

	public static final TagKey<Structure> LOCATED = TagKey.create(Registries.STRUCTURE, OpenBlocks.location("golden_eye_located"));

	// like the eye of ender and /locate, in chunks
	private static final int SEARCH_RADIUS = 100;

	public GoldenEyeItem(Properties properties) {
		super(properties);
	}

	/**
	 * A new golden eye is empty, like legacy; it has to be charged with ender pearls first.
	 */
	public static ItemStack createEmpty(Item item) {
		final ItemStack stack = new ItemStack(item);
		stack.setDamageValue(MAX_DAMAGE);
		return stack;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

		if (level instanceof ServerLevel serverLevel) {
			if (player.isShiftKeyDown()) lockOnNearestStructure(stack, serverLevel, player);
			else if (throwEye(stack, serverLevel, player)) player.setItemInHand(hand, player.getAbilities().instabuild? stack : ItemStack.EMPTY);
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	private static void lockOnNearestStructure(ItemStack stack, ServerLevel level, Player player) {
		final Pair<BlockPos, Holder<Structure>> found = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(LOCATED)
				.map(structures -> findNearest(level, structures, player.blockPosition()))
				.orElse(null);

		final var key = found != null? found.getSecond().unwrapKey().orElse(null) : null;
		if (key == null) {
			player.sendSystemMessage(Component.translatable("openblocks.misc.no_nearby_structures"));
			return;
		}

		stack.set(OBComponents.GOLDEN_EYE_TARGET.get(), new GoldenEyeTarget(key, GlobalPos.of(level.dimension(), found.getFirst())));
		player.sendSystemMessage(Component.translatable("openblocks.misc.locked_on_nearest_structure", GoldenEyeTarget.structureName(key)));
	}

	private static Pair<BlockPos, Holder<Structure>> findNearest(ServerLevel level, HolderSet<Structure> structures, BlockPos pos) {
		if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) return null;
		// unlike legacy, also finds structures in explored chunks
		return level.getChunkSource().getGenerator().findNearestMapStructure(level, structures, pos, SEARCH_RADIUS, false);
	}

	private static boolean throwEye(ItemStack stack, ServerLevel level, Player player) {
		final GoldenEyeTarget target = stack.get(OBComponents.GOLDEN_EYE_TARGET.get());
		if (target == null) {
			player.sendSystemMessage(Component.translatable("openblocks.misc.structure_not_locked"));
			return false;
		}
		if (target.pos().dimension() != level.dimension()) {
			player.sendSystemMessage(Component.translatable("openblocks.misc.structure_other_dimension", GoldenEyeTarget.structureName(target.structure())));
			return false;
		}

		final boolean creative = player.getAbilities().instabuild;
		if (!creative && stack.getDamageValue() >= stack.getMaxDamage()) return false;

		// in creative mode the eye stays in the hand, like the eye of ender, and the thrown one vanishes
		ItemStack thrown = ItemStack.EMPTY;
		if (!creative) {
			thrown = stack.copy();
			thrown.setDamageValue(stack.getDamageValue() + 1);
		}

		final GoldenEye eye = new GoldenEye(level, player, target.pos().pos(), thrown);
		level.addFreshEntity(eye);
		level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		final GoldenEyeTarget target = stack.get(OBComponents.GOLDEN_EYE_TARGET.get());
		if (target != null) tooltip.add(Component.translatable("openblocks.misc.locked_on_structure", GoldenEyeTarget.structureName(target.structure())));
	}
}
