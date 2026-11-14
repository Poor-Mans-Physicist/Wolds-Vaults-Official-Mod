package xyz.iwolfking.woldsvaults.client.spellcraft.editor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;

/**
 * Right-clicking a focus or wand with spellcraft in a player-inventory slot of any container screen, with nothing on
 * the cursor, opens the focus spellcraft screen for that inventory slot. The creative inventory is skipped: its slots
 * don't map to player inventory indices.
 */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, value = Dist.CLIENT)
public final class SpellcraftScreenOpener {
    private SpellcraftScreenOpener() {
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseClickedEvent.Pre event) {
        if (event.getButton() != 1 || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !(slot.container instanceof Inventory) || !screen.getMenu().getCarried().isEmpty()) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (!SpellcraftItemData.canCarry(stack) || !SpellcraftItemData.has(stack)) {
            return;
        }
        event.setCanceled(true);
        SpellcraftScreen.open(screen, slot.getContainerSlot());
    }
}
