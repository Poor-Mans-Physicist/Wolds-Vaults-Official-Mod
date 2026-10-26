package xyz.iwolfking.woldsvaults.mixins.cast;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import iskallia.vault.skill.ability.effect.spi.core.Ability;
import iskallia.vault.skill.ability.effect.spi.core.InstantAbility;
import iskallia.vault.skill.base.SkillContext;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import xyz.iwolfking.woldsvaults.api.cast.CastContext;
import xyz.iwolfking.woldsvaults.api.cast.CastTag;

/**
 * Opens a cast context for every instant ability a player casts, covering the mana check, the hits
 * and the payment, so per-cast effects can tell the hits of one cast apart from the next.
 */
@Mixin(value = InstantAbility.class, remap = false)
public class MixinInstantAbilityCast {

    @WrapMethod(method = "onAction")
    private Ability.ActionResult woldsvaults$openCastContext(SkillContext context, Operation<Ability.ActionResult> original) {
        ServerPlayer player = context.getSource().as(ServerPlayer.class).orElse(null);
        if (player == null) {
            return original.call(context);
        }
        CastTag tag = new CastTag(player, (InstantAbility) (Object) this);
        return CastContext.run(tag, () -> original.call(context));
    }
}
