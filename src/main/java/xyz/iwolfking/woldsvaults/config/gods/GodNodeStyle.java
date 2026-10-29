package xyz.iwolfking.woldsvaults.config.gods;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.entry.SkillStyle;

// One god tree star's style: the base mod's SkillStyle plus how large the star is drawn. A size of 0, or none
// in the file, draws the star at its type's default size.
public class GodNodeStyle extends SkillStyle {
    @Expose public int size;

    public GodNodeStyle() {
        super(0, 0, null);
    }
}
