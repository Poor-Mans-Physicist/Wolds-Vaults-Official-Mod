package xyz.iwolfking.woldsvaults.client.spellcraft.keys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import iskallia.vault.init.ModKeybinds;
import java.util.ArrayList;
import java.util.List;
import xyz.iwolfking.woldsvaults.mixins.accessors.OptionsKeyMappingsAccessor;

final class QuickfireKeys {
    private static boolean retired;

    private QuickfireKeys() {
    }

    static boolean retired() {
        return retired;
    }

    /**
     * Retires VH's per-ability quick-fire keys: they are unbound and taken out of the Controls list, since abilities
     * are cast from spell slots now (the server ignores their packet too).
     */
    static void hide(Minecraft minecraft) {
        retired = true;
        List<KeyMapping> kept = new ArrayList<>();
        int hidden = 0;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (ModKeybinds.QUICKFIRE_CATEGORY.equals(mapping.getCategory())) {
                mapping.setKey(InputConstants.UNKNOWN);
                hidden++;
            } else {
                kept.add(mapping);
            }
        }
        if (hidden > 0) {
            KeyMapping.resetMapping();
            ((OptionsKeyMappingsAccessor) minecraft.options)
                    .woldsvaults$setKeyMappings(kept.toArray(new KeyMapping[0]));
        }
    }
}
