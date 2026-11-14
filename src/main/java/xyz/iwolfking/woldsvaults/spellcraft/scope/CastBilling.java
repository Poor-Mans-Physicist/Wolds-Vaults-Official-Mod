package xyz.iwolfking.woldsvaults.spellcraft.scope;

import xyz.iwolfking.woldsvaults.spellcraft.bar.EffectiveBar;

import javax.annotation.Nullable;

/**
 * How mana and cooldown are charged inside a slot cast. The trigger step charges the whole slot (the mana of every
 * ability in the spell times the spell's mana factor, and the slot cooldown); every other step is free, since the
 * slot already paid. {@code suppressCastEvent} keeps a step from firing on-cast effects (Shotgun copies).
 * {@code fullyCharged}: the slot was cast with Charge held to full.
 */
public record CastBilling(Mode mode, @Nullable String triggerSpec, float otherAbilitiesMana, float manaFactor,
                          @Nullable EffectiveBar.Slot slot, boolean suppressCastEvent, boolean fullyCharged) {
    public static CastBilling trigger(String triggerSpec, float otherAbilitiesMana, float manaFactor, EffectiveBar.Slot slot,
                                      boolean fullyCharged) {
        return new CastBilling(Mode.TRIGGER, triggerSpec, otherAbilitiesMana, manaFactor, slot, false, fullyCharged);
    }

    public static CastBilling free(boolean suppressCastEvent) {
        return new CastBilling(Mode.FREE, null, 0.0F, 1.0F, null, suppressCastEvent, false);
    }

    public enum Mode {
        TRIGGER,
        FREE
    }
}
