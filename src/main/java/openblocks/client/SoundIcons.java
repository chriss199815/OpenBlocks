package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import openblocks.OpenBlocks;

/**
 * Port of legacy {@code SoundIconRegistry} and {@code Icons}: the icon sonic glasses show for a sound. Like the legacy
 * category tree, the longest known prefix of the sound id (split at dots) decides. The entries use the sound ids of
 * 1.21. Legacy's list of mobs is replaced by a lookup of the entity named in the sound ({@code entity.<mob>.<event>},
 * colored like its spawn egg), and block sounds without own entry ({@code block.<block>.<event>}) show the block.
 */
public class SoundIcons {

	public interface Icon {
		/**
		 * Draws the icon as a square from -size to size in x and y.
		 */
		void draw(PoseStack.Pose pose, VertexConsumer consumer, float size, float alpha);
	}

	private record Sprite(Supplier<TextureAtlasSprite> sprite, int color) implements Icon {
		@Override
		public void draw(PoseStack.Pose pose, VertexConsumer consumer, float size, float alpha) {
			final TextureAtlasSprite sprite = this.sprite.get();
			final int color = FastColor.ARGB32.color(Mth.floor(alpha * 255), this.color);
			consumer.addVertex(pose, -size, size, 0).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
			consumer.addVertex(pose, -size, -size, 0).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
			consumer.addVertex(pose, size, -size, 0).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
			consumer.addVertex(pose, size, size, 0).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
		}
	}

	/**
	 * Icons are drawn without depth test, so the front only has to come after the back.
	 */
	private record Composed(Icon front, Icon back, float backScale) implements Icon {
		@Override
		public void draw(PoseStack.Pose pose, VertexConsumer consumer, float size, float alpha) {
			back.draw(pose, consumer, size * backScale, alpha);
			front.draw(pose, consumer, size, alpha);
		}
	}

	private static final int WHITE = 0xFFFFFF;
	private static final int BLACK = 0x000000;

	private static final ResourceLocation FRAME = ob("frame");
	private static final Icon WHITE_FRAME = icon(FRAME, WHITE);
	private static final Icon RED_FRAME = icon(FRAME, 0xFF0000);
	private static final Icon BLUE_FRAME = icon(FRAME, 0x0000FF);
	private static final Icon GENERIC = icon(ob("generic"));
	private static final Icon UNKNOWN = icon(ob("unknown"));

	// sound name -> entity id, where they differ
	private static final Map<String, String> MOB_ALIASES = Map.of("puffer_fish", "pufferfish");

	private static final String[] MUSIC_DISCS = { "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait",
			"otherside", "5", "pigstep", "relic", "creator", "creator_music_box", "precipice" };

	private static final Map<String, Icon> ICONS = new HashMap<>();
	private static final Map<ResourceLocation, Icon> CACHE = new HashMap<>();

	static {
		registerDefaults();
	}

	private static ResourceLocation ob(String id) {
		return OpenBlocks.location("item/sound_" + id);
	}

	private static ResourceLocation mc(String id) {
		return ResourceLocation.withDefaultNamespace(id);
	}

	private static Icon icon(ResourceLocation sprite, int color) {
		return new Sprite(() -> Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(sprite), color);
	}

	private static Icon icon(ResourceLocation sprite) {
		return icon(sprite, WHITE);
	}

	private static Icon layered(Icon front, Icon back) {
		return new Composed(front, back, 1);
	}

	private static Icon framed(Icon inner, Icon frame) {
		return new Composed(frame, inner, 0.6f);
	}

	private static Icon framed(ResourceLocation inner, Icon frame) {
		return framed(icon(inner), frame);
	}

	/**
	 * Main thread only, like the sound events.
	 */
	public static Icon get(ResourceLocation sound) {
		return CACHE.computeIfAbsent(sound, SoundIcons::find);
	}

	private static Icon find(ResourceLocation sound) {
		final String namespace = sound.getNamespace();
		final String[] path = sound.getPath().split("\\.");
		for (int length = path.length; length >= 2; length--) {
			final Icon icon = ICONS.get(key(namespace, path, length));
			if (icon != null) return icon;
		}

		if (path.length >= 2) {
			final String event = path.length > 2? path[2] : "";
			final Icon icon = switch (path[0]) {
				case "entity" -> mobIcon(namespace, path[1], event);
				case "block" -> blockIcon(namespace, path[1], event);
				default -> null;
			};
			if (icon != null) return icon;
		}

		final Icon icon = ICONS.get(key(namespace, path, 1));
		if (icon != null) return icon;
		return namespace.equals(OpenBlocks.MODID)? GENERIC : UNKNOWN;
	}

	private static String key(String namespace, String[] path, int length) {
		return namespace + ":" + String.join(".", Arrays.copyOf(path, length));
	}

	@Nullable
	private static Icon mobIcon(String namespace, String mob, String event) {
		final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, MOB_ALIASES.getOrDefault(mob, mob));
		final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
		if (type == null) return null;

