package xyz.iwolfking.woldsvaults.spellcraft.spell;

import xyz.iwolfking.woldsvaults.spellcraft.glyph.GlyphType;

/**
 * One placed glyph with its view line's potency, quality and cost, its state, and its token index in the spell.
 * {@code dependent}: it would act, but the Bundled Spell it rides on is inactive ({@code state} is that glyph's).
 */
public record PlacedGlyph(GlyphType type, float potency, float quality, int cost, GlyphState state, boolean dependent, int token) {
    public boolean active() {
        return this.state == GlyphState.ACTIVE;
    }

    boolean potential() {
        return this.state != GlyphState.DORMANT;
    }
}
