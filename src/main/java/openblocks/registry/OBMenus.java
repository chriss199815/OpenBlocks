package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.menu.XpBottlerMenu;

public class OBMenus {

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OpenBlocks.MODID);

	public static final DeferredHolder<MenuType<?>, MenuType<XpBottlerMenu>> XP_BOTTLER = MENUS.register("xp_bottler", () -> IMenuTypeExtension.create(XpBottlerMenu::new));
}
