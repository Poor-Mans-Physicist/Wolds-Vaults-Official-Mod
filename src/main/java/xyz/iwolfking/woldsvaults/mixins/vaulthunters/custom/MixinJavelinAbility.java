package xyz.iwolfking.woldsvaults.mixins.vaulthunters.custom;

import iskallia.vault.entity.entity.VaultThrownJavelin;
import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.mana.ManaPlayer;
import iskallia.vault.snapshot.AttributeSnapshot;
import iskallia.vault.snapshot.AttributeSnapshotHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.abilities.flexible.FlexibleAbility;
import xyz.iwolfking.woldsvaults.api.cast.CastContext;
import xyz.iwolfking.woldsvaults.api.cast.CastTag;
import xyz.iwolfking.woldsvaults.init.ModGearAttributes;
import xyz.iwolfking.woldsvaults.modifiers.gear.special.ImplodingJavelinModification;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = VaultThrownJavelin.class, remap = false)
public abstract class MixinJavelinAbility {
    @Shadow
    public abstract Player getThrower();

    @Unique
    private static final AtomicBoolean woldsvaults$WARNED_NO_CAST = new AtomicBoolean();

    /**
     * Imploding Javelin: a javelin hit casts a free Implode on the target. It deals the mana the throw paid
     * (a sneaking Power Dump throw pays the whole bar), or the current bar for a standing throw, scaled by
     * {@code 1 + imploding_javelin_damage}.
     */
    @Inject(method = "doPostHurtEffects", at = @At(value = "INVOKE", target = "Liskallia/vault/util/EntityHelper;knockbackIgnoreResist(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;F)V", remap = false), remap = true)
    private void implodeJavelin(LivingEntity pLiving, CallbackInfo ci) {
        Player thrower = this.getThrower();
        AttributeSnapshot snapshot = AttributeSnapshotHelper.getInstance().getSnapshot(thrower);
        if(snapshot.getAttributeValue(ModGearAttributes.IMPLODING_JAVELIN, VaultGearAttributeTypeMerger.anyTrue())) {
            float mana = ((ManaPlayer) thrower).getMana();
            CastTag tag = CastContext.current();
            if (tag == null || !(thrower instanceof ServerPlayer serverThrower) || !tag.isOwnedBy(serverThrower)) {
                if (woldsvaults$WARNED_NO_CAST.compareAndSet(false, true)) {
                    WoldsVaults.LOGGER.warn("Imploding Javelin hit found no cast tag for its thrower; the free Implode uses the current mana bar.");
                }
            } else if (tag.manaConsumed() > 0.0F) {
                mana = tag.manaConsumed();
            }
            float damageScale = 1.0F + ImplodingJavelinModification.getTotalBonus(thrower);
            new FlexibleAbility().castImplode(thrower, pLiving, mana, damageScale);
        }
     }

}
