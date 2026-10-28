package xyz.iwolfking.woldsvaults.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.iwolfking.woldsvaults.modifiers.gear.riptide.RiptideDash;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityRiptide extends Entity {
    @Shadow protected int autoSpinAttackTicks;

    private MixinLivingEntityRiptide(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow protected abstract void doAutoAttackOnTouch(LivingEntity target);

    @Shadow protected abstract void setLivingEntityFlag(int flag, boolean set);

    /**
     * Piercing riptide: every living entity the spin sweeps through is hit once per dash and the dash carries on,
     * instead of stopping and bouncing back on the first one. Walls still end it. Runs on both sides, since the
     * player's movement is client-driven.
     */
    @Inject(method = "checkAutoSpinAttack", at = @At("HEAD"), cancellable = true)
    private void pierceRiptide(AABB previous, AABB current, CallbackInfo ci) {
        if (!((Object) this instanceof Player player) || !RiptideDash.hasPierce(player)) {
            return;
        }
        for (Entity entity : this.level.getEntities(this, previous.minmax(current))) {
            if (entity instanceof LivingEntity target && target.isAlive() && RiptideDash.markContact(player, target)) {
                this.doAutoAttackOnTouch(target);
            }
        }
        if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }
        if (!this.level.isClientSide && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
        }
        ci.cancel();
    }

    /**
     * Brackets the equipment scan so the base mod's cooldown check there ignores a riptide cooldown: the trident
     * keeps its vanilla attribute modifiers (attack damage, attack speed) through its own riptide cooldown, as it
     * already does in the attribute snapshot.
     */
    @Inject(method = "collectEquipmentChanges", at = @At("HEAD"))
    private void beginEquipmentScan(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        RiptideDash.beginEquipmentScan((LivingEntity) (Object) this);
    }

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void endEquipmentScan(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        RiptideDash.endEquipmentScan();
    }
}
