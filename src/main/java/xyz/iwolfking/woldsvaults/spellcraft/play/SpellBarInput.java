package xyz.iwolfking.woldsvaults.spellcraft.play;

import iskallia.vault.skill.base.SkillContext;
import iskallia.vault.skill.base.SpecializedSkill;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.server.level.ServerPlayer;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;
import xyz.iwolfking.woldsvaults.spellcraft.bar.EffectiveBar;
import xyz.iwolfking.woldsvaults.spellcraft.bar.SpellBarData;
import xyz.iwolfking.woldsvaults.spellcraft.cast.SpellMessages;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;

import javax.annotation.Nullable;
import java.util.Map;

public final class SpellBarInput {
    private SpellBarInput() {
    }

    /** A spell slot key was pressed or released: select that slot, then cast it. */
    public static void onSlotKey(ServerPlayer player, int index, boolean down) {
        if (index < 0 || index >= SpellcraftItemData.SLOTS || player.isSpectator()) {
            return;
        }
        EffectiveBar bar = SpellBars.refresh(player);
        SpellBarData.PlayerBar saved = SpellBars.saved(player);
        if (bar.slot(index).kind() != EffectiveBar.Kind.EMPTY && saved.selected != index) {
            SpellBars.select(player, saved, index);
            bar = SpellBars.refresh(player);
        }
        press(player, bar, index, down);
    }

    /** VH's cast key: casts the selected slot. Returns true when handled (VH's own handling is skipped). */
    public static boolean onTreeKey(ServerPlayer player, boolean down) {
        if (SpellBars.isDispatching()) {
            return false;
        }
        EffectiveBar bar = SpellBars.refresh(player);
        press(player, bar, SpellBars.saved(player).selected, down);
        return true;
    }

    /**
     * VH's quick-select or wheel picked an ability. Selects the slot that casts it and returns false so VH completes
     * the selection; returns true (cancel) when the ability isn't on the bar.
     */
    public static boolean onQuickSelect(ServerPlayer player, String group) {
        if (SpellBars.isDispatching()) {
            return false;
        }
        EffectiveBar bar = SpellBars.refresh(player);
        int index = bar.slotOf(group);
        if (index < 0) {
            SpellMessages.show(player, AbilityLookup.groupName(group) + " isn't on your spell bar");
            return true;
        }
        SpellBarData.PlayerBar saved = SpellBars.saved(player);
        if (saved.selected != index) {
            SpellBars.select(player, saved, index);
        }
        return false;
    }

    /** Scrolling with the cast key held moves the selected slot through filled slots. */
    public static void onScroll(ServerPlayer player, int delta) {
        EffectiveBar bar = SpellBars.refresh(player);
        SpellBarData.PlayerBar saved = SpellBars.saved(player);
        int index = saved.selected;
        int direction = delta > 0 ? 1 : -1;
        for (int i = 0; i < Math.abs(delta); i++) {
            int next = SpellBars.firstFilled(bar, index, direction);
            if (next < 0) {
                return;
            }
            index = next;
        }
        SpellBars.select(player, saved, index);
        SpellBars.refresh(player);
    }

    private static void press(ServerPlayer player, EffectiveBar bar, int index, boolean down) {
        EffectiveBar.Slot slot = bar.slot(index);
        if (slot.kind() != EffectiveBar.Kind.EMPTY && HeldSlots.repeats(player, slot)) {
            Map<Integer, Integer> held = HeldSlots.held(player);
            if (down) {
                castOnce(player, bar, slot);
                held.put(index, HeldSlots.nextAttempt(player, slot));
            } else {
                held.remove(index);
            }
            return;
        }
        switch (slot.kind()) {
            case EMPTY -> {
                if (!down) {
                    SpellMessages.show(player, slot.hidden()
                            ? "Slot " + (index + 1) + " is hidden: another slot casts " + AbilityLookup.groupName(slot.group())
                            : "Slot " + (index + 1) + " is empty");
                }
            }
            case ABILITY -> {
                AbilityTree tree = selectedTree(player, slot);
                if (tree == null) {
                    if (!down) {
                        SpellMessages.show(player, AbilityLookup.groupName(slot.group()) + " is not learned");
                    }
                    return;
                }
                SkillContext context = SkillContext.of(player);
                SpellBars.dispatch(() -> {
                    if (down) {
                        tree.onKeyDown(context);
                    } else {
                        tree.onKeyUp(context);
                    }
                });
            }
            case SPELL -> SpellTrigger.onSpellKey(player, bar, slot, down);
        }
    }

    static void castOnce(ServerPlayer player, EffectiveBar bar, EffectiveBar.Slot slot) {
        if (slot.kind() == EffectiveBar.Kind.SPELL) {
            SpellTrigger.onSpellKey(player, bar, slot, false);
            return;
        }
        AbilityTree tree = selectedTree(player, slot);
        if (tree == null) {
            return;
        }
        SkillContext context = SkillContext.of(player);
        SpellBars.dispatch(() -> {
            tree.onKeyDown(context);
            tree.onKeyUp(context);
        });
    }

    /** The player's tree once its selection follows {@code slot}, or null when the slot's ability did not become selected. */
    @Nullable
    private static AbilityTree selectedTree(ServerPlayer player, EffectiveBar.Slot slot) {
        AbilityTree tree = AbilityLookup.tree(player);
        SpellBars.followSelection(player, tree, slot);
        SpecializedSkill selected = tree.getSelected();
        if (selected == null || !selected.getId().equals(slot.group())) {
            return null;
        }
        return tree;
    }
}
