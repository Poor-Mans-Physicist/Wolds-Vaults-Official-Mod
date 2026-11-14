package xyz.iwolfking.woldsvaults.spellcraft.spell;

/** How strictly a spell is read. */
public enum ReadMode {
    /** Casting and display: only structural problems are errors; glyphs the view can't back go inactive. */
    LENIENT,
    /** Every glyph must be backed by the view, within its limit, and fit its step. */
    STRICT,
    /**
     * Editing: only copies the saved spell already held may lack a line, exceed the limit or not fit their step
     * (they stay, inactive); new copies must come from the view and fit.
     */
    STRICT_NEW
}
