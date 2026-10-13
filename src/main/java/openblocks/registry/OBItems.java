package openblocks.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.item.HangGliderItem;
import openblocks.item.SleepingBagItem;
import openblocks.item.TankBlockItem;
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
}
