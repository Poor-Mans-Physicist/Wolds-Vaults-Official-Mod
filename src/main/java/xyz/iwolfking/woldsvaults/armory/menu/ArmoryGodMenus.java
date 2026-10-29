package xyz.iwolfking.woldsvaults.armory.menu;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.armory.ArmoryViewInfo;
import xyz.iwolfking.woldsvaults.armory.block.ArmoryBlockEntity;

/** The read-only Gods view menu for a saved armory page. */
public final class ArmoryGodMenus {
    private ArmoryGodMenus() {
    }

    public static void register(RegistryEvent.Register<MenuType<?>> event) {
    }

    /** Opens the saved page's god constellations. */
    public static void open(ServerPlayer player, ArmoryBlockEntity armory, ArmoryViewInfo info) {
        WoldsVaults.LOGGER.warn("Armory: the Gods view is not available yet");
        player.displayClientMessage(new TextComponent("This armory view is not available yet."), true);
    }
}
