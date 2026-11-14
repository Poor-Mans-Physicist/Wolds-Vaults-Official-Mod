package xyz.iwolfking.woldsvaults.spellcraft.play;

import iskallia.vault.mana.ManaPlayer;
import iskallia.vault.skill.ability.effect.spi.core.Ability;
import iskallia.vault.skill.ability.effect.spi.core.InstantAbility;
import iskallia.vault.skill.ability.effect.spi.core.InstantManaAbility;
import iskallia.vault.util.calc.ManaCostHelper;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.server.level.ServerPlayer;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;
import xyz.iwolfking.woldsvaults.spellcraft.bar.EffectiveBar;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class HeldSlots {
    private static final int RETRY_TICKS = 10;
    private static final Map<UUID, Map<Integer, Integer>> HELD = new HashMap<>();

    private HeldSlots() {
    }

    static Map<Integer, Integer> held(ServerPlayer player) {
        return HELD.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
    }

    static void cancelChanged(ServerPlayer player, @Nullable String[] previous, String[] keys) {
        Map<Integer, Integer> held = HELD.get(player.getUUID());
        for (int i = 0; i < keys.length; i++) {
            if (previous != null && held != null && !keys[i].equals(previous[i])) {
                held.remove(i);
            }
        }
    }

    /**
     * Whether a held key keeps recasting this slot: instant abilities and instant spells without Charge. Toggles,
     * holds and Charge spells keep their own press and release handling.
     */
    static boolean repeats(ServerPlayer player, EffectiveBar.Slot slot) {
        Ability trigger = AbilityLookup.ability(AbilityLookup.tree(player), slot.group());
        if (!(trigger instanceof InstantAbility) || slot.locked()) {
            return false;
        }
        return slot.kind() == EffectiveBar.Kind.ABILITY || (slot.model() != null && !slot.model().hasCharge());
    }

    /**
     * Recasts held slots on the tick they are ready: the tick the last of their groups' cooldowns ends (VH ticks
     * cooldowns at the start of the player tick, this runs at its end) when the slot is affordable. An attempt that does not start the cooldown
     * (a curse, a suppressor, a failed cast) waits {@link #RETRY_TICKS} before the next one.
     */
    static void recast(ServerPlayer player) {
        Map<Integer, Integer> held = HELD.get(player.getUUID());
        if (held == null || held.isEmpty()) {
            return;
        }
        EffectiveBar bar = SpellBars.current(player);
        if (bar == null) {
            return;
        }
        AbilityTree tree = AbilityLookup.tree(player);
        for (Map.Entry<Integer, Integer> entry : new ArrayList<>(held.entrySet())) {
            EffectiveBar.Slot slot = bar.slot(entry.getKey());
            if (slot.kind() == EffectiveBar.Kind.EMPTY || !repeats(player, slot)) {
                held.remove(entry.getKey());
                continue;
            }
            Ability trigger = AbilityLookup.ability(tree, slot.group());
            if (player.tickCount < entry.getValue() || trigger == null || SpellCooldowns.blocking(tree, slot.groups()) != null
                    || !canAfford(player, tree, slot)) {
                continue;
            }
            SpellBarInput.castOnce(player, bar, slot);
            held.put(entry.getKey(), nextAttempt(player, slot));
        }
    }

    static int nextAttempt(ServerPlayer player, EffectiveBar.Slot slot) {
        return SpellCooldowns.blocking(AbilityLookup.tree(player), slot.groups()) != null ? 0 : player.tickCount + RETRY_TICKS;
    }

    private static boolean canAfford(ServerPlayer player, AbilityTree tree, EffectiveBar.Slot slot) {
        if (player.isCreative()) {
            return true;
        }
        float cost = 0.0F;
        for (String group : slot.model() != null ? slot.model().castGroups() : slot.groups()) {
            if (AbilityLookup.ability(tree, group) instanceof InstantManaAbility mana) {
                cost += ManaCostHelper.adjustManaCost(player, mana, mana.getManaCost());
            }
        }
        if (slot.model() != null) {
            cost *= slot.model().manaFactor();
        }
        return ((ManaPlayer) player).getMana() >= cost;
    }

    static void forget(UUID player) {
        HELD.remove(player);
    }

    static void clear() {
        HELD.clear();
    }
}
