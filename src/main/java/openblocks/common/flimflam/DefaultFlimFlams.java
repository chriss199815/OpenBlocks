package openblocks.common.flimflam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import openblocks.api.FlimFlamRegistry;
import openblocks.registry.OBComponents;
import openblocks.registry.OBSounds;

/**
 * Port of the legacy flim flams ({@code openblocks.enchantments.flimflams}) with their legacy names, costs and weights.
 */
public class DefaultFlimFlams {

	private static final int TEN_MINUTES = 10 * 60 * 20;

	public static void register() {
		FlimFlamRegistry.register("inventory-shuffle", -50, 100, DefaultFlimFlams::inventoryShuffle).markSafe();
		FlimFlamRegistry.register("useless-tool", -125, 50, DefaultFlimFlams::uselessTool).markSafe();
		FlimFlamRegistry.register("bane", -125, 100, DefaultFlimFlams::bane).markSafe();
		FlimFlamRegistry.register("epic-lore", -10, 100, DefaultFlimFlams::lore).markSafe();
		FlimFlamRegistry.register("living-rename", -10, 100, DefaultFlimFlams::rename).markSafe();
		FlimFlamRegistry.register("squid", -75, 50, DefaultFlimFlams::squid).markSafe();
		FlimFlamRegistry.register("sheep-dye", -5, 50, DefaultFlimFlams::sheepDye).markSafe();
		FlimFlamRegistry.register("invisible-mobs", -25, 10, DefaultFlimFlams::invisibleMobs).markSafe();
		FlimFlamRegistry.register("sound", -5, 150, DefaultFlimFlams::sound).markSilent().markSafe();

		FlimFlamRegistry.register("snowballs", -50, 50, DefaultFlimFlams::snowballs);
		FlimFlamRegistry.register("teleport", -100, 30, DefaultFlimFlams::teleport);
		FlimFlamRegistry.register("mount", -150, 25, DefaultFlimFlams::mount);
		FlimFlamRegistry.register("encase", -50, 50, DefaultFlimFlams::encase).setRange(Integer.MIN_VALUE, -300);
		FlimFlamRegistry.register("creepers", -60, 50, DefaultFlimFlams::creepers);
		FlimFlamRegistry.register("disarm", -50, 50, DefaultFlimFlams::disarm);
		FlimFlamRegistry.register("effect", -75, 75, DefaultFlimFlams::effect);
		FlimFlamRegistry.register("skyblock", -100, 150, DefaultFlimFlams::skyblock).setRange(Integer.MIN_VALUE, -400);
	}

	private static <T> T random(List<T> list, RandomSource random) {
		return list.get(random.nextInt(list.size()));
	}

	private static AABB around(ServerPlayer target, double range) {
		return target.getBoundingBox().inflate(range);
	}

	private static boolean inventoryShuffle(ServerPlayer target) {
		if (target.containerMenu != target.inventoryMenu) return false;
		final List<ItemStack> items = new ArrayList<>(target.getInventory().items);
		Collections.shuffle(items);
		for (int i = 0; i < items.size(); i++)
			target.getInventory().items.set(i, items.get(i));
		return true;
	}

	private static final List<Item> USELESS_TOOLS = List.of(
			Items.DIAMOND_PICKAXE, Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE,
			Items.DIAMOND_SHOVEL, Items.GOLDEN_SHOVEL, Items.IRON_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL,
			Items.DIAMOND_AXE, Items.GOLDEN_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.WOODEN_AXE,
			Items.SHEARS,
			Items.DIAMOND_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.LEATHER_LEGGINGS,
			Items.DIAMOND_BOOTS, Items.GOLDEN_BOOTS, Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS, Items.LEATHER_BOOTS,
			Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE,
			Items.DIAMOND_HELMET, Items.GOLDEN_HELMET, Items.IRON_HELMET, Items.CHAINMAIL_HELMET, Items.LEATHER_HELMET);

