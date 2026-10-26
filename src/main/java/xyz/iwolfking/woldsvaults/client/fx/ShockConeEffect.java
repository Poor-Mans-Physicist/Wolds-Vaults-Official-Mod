package xyz.iwolfking.woldsvaults.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * One pulse of the riptide shock cone, left in the world where the dash was on that tick: pulsing shock rings at the
 * base and inside the cone, and a braid of streamers that swirl back along its length from the apex ahead of the
 * player to the base ring behind. Successive pulses overlap into a trailing, spiralling bow shock.
 */
@OnlyIn(Dist.CLIENT)
final class ShockConeEffect implements RiptideFx.Effect {
    private static final int LIFETIME = 7;
    private static final int SEGMENTS = 28;
    private static final int SWIRLS = 6;
    private static final int SWIRL_POINTS = 12;
    private static final float SWIRL_TWIST = 2.4F;
    private static final float SWIRL_SPIN = 2.8F;
    private static final float SWIRL_LENGTH = 0.5F;
    private static final float GROWTH = 0.45F;
    private static final float DRIFT = 0.8F;

    private final Vec3 origin;
    private final Vec3 axis;
    private final Vec3 side;
    private final Vec3 up;
    private final float ahead;
    private final float behind;
    private final float radius;
    private final float phase;
    private int age;

    ShockConeEffect(Vec3 origin, Vec3 axis, float ahead, float behind, float radius, float phase) {
        this.origin = origin;
        this.axis = axis.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : axis.normalize();
        Vec3[] basis = FxGeometry.basis(this.axis);
        this.side = basis[0];
        this.up = basis[1];
        this.ahead = ahead;
        this.behind = behind;
        this.radius = radius;
        this.phase = phase;
    }

    @Override
    public boolean tick(ClientLevel level) {
        return ++this.age < LIFETIME;
    }

    private float progress(float partialTick) {
        return Math.min(1.0F, (this.age + partialTick) / LIFETIME);
    }

    private Vec3 point(float s, double theta, float t, Vec3 camera) {
        double length = this.ahead + this.behind;
        double along = this.ahead - s * length - DRIFT * t;
        double r = this.radius * s * (1.0 + GROWTH * t);
        return this.origin.add(this.axis.scale(along))
                .add(this.side.scale(Math.cos(theta) * r))
                .add(this.up.scale(Math.sin(theta) * r))
                .subtract(camera);
    }

    @Override
    public void renderGlow(VertexConsumer consumer, Matrix4f pose, Vec3 camera, float partialTick) {
        float t = progress(partialTick);
        float fade = (float) Math.pow(1.0F - t, 1.6F);
        float[] rimRgb = FxGeometry.lerp(RiptideFx.TEAL, RiptideFx.VIOLET, t);
        renderRing(consumer, pose, camera, 1.0F, t, 0.16F, FxGeometry.rgba(rimRgb, 0.85F * fade));
        renderRing(consumer, pose, camera, 1.0F, t, 0.05F, FxGeometry.rgba(RiptideFx.WHITE, 0.9F * fade));
        for (int k = 0; k < 2; k++) {
            float s = 0.35F + 0.6F * ((k * 0.5F + t * 1.5F) % 1.0F);
            renderRing(consumer, pose, camera, s, t, 0.07F, FxGeometry.rgba(RiptideFx.VIOLET, 0.5F * fade * (1.0F - s * 0.4F)));
        }
        renderSwirls(consumer, pose, camera, t, fade);
    }

    /**
     * Streamers wound around the cone as helices. Each one's bright head runs from near the apex to the base over the
     * pulse's life with a fading tail behind it; alternate streamers wind the other way and glow violet, so the set
     * reads as a turning braid.
     */
    private void renderSwirls(VertexConsumer consumer, Matrix4f pose, Vec3 camera, float t, float fade) {
        float head = Math.min(1.0F, 0.15F + t * 1.05F);
        float tail = Math.max(0.06F, head - SWIRL_LENGTH);
        if (head - tail < 0.02F) {
            return;
        }
        for (int i = 0; i < SWIRLS; i++) {
            boolean counter = i % 2 == 1;
            double sign = counter ? -1.0 : 1.0;
            double start = Math.PI * 2.0 * (this.phase + (double) i / SWIRLS);
            List<Vec3> streamer = new ArrayList<>(SWIRL_POINTS + 1);
            for (int j = 0; j <= SWIRL_POINTS; j++) {
                float s = tail + (head - tail) * j / SWIRL_POINTS;
                double theta = start + sign * (SWIRL_TWIST * s + SWIRL_SPIN * t);
                streamer.add(point(s, theta, t, camera));
            }
            float[] tip = counter ? RiptideFx.VIOLET : RiptideFx.TEAL;
            FxGeometry.ribbon(consumer, pose, streamer, 0.02F, 0.09F,
                    FxGeometry.rgba(tip, 0.0F), FxGeometry.rgba(tip, 0.7F * fade));
            FxGeometry.ribbon(consumer, pose, streamer, 0.006F, 0.03F,
                    FxGeometry.rgba(RiptideFx.WHITE, 0.0F), FxGeometry.rgba(RiptideFx.WHITE, 0.85F * fade));
        }
    }

    private void renderRing(VertexConsumer consumer, Matrix4f pose, Vec3 camera, float s, float t, float width, float[] color) {
        List<Vec3> ring = new ArrayList<>(SEGMENTS + 1);
        for (int seg = 0; seg <= SEGMENTS; seg++) {
            ring.add(point(s, Math.PI * 2.0 * seg / SEGMENTS, t, camera));
        }
        FxGeometry.ribbon(consumer, pose, ring, width, width, color, color);
    }
}
