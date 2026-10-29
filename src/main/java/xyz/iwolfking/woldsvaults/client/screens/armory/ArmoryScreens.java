package xyz.iwolfking.woldsvaults.client.screens.armory;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.iwolfking.woldsvaults.armory.menu.ArmoryGodMenus;

/** Registers the armory's screens and block render layers. */
@OnlyIn(Dist.CLIENT)
public final class ArmoryScreens {
    private ArmoryScreens() {
    }

    public static void register() {
        MenuScreens.register(ArmoryGodMenus.ARMORY_GODS, ArmoryGodTreeScreen::new);
    }
}
