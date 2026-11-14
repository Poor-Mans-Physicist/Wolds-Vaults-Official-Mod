package xyz.iwolfking.woldsvaults.spellcraft.play;

import iskallia.vault.skill.base.Skill;
import iskallia.vault.skill.base.SpecializedSkill;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.server.level.ServerPlayer;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;
import xyz.iwolfking.woldsvaults.spellcraft.bar.SpellBarData;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;

import javax.annotation.Nullable;

public final class DefaultBar {
    private DefaultBar() {
    }

    static void migrate(ServerPlayer player, SpellBarData.PlayerBar saved, AbilityTree tree) {
        int slot = 0;
        for (Skill skill : tree.skills) {
            if (slot >= SpellcraftItemData.SLOTS) {
                break;
            }
            if (skill instanceof SpecializedSkill specialized && specialized.isUnlocked()) {
                saved.defaults[slot] = specialized.getId();
                if (tree.getSelected() != null && tree.getSelected().getId().equals(specialized.getId())) {
                    saved.selected = slot;
                }
                slot++;
            }
        }
        saved.migrated = true;
        SpellBarData.get(player.getServer()).setDirty();
    }

    /** Sets a default-bar slot (empty string clears it); the ability moves if it was on another default slot. */
    @Nullable
    public static String editDefault(ServerPlayer player, int index, String group) {
        if (index < 0 || index >= SpellcraftItemData.SLOTS) {
            return "no such slot";
        }
        if (!group.isEmpty() && AbilityLookup.specOf(AbilityLookup.tree(player)).apply(group) == null) {
            return group + " is not an ability";
        }
        SpellBarData.PlayerBar saved = SpellBars.saved(player);
        if (!group.isEmpty()) {
            for (int i = 0; i < saved.defaults.length; i++) {
                if (group.equals(saved.defaults[i])) {
                    saved.defaults[i] = "";
                }
            }
        }
        saved.defaults[index] = group;
        SpellBarData.get(player.getServer()).setDirty();
        SpellBars.refresh(player);
        return null;
    }
}
