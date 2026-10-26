package xyz.iwolfking.woldsvaults.mixins.vaulthunters.custom;

import iskallia.vault.entity.entity.EffectCloudEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = EffectCloudEntity.class, remap = false)
public abstract class MixinEffectCloudEntityParticles {
    private static final double CONTAIN_SLACK = 0.75;
    private static final double SAME_LAYER = 1.0;
    private static final float SAME_RADIUS = 0.05F;

    /**
     * Skips this cloud's client particles when an identical cloud (same particle and colour, same height band) already
     * covers its whole area, so stacked copies of one cloud draw once. Of two equal copies the one with the lower
     * entity id keeps drawing. Only the visuals are skipped; effects still apply from every cloud.
     */
    @Inject(method = "tickParticles", at = @At("HEAD"), cancellable = true)
    private void woldsvaults$skipCoveredDuplicate(CallbackInfo ci) {
        EffectCloudEntity self = (EffectCloudEntity) (Object) this;
        float radius = self.getRadius();
        AABB area = self.getBoundingBox().inflate(radius + CONTAIN_SLACK, SAME_LAYER, radius + CONTAIN_SLACK);
        List<EffectCloudEntity> others = self.level.getEntitiesOfClass(EffectCloudEntity.class, area, other -> other != self && other.isAlive());
        for (EffectCloudEntity other : others) {
            if (covers(other, self, radius)) {
                ci.cancel();
                return;
            }
        }
    }

    private static boolean covers(EffectCloudEntity other, EffectCloudEntity self, float radius) {
        if (other.getColor() != self.getColor() || other.getParticleData().getType() != self.getParticleData().getType()
                || Math.abs(other.getY() - self.getY()) > SAME_LAYER) {
            return false;
        }
        float otherRadius = other.getRadius();
        double dx = other.getX() - self.getX();
        double dz = other.getZ() - self.getZ();
        if (Math.sqrt(dx * dx + dz * dz) + radius > otherRadius + CONTAIN_SLACK) {
            return false;
        }
        if (otherRadius > radius + SAME_RADIUS) {
            return true;
        }
        return otherRadius >= radius - SAME_RADIUS && ((Entity) other).getId() < ((Entity) self).getId();
    }
}
