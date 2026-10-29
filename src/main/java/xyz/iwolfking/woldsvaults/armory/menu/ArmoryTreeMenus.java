package xyz.iwolfking.woldsvaults.armory.menu;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.armory.ArmoryTab;
import xyz.iwolfking.woldsvaults.armory.ArmoryViewInfo;
import xyz.iwolfking.woldsvaults.armory.block.ArmoryBlockEntity;

/** Read-only tree view menus (Abilities, Talents, Expertises, Prestige, Gods) for a saved armory page. */
public final class ArmoryTreeMenus {
    private ArmoryTreeMenus() {
    }

    public static void register(RegistryEvent.Register<MenuType<?>> event) {
    }

    /** Opens the tree tab named by {@code info.tab()} for the page {@code info.page()}. */
    public static void open(ServerPlayer player, ArmoryBlockEntity armory, ArmoryViewInfo info) {
        if (info.tab() == ArmoryTab.GODS) {
            ArmoryGodMenus.open(player, armory, info);
            return;
        }
        WoldsVaults.LOGGER.warn("Armory: the {} view is not available yet", info.tab());
        player.displayClientMessage(new TextComponent("This armory view is not available yet."), true);
    }
}
