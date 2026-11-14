package xyz.iwolfking.woldsvaults.client.spellcraft.gui;

import com.mojang.blaze3d.platform.InputConstants;
import iskallia.vault.skill.base.Skill;
import iskallia.vault.skill.base.SpecializedSkill;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import xyz.iwolfking.woldsvaults.client.spellcraft.keys.SpellBarKeys;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;

import javax.annotation.Nullable;

public final class SpellcraftNames {
    private static final String LANG = "woldsvaults.spellcraft.";

    private SpellcraftNames() {
    }

    /** A translated UI string under {@code woldsvaults.spellcraft.}. */
    public static String tr(String key, Object... args) {
        return I18n.get(LANG + key, args);
    }

    /**
     * A short label for a spell slot key, at most {@code room} font pixels wide: mouse buttons as {@code M4}, other keys
     * by their name, cut to fit; empty when unbound.
     */
    public static String keyLabel(int slot, Font font, int room) {
        KeyMapping key = SpellBarKeys.KEYS[slot];
        if (key == null || key.isUnbound()) {
            return "";
        }
        String label = key.getKey().getType() == InputConstants.Type.MOUSE
                ? "M" + (key.getKey().getValue() + 1)
                : key.getTranslatedKeyMessage().getString();
        return font.plainSubstrByWidth(label, room);
    }

    /** The key's full name for tooltips, or empty when unbound. */
    public static String keyName(int slot) {
        KeyMapping key = SpellBarKeys.KEYS[slot];
        if (key == null || key.isUnbound()) {
            return "";
        }
        return key.getTranslatedKeyMessage().getString();
    }

    /** An ability group as players read it: its current specialization's name, else the id with spaces. */
    public static String abilityName(@Nullable AbilityTree tree, String group) {
        if (tree != null) {
            Skill skill = tree.getForId(group).orElse(null);
            if (skill instanceof SpecializedSkill specialized && specialized.getSpecialization() != null
                    && specialized.getSpecialization().getName() != null) {
                return specialized.getSpecialization().getName();
            }
        }
        return AbilityLookup.groupName(group);
    }

    /** The group's own name (no specialization), for compact lists such as "Stonefall + Nova". */
    public static String groupName(@Nullable AbilityTree tree, String group) {
        if (tree != null) {
            Skill skill = tree.getForId(group).orElse(null);
            if (skill != null && skill.getName() != null) {
                return skill.getName();
            }
        }
        return AbilityLookup.groupName(group);
    }

    /** The text with its first letter upper-cased, skipping leading colour tags, signs and digits. */
    public static String capitalize(String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '<') {
                int close = text.indexOf('>', i);
                if (close < 0) {
                    break;
                }
                i = close + 1;
                continue;
            }
            if (Character.isLetter(c)) {
                return text.substring(0, i) + Character.toUpperCase(c) + text.substring(i + 1);
            }
            if (Character.isDigit(c) || c == '+' || c == '-' || c == ' ' || c == '%' || c == '.') {
                return text;
            }
            i++;
        }
        return text;
    }

    public static MutableComponent colored(String text, int color) {
        return new TextComponent(text).withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
    }
}
