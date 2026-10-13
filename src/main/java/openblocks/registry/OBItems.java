package openblocks.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.item.XpBucketItem;

public class OBItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OpenBlocks.MODID);

	public static final DeferredItem<XpBucketItem> XP_BUCKET = ITEMS.register("xp_bucket", () -> new XpBucketItem(OBFluids.XP_JUICE.get(), new Item.Properties()
			.craftRemainder(Items.BUCKET)
			.stacksTo(1)));
}
