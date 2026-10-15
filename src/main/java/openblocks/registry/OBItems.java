package openblocks.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.common.StencilPattern;
import openblocks.item.CursorItem;
import openblocks.item.DevNullItem;
import openblocks.item.GoldenEyeItem;
import openblocks.item.HangGliderItem;
import openblocks.item.LuggageItem;
import openblocks.item.PaintBrushItem;
import openblocks.item.PaintCanItem;
import openblocks.item.PedometerItem;
import openblocks.item.PointerItem;
import openblocks.item.SleepingBagItem;
import openblocks.item.SlimalyzerItem;
import openblocks.item.SpongeOnAStickItem;
import openblocks.item.SqueegeeItem;
import openblocks.item.StencilItem;
import openblocks.item.TankBlockItem;
import openblocks.item.TastyClayItem;
import openblocks.item.TrophyItem;
import openblocks.item.WrenchItem;
import openblocks.item.XpBucketItem;

public class OBItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OpenBlocks.MODID);

	public static final DeferredItem<XpBucketItem> XP_BUCKET = ITEMS.register("xp_bucket", () -> new XpBucketItem(OBFluids.XP_JUICE.get(), new Item.Properties()
			.craftRemainder(Items.BUCKET)
			.stacksTo(1)));

	public static final DeferredItem<Item> GLIDER_WING = ITEMS.registerSimpleItem("glider_wing");
	public static final DeferredItem<HangGliderItem> HANG_GLIDER = ITEMS.register("hang_glider", () -> new HangGliderItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<SleepingBagItem> SLEEPING_BAG = ITEMS.register("sleeping_bag", () -> new SleepingBagItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<TankBlockItem> TANK = ITEMS.register("tank", () -> new TankBlockItem(OBBlocks.TANK.get(), new Item.Properties()));

	public static final DeferredItem<PaintCanItem> PAINT_CAN = ITEMS.register("paint_can", () -> new PaintCanItem(OBBlocks.PAINT_CAN.get(), new Item.Properties().stacksTo(1)));
	public static final DeferredItem<PaintBrushItem> PAINT_BRUSH = ITEMS.register("paintbrush", () -> new PaintBrushItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<LuggageItem> LUGGAGE = ITEMS.register("luggage", () -> new LuggageItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<DevNullItem> DEV_NULL = ITEMS.register("dev_null", () -> new DevNullItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<GoldenEyeItem> GOLDEN_EYE = ITEMS.register("golden_eye", () -> new GoldenEyeItem(new Item.Properties().durability(GoldenEyeItem.MAX_DAMAGE)));

	// legacy MetasGenericUnstackable
	public static final DeferredItem<PointerItem> POINTER = ITEMS.register("pointer", () -> new PointerItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<CursorItem> CURSOR = ITEMS.register("cursor", () -> new CursorItem(new Item.Properties().durability(CursorItem.DURABILITY)));

	public static final DeferredItem<SqueegeeItem> SQUEEGEE = ITEMS.register("squeegee", () -> new SqueegeeItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<TrophyItem> TROPHY = ITEMS.register("trophy", () -> new TrophyItem(OBBlocks.TROPHY.get(), new Item.Properties()));

	public static final DeferredItem<WrenchItem> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<PedometerItem> PEDOMETER = ITEMS.register("pedometer", () -> new PedometerItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<SlimalyzerItem> SLIMALYZER = ITEMS.register("slimalyzer", () -> new SlimalyzerItem(new Item.Properties().stacksTo(1)));

	// durability from the config, see SpongeOnAStickItem.setDurability
	public static final DeferredItem<SpongeOnAStickItem> SPONGE_ON_A_STICK = ITEMS.register("sponge_on_a_stick",
			() -> new SpongeOnAStickItem(new Item.Properties().durability(SpongeOnAStickItem.DEFAULT_DURABILITY)));

	public static final DeferredItem<TastyClayItem> TASTY_CLAY = ITEMS.register("tasty_clay", () -> new TastyClayItem(new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).alwaysEdible().build())));

	// legacy MetasGeneric
	public static final DeferredItem<Item> UNPREPARED_STENCIL = ITEMS.registerSimpleItem("unprepared_stencil");
	public static final DeferredItem<Item> SKETCHING_PENCIL = ITEMS.registerSimpleItem("sketching_pencil");
	// crafting parts of features that are not ported yet (crane backpack, cartographer, projector)
	public static final DeferredItem<Item> BEAM = ITEMS.registerSimpleItem("beam");
	public static final DeferredItem<Item> CRANE_ENGINE = ITEMS.registerSimpleItem("crane_engine");
	public static final DeferredItem<Item> CRANE_MAGNET = ITEMS.registerSimpleItem("crane_magnet");
	public static final DeferredItem<Item> LINE = ITEMS.registerSimpleItem("line");
	public static final DeferredItem<Item> MAP_CONTROLLER = ITEMS.registerSimpleItem("map_controller");
	public static final DeferredItem<Item> MAP_MEMORY = ITEMS.registerSimpleItem("map_memory");
	public static final DeferredItem<Item> ASSISTANT_BASE = ITEMS.registerSimpleItem("assistant_base");

	public static final Map<StencilPattern, DeferredItem<StencilItem>> STENCILS = registerStencils();

	private static Map<StencilPattern, DeferredItem<StencilItem>> registerStencils() {
		final Map<StencilPattern, DeferredItem<StencilItem>> result = new EnumMap<>(StencilPattern.class);
		for (StencilPattern pattern : StencilPattern.values())
			result.put(pattern, ITEMS.register(StencilItem.itemId(pattern), () -> new StencilItem(pattern, new Item.Properties())));
		return Collections.unmodifiableMap(result);
	}
}