	/**
	 * Drops a tool or armor piece with one random enchantment that breaks on the next use.
	 */
	private static boolean uselessTool(ServerPlayer target) {
		final RandomSource random = target.getRandom();
		final ItemStack dropped = new ItemStack(random(USELESS_TOOLS, random));
		final List<EnchantmentInstance> enchantments = EnchantmentHelper.getAvailableEnchantmentResults(30, dropped,
				target.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE)
						.map(HolderSet.Named::stream).orElseGet(Stream::empty));
		if (enchantments.isEmpty()) return false;
		final EnchantmentInstance data = random(enchantments, random);
		dropped.enchant(data.enchantment, random.nextInt(data.level) + 1);
		dropped.setDamageValue(dropped.getMaxDamage());
		target.level().addFreshEntity(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), dropped));
		return true;
	}

	/**
	 * Bane of Arthropods V on the first unstackable item that cannot be enchanted otherwise.
	 */
	private static boolean bane(ServerPlayer target) {
		final Holder<Enchantment> bane = target.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BANE_OF_ARTHROPODS);
		for (ItemStack stack : target.getInventory().items) {
			if (!stack.isEmpty() && stack.getMaxStackSize() == 1 && !stack.isEnchantable() && !stack.isEnchanted()) {
				stack.enchant(bane, 5);
				return true;
			}
		}
		return false;
	}

	private static boolean lore(ServerPlayer target) {
		final List<EquipmentSlot> slots = new ArrayList<>(List.of(EquipmentSlot.values()));
		Collections.shuffle(slots);
		for (EquipmentSlot slot : slots) {
			final ItemStack item = target.getItemBySlot(slot);
			if (item.isEmpty()) continue;
			final String lore = LoreGenerator.generateLore(target.getRandom(), target.getName().getString(), identityType(item));
			item.set(OBComponents.SILLY_LORE, splitText(lore, 30));
			return true;
		}
		return false;
	}

	private static List<String> splitText(String text, int maxSize) {
		final List<String> result = new ArrayList<>();
		final List<String> buffer = new ArrayList<>();
		int length = 0;
		for (String word : text.split(" ")) {
			if (word.isEmpty()) continue;
			if (length + word.length() > maxSize && !buffer.isEmpty()) {
				result.add(String.join(" ", buffer));
				length = 0;
				buffer.clear();
			}
			buffer.add(word);
			length += word.length() + 1;
		}
		if (!buffer.isEmpty()) result.add(String.join(" ", buffer));
		return result;
	}

	private static String identityType(ItemStack stack) {
		final Item item = stack.getItem();
		if (item instanceof ArmorItem armor) {
			return switch (armor.getType()) {
				case HELMET -> "helmet";
				case CHESTPLATE -> "chestplate";
				case LEGGINGS -> "leggings";
				case BOOTS -> "boots";
				default -> "gizmo";
			};
		}
		if (item instanceof PickaxeItem) return "pickaxe";
		if (item instanceof ShearsItem) return "shears";
		if (item instanceof AxeItem) return "axe";
		if (item instanceof ShovelItem) return "shovel";
		if (item instanceof BlockItem) return "block";
		if (item instanceof BucketItem) return "bucket";
		return "gizmo";
	}

	private static boolean rename(ServerPlayer target) {
		final List<Mob> mobs = target.level().getEntitiesOfClass(Mob.class, around(target, 20));
		Collections.shuffle(mobs);
		for (Mob mob : mobs) {
			if (!mob.hasCustomName()) {
				final String name = LoreGenerator.generateName(target.getRandom());
				mob.setCustomName(Component.literal(name.length() > 64? name.substring(0, 61) + "..." : name));
				return true;
			}
		}
		return false;
	}

	private static final List<String> SQUID_NAMES = List.of("Fancy Hat", "Hello there!", "Look at my hat!", "My hat is amazing!", "Ceci n'est pas une pipe", "???");

	private static boolean squid(ServerPlayer target) {
		if (target.isVehicle()) return false;
		final Squid squid = EntityType.SQUID.create(target.level());
		if (squid == null) return false;
		squid.moveTo(target.getX(), target.getBoundingBox().minY, target.getZ());
		squid.setCustomName(Component.literal(random(SQUID_NAMES, target.getRandom())));
		target.level().addFreshEntity(squid);
		return true;
	}

	private static boolean sheepDye(ServerPlayer target) {
		final List<Sheep> sheep = target.level().getEntitiesOfClass(Sheep.class, around(target, 20));
		if (sheep.isEmpty()) return false;
		random(sheep, target.getRandom()).setColor(DyeColor.byId(target.getRandom().nextInt(DyeColor.values().length)));
		return true;
	}

	private static boolean invisibleMobs(ServerPlayer target) {
		final List<Mob> mobs = target.level().getEntitiesOfClass(Mob.class, around(target, 20));
		if (mobs.isEmpty()) return false;
		for (Mob mob : mobs)
			if (target.getRandom().nextFloat() < 0.3) mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, TEN_MINUTES, 1));
		return true;
	}

	private static List<Holder<SoundEvent>> sounds() {
		final List<Holder<SoundEvent>> sounds = new ArrayList<>(List.of(OBSounds.MOSQUITO, OBSounds.ALARM_CLOCK, OBSounds.VIBRATE, OBSounds.PLAYER_WHOOPS, SoundEvents.GENERIC_EXPLODE));
		for (SoundEvent sound : List.of(SoundEvents.TNT_PRIMED, SoundEvents.ELDER_GUARDIAN_CURSE, SoundEvents.BLAZE_SHOOT, SoundEvents.ENDERMAN_STARE,
				SoundEvents.GHAST_SCREAM, SoundEvents.GHAST_SHOOT, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, SoundEvents.CREEPER_PRIMED))
			sounds.add(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound));
		return sounds;
	}

	/**
	 * A sound only the target hears.
	 */
	private static boolean sound(ServerPlayer target) {
		final Holder<SoundEvent> sound = random(sounds(), target.getRandom());
		target.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, target.getX(), target.getY(), target.getZ(), 1, 1, target.getRandom().nextLong()));
		return true;
	}

	private static boolean snowballs(ServerPlayer target) {
		final Level level = target.level();
		final RandomSource random = target.getRandom();
		for (int i = 0; i < 200; i++) {
			final Snowball snowball = new Snowball(level, target.getX(), target.getY() + 4, target.getZ());
			snowball.setDeltaMovement(random.nextGaussian() * 0.05, 1, random.nextGaussian() * 0.05);
			level.addFreshEntity(snowball);
		}
		return true;
	}

	private static boolean teleport(ServerPlayer target) {
		final ThrownEnderpearl pearl = new ThrownEnderpearl(target.level(), target);
		pearl.setPos(target.getX(), target.getY() + 1, target.getZ());
		pearl.setDeltaMovement(target.getRandom().nextGaussian(), 0.5, target.getRandom().nextGaussian());
		target.level().addFreshEntity(pearl);
		return true;
	}

	private static boolean mount(ServerPlayer target) {
		final List<Mob> mobs = target.level().getEntitiesOfClass(Mob.class, around(target, 40),
				mob -> !(mob instanceof Creeper) && !(mob instanceof Squid));
		if (mobs.isEmpty()) return false;
		return target.startRiding(random(mobs, target.getRandom()));
	}

	/**
	 * Dirt around the player, with a torch inside.
	 */
	private static boolean encase(ServerPlayer target) {
		final Level level = target.level();
		final int playerX = Mth.floor(target.getX());
		final int playerY = Mth.floor(target.getBoundingBox().minY) - 1;
		final int playerZ = Mth.floor(target.getZ());

		for (int y = playerY; y <= playerY + 3; y++) {
			for (int x = playerX - 1; x <= playerX + 1; x++) {
				for (int z = playerZ - 1; z <= playerZ + 1; z++) {
					final boolean isGap = y < playerY + 3 && x == playerX && z == playerZ;
					final BlockPos pos = new BlockPos(x, y, z);
					if (!isGap && level.isEmptyBlock(pos)) level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
				}
			}
		}

		final BlockPos torchPos = new BlockPos(playerX, playerY + 2, playerZ);
		if (level.isEmptyBlock(torchPos) && Blocks.TORCH.defaultBlockState().canSurvive(level, torchPos))
			level.setBlockAndUpdate(torchPos, Blocks.TORCH.defaultBlockState());
		return true;
	}

	/**
	 * Charged creepers that do not explode (radius 0), falling from the sky around the player.
	 */
	private static boolean creepers(ServerPlayer target) {
		final RandomSource random = target.getRandom();
		final CompoundTag dud = new CompoundTag();
		dud.putBoolean("powered", true);
		dud.putByte("ExplosionRadius", (byte)0);
		for (int i = 0; i < 15; i++) {
			final Creeper creeper = EntityType.CREEPER.create(target.level());
			if (creeper == null) return false;
			creeper.readAdditionalSaveData(dud);
			creeper.moveTo(target.getX() + 20 * (random.nextFloat() - 0.5),
					target.getY() + 5 * (1 + random.nextFloat()),
					target.getZ() + 20 * (random.nextFloat() - 0.5));
			target.level().addFreshEntity(creeper);
		}
		return true;
	}

	/**
	 * Drops one item each from armor slots and the selected slot, with half a chance each.
	 */
	private static boolean disarm(ServerPlayer target) {
		boolean dropped = false;
		for (int i = 0; i < 4; i++)
			dropped |= tryDropStack(target, 36 + i);
		dropped |= tryDropStack(target, target.getInventory().selected);
		return dropped;
	}

	private static boolean tryDropStack(ServerPlayer target, int slot) {
		final ItemStack stack = target.getInventory().getItem(slot);
		if (stack.isEmpty() || target.getRandom().nextFloat() > 0.5f) return false;
		target.drop(target.getInventory().removeItem(slot, 1), true);
		return true;
	}

	private record Effect(Holder<MobEffect> effect, int levelMin, int levelMax, int durationMin, int durationMax) {}

	private static int seconds(int s) {
		return s * 20;
	}

	private static final List<Effect> EFFECTS = List.of(
			new Effect(MobEffects.BLINDNESS, 1, 1, seconds(15), seconds(60)),
			new Effect(MobEffects.CONFUSION, 1, 1, seconds(15), seconds(60)),
			new Effect(MobEffects.DIG_SLOWDOWN, 50, 100, seconds(15), seconds(60)),
			new Effect(MobEffects.JUMP, 30, 50, seconds(5), seconds(15)),
			new Effect(MobEffects.MOVEMENT_SPEED, 50, 100, seconds(5), seconds(15)),
			new Effect(MobEffects.MOVEMENT_SLOWDOWN, 4, 7, seconds(5), seconds(15)),
			new Effect(MobEffects.LEVITATION, 1, 1, seconds(5), seconds(15)));

	/**
	 * Two random effects; like legacy, the levels are amplifiers (so mining fatigue is extreme).
	 */
	private static boolean effect(ServerPlayer target) {
		final RandomSource random = target.getRandom();
		final List<Effect> effects = new ArrayList<>(EFFECTS);
		Collections.shuffle(effects);
		for (Effect selected : effects.subList(0, 2)) {
			final int duration = Mth.nextInt(random, selected.durationMin(), selected.durationMax());
			final int amplifier = Mth.nextInt(random, selected.levelMin(), selected.levelMax());
			target.addEffect(new MobEffectInstance(selected.effect(), duration, amplifier));
		}
		return true;
	}

	/**
	 * Puts the player on a small ice platform 150 blocks up (at most up to near the build limit).
	 */
	private static boolean skyblock(ServerPlayer target) {
		final ServerLevel level = target.serverLevel();
		if (level.dimensionType().ultraWarm() || level.dimensionType().hasCeiling()) return false;

		final BlockPos trapCenter = BlockPos.containing(target.getX(), Math.min(target.getY() + 150, level.getMaxBuildHeight() - 6), target.getZ());
		final List<BlockPos> blocks = List.of(trapCenter.below(), trapCenter.relative(Direction.EAST), trapCenter.relative(Direction.NORTH),
				trapCenter.relative(Direction.SOUTH), trapCenter.relative(Direction.WEST));
		for (BlockPos pos : blocks)
			if (!level.isEmptyBlock(pos)) return false;
		for (BlockPos pos : blocks)
			if (!level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState())) return false;

		target.teleportTo(trapCenter.getX() + 0.5, trapCenter.getY() + 1, trapCenter.getZ() + 0.5);
		return true;
	}
}
