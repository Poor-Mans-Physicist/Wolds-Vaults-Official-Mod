package xyz.iwolfking.woldsvaults.mixins.vaulthunters.gods.idona;

import iskallia.vault.util.calc.ManaCostHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ManaCostHelper.class, remap = false)
public interface ManaCostHelperAccessor {
    /** Rolls the player's Ethereal talents once, as a payment would. */
    @Invoker("rollsEthereal")
    static boolean woldsvaults$rollsEthereal(ServerPlayer player) {
        throw new AssertionError();
    }
}
