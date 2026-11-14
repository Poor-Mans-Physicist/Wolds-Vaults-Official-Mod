package xyz.iwolfking.woldsvaults.spellcraft;

import xyz.iwolfking.woldsvaults.spellcraft.play.SpellCharge;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectileGlyphs;
import xyz.iwolfking.woldsvaults.spellcraft.roll.SpellcraftRoller;
import xyz.iwolfking.woldsvaults.spellcraft.buff.LingeringBuffs;
import xyz.iwolfking.woldsvaults.spellcraft.gear.ConvertedEtchings;
import xyz.iwolfking.woldsvaults.spellcraft.gear.ConvertedAffixes;
import xyz.iwolfking.woldsvaults.spellcraft.roll.UniqueSetRoller;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityCastTracker;
import net.minecraftforge.common.MinecraftForge;
import xyz.iwolfking.woldsvaults.api.core.WoldLogger;
import xyz.iwolfking.woldsvaults.spellcraft.buff.ToggleDiscounts;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectileSteering;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectileSizing;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectilePhysics;
import xyz.iwolfking.woldsvaults.spellcraft.scope.DeferredReach;
import xyz.iwolfking.woldsvaults.spellcraft.buff.BuffGlyphs;
import xyz.iwolfking.woldsvaults.spellcraft.cast.SpellScheduler;
import xyz.iwolfking.woldsvaults.spellcraft.hit.ShotgunHits;
import xyz.iwolfking.woldsvaults.spellcraft.hit.GlyphArea;
import xyz.iwolfking.woldsvaults.spellcraft.hit.GlyphDamage;

/**
 * Spellcraft: focuses and wands roll arcana, glyph lines and depth; their owner builds augmented spells from the glyphs
 * the item's view offers and gives each spell a bar slot and a priority. While the item is equipped its spells overlay
 * those slots of the default bar. This class holds the entry points the bar and the focus screen call.
 */
public final class Spellcraft {
    private Spellcraft() {
    }

    /**
     * Binds the cast-effect handlers at setup, in this order, instead of from annotated subscribers, so they run after
     * every annotated handler of the same priority: elemental procs read a hit before glyph damage scales it.
     */
    public static void registerHandlers() {
        MinecraftForge.EVENT_BUS.register(ToggleDiscounts.class);
        MinecraftForge.EVENT_BUS.register(ProjectileSteering.class);
        MinecraftForge.EVENT_BUS.register(ProjectileSizing.class);
        MinecraftForge.EVENT_BUS.register(ProjectilePhysics.class);
        MinecraftForge.EVENT_BUS.register(DeferredReach.class);
        MinecraftForge.EVENT_BUS.register(BuffGlyphs.class);
        MinecraftForge.EVENT_BUS.register(ProjectileGlyphs.class);
        MinecraftForge.EVENT_BUS.register(GlyphDamage.class);
        MinecraftForge.EVENT_BUS.register(SpellScheduler.class);
        MinecraftForge.EVENT_BUS.register(SpellCharge.class);
        MinecraftForge.EVENT_BUS.register(ShotgunHits.class);
    }

    /** Registers the VH event listeners; called once from common setup. */
    public static void init() {
        try {
            AbilityCastTracker.register();
            GlyphArea.register();
            ConvertedAffixes.init();
            ConvertedEtchings.init();
            LingeringBuffs.init();
            SpellcraftRoller.setUniqueSets(new UniqueSetRoller());
        } catch (RuntimeException e) {
            WoldLogger.error("Spellcraft setup failed; any of its VH hooks after the failing one may be missing (cast tracking, glyph areas, "
                    + "converted affixes, converted etchings, lingering buffs, unique set rolls).", e);
            throw e;
        }
    }
}
