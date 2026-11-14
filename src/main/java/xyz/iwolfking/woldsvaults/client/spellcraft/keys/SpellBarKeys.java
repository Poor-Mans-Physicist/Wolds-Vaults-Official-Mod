package xyz.iwolfking.woldsvaults.client.spellcraft.keys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.client.spellcraft.bar.SkillMenuSpellBar;
import xyz.iwolfking.woldsvaults.client.spellcraft.state.ClientGlyphViews;
import xyz.iwolfking.woldsvaults.client.spellcraft.state.ClientSpellBar;
import xyz.iwolfking.woldsvaults.init.ModNetwork;
import xyz.iwolfking.woldsvaults.network.packets.SpellSlotKeyMessage;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;
import xyz.iwolfking.woldsvaults.spellcraft.play.StatMirror;

import java.util.Arrays;

/**
 * The 16 spell slot keys, each sending a press when it goes down and a release when it comes up. Opening a screen
 * releases any held slot. The client tick also retires Vault Hunters' quick-fire keys and counts cooldowns down, and
 * logging out clears the client spellcraft caches.
 */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, value = Dist.CLIENT)
public final class SpellBarKeys {
    public static final KeyMapping[] KEYS = new KeyMapping[SpellcraftItemData.SLOTS];
    private static final boolean[] DOWN = new boolean[SpellcraftItemData.SLOTS];

    private SpellBarKeys() {
    }

    public static void register() {
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i] = new SlotKeyMapping(i, "key." + WoldsVaults.MOD_ID + ".spell_slot_" + (i + 1),
                    "key.category." + WoldsVaults.MOD_ID + ".spell_bar");
            ClientRegistry.registerKeyBinding(KEYS[i]);
        }
    }

    /** A slot key that sorts by slot number in the Controls list, so 2 comes before 10. */
    private static final class SlotKeyMapping extends KeyMapping {
        private final int slot;

        SlotKeyMapping(int slot, String name, String category) {
            super(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
            this.slot = slot;
        }

        @Override
        public int compareTo(KeyMapping other) {
            if (other instanceof SlotKeyMapping slotKey) {
                return Integer.compare(this.slot, slotKey.slot);
            }
            return super.compareTo(other);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || KEYS[0] == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!QuickfireKeys.retired() && minecraft.options != null) {
            QuickfireKeys.hide(minecraft);
        }
        if (minecraft.player != null && !minecraft.isPaused()) {
            ClientCooldownTicker.tick();
        }
        SkillMenuSpellBar.forgetClosed();
        boolean active = minecraft.player != null && minecraft.screen == null && minecraft.getConnection() != null;
        for (int i = 0; i < KEYS.length; i++) {
            boolean down = active && KEYS[i].isDown();
            if (down != DOWN[i]) {
                DOWN[i] = down;
                if (minecraft.getConnection() != null) {
                    ModNetwork.sendToServer(new SpellSlotKeyMessage(i, down));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        StatMirror.clearClient();
        Arrays.fill(DOWN, false);
        ClientSpellBar.reset();
        ClientGlyphViews.reset();
        SkillMenuSpellBar.reset();
    }
}
