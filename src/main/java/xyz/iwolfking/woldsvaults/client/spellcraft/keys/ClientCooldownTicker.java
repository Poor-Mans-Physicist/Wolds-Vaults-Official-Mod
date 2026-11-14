package xyz.iwolfking.woldsvaults.client.spellcraft.keys;

import iskallia.vault.client.data.ClientAbilityData;
import iskallia.vault.skill.ability.effect.spi.core.Ability;
import iskallia.vault.skill.tree.AbilityTree;

final class ClientCooldownTicker {
    private ClientCooldownTicker() {
    }

    /**
     * Counts the client's copy of ability cooldowns down every tick. VH only syncs them on casts and every half second
     * for the selected ability, so without this the HUD sat on stale values (a 1 s cooldown stuck at "1s", or coming
     * back after it had already ended); every server sync still overwrites the local count.
     */
    static void tick() {
        AbilityTree tree = ClientAbilityData.getTree();
        if (tree == null) {
            return;
        }
        tree.iterate(Ability.class, ability -> ability.getCooldown().ifPresent(cooldown -> {
            cooldown.decrement();
            if (cooldown.getRemainingTicks() <= 0 && cooldown.getRemainingDelayTicks() <= 0) {
                ability.setCooldownFromSync(0, 0, 0);
            }
        }));
    }
}
