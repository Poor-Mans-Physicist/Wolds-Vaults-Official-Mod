package xyz.iwolfking.woldsvaults.client.lightning;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.client.hud.AbilityHud;

/**
 * While the player is lightning (Lightning Fiend 6-piece) the hotbar and the held item's name give way to the form's
 * own bar, in the hotbar's place: Arc Lash in the left slot, Thunderstrike in the middle and Lightning Rod in the
 * right, each with its key (and the lash and rod a cooldown veil like an item's), the lash's and rod's names (the
 * rod's target once it has one) and the time the form has left, which is also the Thunderstrike's power.
 */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightningFormHud {
    private static final ResourceLocation ARC_LASH = new ResourceLocation(WoldsVaults.MOD_ID, "textures/gui/lightning_form/arc_lash.png");
    private static final ResourceLocation LIGHTNING_ROD = new ResourceLocation(WoldsVaults.MOD_ID, "textures/gui/lightning_form/lightning_rod.png");
    private static final ResourceLocation THUNDERSTRIKE = new ResourceLocation(WoldsVaults.MOD_ID, "textures/gui/lightning_form/thunderstrike.png");
    private static final int TEXT = 0xC6F8FF;
    private static final int SET_COLOUR = 0x6FCFFF;

    private LightningFormHud() {
    }

    @SubscribeEvent
    public static void onHudLayer(RenderGameOverlayEvent.PreLayer event) {
        if (ClientLightningForm.isActive()) {
            AbilityHud.replaceHotbar(event, LightningFormHud::draw);
        }
    }

    private static void draw(AbilityHud hud) {
        Minecraft mc = hud.mc;
        float partialTick = hud.partialTick;
        hud.panel();
        hud.cooldownSlot(AbilityHud.Slot.LEFT, ARC_LASH, ClientLightningForm.lashCooldown(partialTick), mc.options.keyAttack);
        hud.cooldownSlot(AbilityHud.Slot.MIDDLE, THUNDERSTRIKE, 0.0F, mc.options.keyPickItem);
        hud.cooldownSlot(AbilityHud.Slot.RIGHT, LIGHTNING_ROD, ClientLightningForm.rodCooldown(partialTick), mc.options.keyUse);

        // the names; the rod's turns to its target's once it has one
        hud.labelLeft("Arc Lash", TEXT);
        Entity marked = mc.level == null || ClientLightningForm.markId() < 0 ? null : mc.level.getEntity(ClientLightningForm.markId());
        String rod = marked == null ? "Lightning Rod" : mc.font.plainSubstrByWidth(marked.getDisplayName().getString(), hud.labelRoom());
        hud.labelRight(rod, marked == null ? TEXT : SET_COLOUR);

        hud.timeBar(ClientLightningForm.formLeft(partialTick), 0xFF060B17, 0xFF2BA6D4, 0xFFC6F8FF);
    }
}