		final SpawnEggItem egg = SpawnEggItem.byId(type);
		// like legacy, mobs without egg are white in a black frame
		final int primary = egg != null? egg.getColor(0) : WHITE;
		final int secondary = egg != null? egg.getColor(1) : BLACK;
		final String inner = switch (event) {
			case "death" -> "mob_death";
			case "hurt" -> "mob_hurt";
			default -> type.getCategory() == MobCategory.MONSTER? "mob_hostile" : "mob_friendly";
		};
		return framed(icon(ob(inner), primary), icon(FRAME, secondary));
	}

	@Nullable
	private static Icon blockIcon(String namespace, String name, String event) {
		final Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, name)).orElse(null);
		if (block == null || block == Blocks.AIR) return null;
		final Icon icon = new Sprite(() -> Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(block.defaultBlockState()), WHITE);
		return standardBlockIcon(icon, event);
	}

	private static Icon standardBlockIcon(Icon block, String event) {
		return switch (event) {
			case "break" -> framed(layered(icon(mc("block/destroy_stage_5")), block), WHITE_FRAME);
			case "fall" -> framed(layered(icon(ob("mob_hurt")), block), WHITE_FRAME);
			case "hit" -> framed(layered(icon(mc("item/diamond_shovel")), block), WHITE_FRAME);
			case "place" -> framed(block, WHITE_FRAME);
			case "step" -> framed(layered(icon(mc("item/diamond_boots")), block), WHITE_FRAME);
			default -> layered(UNKNOWN, block);
		};
	}

	private static void put(String key, Icon icon) {
		ICONS.put(key.contains(":")? key : "minecraft:" + key, icon);
	}

	private static void standardBlock(String name, ResourceLocation texture) {
		final Icon block = icon(texture);
		for (String event : new String[] { "break", "fall", "hit", "place", "step" })
			put("block." + name + "." + event, standardBlockIcon(block, event));
		put("block." + name, standardBlockIcon(block, ""));
	}

	private static void basicBlock(String name, ResourceLocation texture) {
		put("block." + name, framed(texture, WHITE_FRAME));
	}

	private static void basicBlockAction(String name, ResourceLocation texture) {
		put("block." + name, layered(icon(ob("click")), icon(texture)));
	}

	private static void singleIcon(String key, ResourceLocation texture) {
		put(key, framed(texture, WHITE_FRAME));
	}

	private static void registerDefaults() {
		standardBlock("anvil", mc("block/anvil"));
		standardBlock("wool", mc("block/white_wool"));
		standardBlock("glass", mc("block/glass"));
		standardBlock("grass", mc("block/dirt"));
		standardBlock("gravel", mc("block/gravel"));
		standardBlock("ladder", mc("block/ladder"));
		standardBlock("metal", mc("block/iron_block"));
		standardBlock("sand", mc("block/sand"));
		standardBlock("slime_block", mc("block/slime_block"));
		standardBlock("snow", mc("block/snow"));
		standardBlock("stone", mc("block/stone"));
		standardBlock("wood", mc("block/oak_log_top"));

		basicBlock("brewing_stand", mc("block/brewing_stand"));
		basicBlock("chest", mc("block/oak_planks"));
		basicBlock("chorus_flower", mc("block/chorus_flower"));
		basicBlock("comparator", mc("block/comparator_on"));
		basicBlock("dispenser", mc("block/dispenser_front"));
		basicBlock("enchantment_table", mc("block/enchanting_table_top"));
		basicBlock("end_gateway", mc("item/end_crystal"));
		basicBlock("ender_chest", mc("item/ender_eye"));
		basicBlock("fence_gate", mc("block/oak_planks"));
		basicBlock("fire", mc("block/fire_0"));
		basicBlock("furnace", mc("block/furnace_front_on"));
		basicBlock("iron_door", mc("item/iron_door"));
		basicBlock("iron_trapdoor", mc("block/iron_trapdoor"));
		basicBlock("lava", mc("block/lava_flow"));
		basicBlock("piston", mc("block/piston_side"));
		basicBlock("portal", mc("block/nether_portal"));
		basicBlock("redstone_torch", mc("block/redstone_torch"));
		basicBlock("tripwire", mc("block/tripwire_hook"));
		basicBlock("water", mc("block/water_flow"));
		basicBlock("lily_pad", mc("block/lily_pad"));
		basicBlock("wooden_door", mc("item/oak_door"));
		basicBlock("wooden_trapdoor", mc("block/oak_trapdoor"));

		basicBlockAction("metal_pressure_plate", mc("block/iron_block"));
		basicBlockAction("stone_button", mc("block/stone"));
		basicBlockAction("stone_pressure_plate", mc("block/stone"));
		basicBlockAction("wooden_button", mc("block/oak_planks"));
		basicBlockAction("wooden_pressure_plate", mc("block/oak_planks"));
		basicBlockAction("lever", mc("block/lever"));

		// legacy had no icon for other instruments
		put("block.note_block", icon(ob("note"), WHITE));
		put("block.note_block.basedrum", icon(ob("note"), 0x00FFFF));
		put("block.note_block.bass", icon(ob("note"), 0x0000FF));
		put("block.note_block.harp", icon(ob("note"), 0xFF0000));
		put("block.note_block.hat", icon(ob("note"), 0x00FF00));
		put("block.note_block.pling", icon(ob("note"), 0xFF00FF));
		put("block.note_block.snare", icon(ob("note"), WHITE));

		final Icon playerBack = icon(ob("player"));
		put("entity.player", framed(playerBack, WHITE_FRAME));
		singleIcon("entity.player.burp", mc("item/baked_potato"));
		singleIcon("entity.player.levelup", mc("item/experience_bottle"));
		put("entity.player.death", framed(layered(framed(ob("mob_death"), WHITE_FRAME), playerBack), RED_FRAME));
		put("entity.player.hurt", framed(layered(framed(ob("mob_hurt"), WHITE_FRAME), playerBack), RED_FRAME));

		singleIcon("entity.experience_orb", mc("item/experience_bottle"));
		singleIcon("entity.arrow", mc("item/arrow"));
		// legacy showed an experience bottle
		singleIcon("entity.egg", mc("item/egg"));
		singleIcon("entity.ender_eye", mc("item/ender_eye"));
		singleIcon("entity.ender_pearl", mc("item/ender_pearl"));
		singleIcon("entity.experience_bottle", mc("item/experience_bottle"));
		singleIcon("entity.firework_rocket", mc("item/firework_rocket"));
		put("entity.item", icon(ob("click")));
		singleIcon("entity.item_frame", mc("item/item_frame"));
		singleIcon("entity.leash_knot", mc("item/lead"));
		put("entity.lightning_bolt", icon(ob("click")));
		singleIcon("entity.lingering_potion", mc("item/lingering_potion"));
		singleIcon("entity.minecart", mc("item/minecart"));
		singleIcon("entity.painting", mc("item/painting"));
		singleIcon("entity.snowball", mc("item/snowball"));
		singleIcon("entity.splash_potion", mc("item/splash_potion"));
		singleIcon("entity.tnt", mc("block/tnt_side"));
		singleIcon("entity.armor_stand", mc("item/armor_stand"));
		singleIcon("entity.fishing_bobber", mc("item/fishing_rod_cast"));

		put("entity.generic", GENERIC);
		singleIcon("entity.generic.burn", mc("block/fire_0"));
		put("entity.generic.death", icon(ob("mob_death")));
		singleIcon("entity.generic.drink", mc("item/potion"));
		singleIcon("entity.generic.eat", mc("item/baked_potato"));
		put("entity.generic.extinguish_fire", icon(ob("fizz")));
		put("entity.generic.hurt", icon(ob("mob_hurt")));
		put("entity.generic.small_fall", icon(ob("mob_hurt")));
		put("entity.generic.splash", icon(ob("rain")));
		put("entity.generic.swim", icon(ob("fizz")));

		singleIcon("item.armor.equip_chain", mc("item/chainmail_chestplate"));
		singleIcon("item.armor.equip_diamond", mc("item/diamond_chestplate"));
		singleIcon("item.armor.equip_gold", mc("item/golden_chestplate"));
		singleIcon("item.armor.equip_iron", mc("item/iron_chestplate"));
		singleIcon("item.armor.equip_leather", mc("item/leather_chestplate"));
		singleIcon("item.armor.equip_netherite", mc("item/netherite_chestplate"));
		singleIcon("item.armor.equip_generic", mc("item/empty_armor_slot_chestplate"));

		singleIcon("item.bottle", mc("item/glass_bottle"));
		singleIcon("item.bucket", mc("item/bucket"));
		singleIcon("item.chorus_fruit", mc("item/popped_chorus_fruit"));
		singleIcon("item.elytra", mc("item/elytra"));
		singleIcon("item.firecharge", mc("item/fire_charge"));
		singleIcon("item.flintandsteel", mc("item/flint_and_steel"));
		singleIcon("item.hoe", mc("item/wooden_hoe"));
		singleIcon("item.shield", mc("item/empty_armor_slot_shield"));
		singleIcon("item.shovel", mc("item/wooden_shovel"));

		for (String disc : MUSIC_DISCS)
			put("music_disc." + disc, framed(mc("item/music_disc_" + disc), BLUE_FRAME));

		put("weather.rain", icon(ob("rain"), 0x0000FF));

		singleIcon("openblocks:elevator", mc("item/ender_pearl"));
		singleIcon("openblocks:luggage.eat.item", mc("item/apple"));
		singleIcon("openblocks:luggage.eat.food", mc("item/baked_potato"));
		singleIcon("openblocks:crayon.place", mc("item/writable_book"));
	}
}
