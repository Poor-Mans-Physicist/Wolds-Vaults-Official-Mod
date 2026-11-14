package xyz.iwolfking.woldsvaults.spellcraft.buff;

import iskallia.vault.mana.ManaAction;
import iskallia.vault.mana.ManaPlayer;
import net.minecraft.server.level.ServerPlayer;


/**
 * Discount and Shotgun copy refunds. VH has no mana-cost stat event (costs come from gear, {@code ManaCostHelper}),
 * so the refund is a share of the Vault mana a cast actually took, measured around the cast.
 */
public final class ManaRefunds {
    private ManaRefunds() {
    }

    public static void refund(ServerPlayer player, float amount) {
        if (amount > 0) {
            ((ManaPlayer) player).increaseMana(ManaAction.PLAYER_ACTION, amount);
        }
    }
}
