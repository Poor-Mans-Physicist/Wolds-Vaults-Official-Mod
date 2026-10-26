package xyz.iwolfking.woldsvaults.client.fx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * One pulse of the riptide shock cone, left in the world where the dash was on that tick: a refracting shell from
 * the apex ahead of the player to the base ring behind, with bright shock rings and streaks running back along it.
 * Successive pulses overlap into a trailing bow shock.
 */
@OnlyIn(Dist.CLIENT)
final class ShockConeEffect implements RiptideFx.Effect {
    private static final int LIFETIME = 7;
    private static final int SEGMENTS = 28;
    private static final int RINGS = 7;
    private static final int STREAKS = 9;
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

    @Override
    public boolean refracts() {
        return true;
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

    private static float shellStrength(float s, float fade) {
        return fade * FxGeometry.smoothstep(0.0F, 0.2F, s) * (1.0F - FxGeometry.smoothstep(0.82F, 1.0F, s));
    }

    @Override
    public void renderRefraction(BufferBuilder builder, Matrix4f pose, Vec3 camera, float partialTick) {
        float t = progress(partialTick);
        float fade = (float) Math.pow(1.0F - t, 1.4F);
        for (int ring = 0; ring < RINGS; ring++) {
            float s0 = (float) ring / RINGS;
            float s1 = (float) (ring + 1) / RINGS;
            float a0 = shellStrength(s0, fade);
            float a1 = shellStrength(s1, fade);
            for (int seg = 0; seg < SEGMENTS; seg++) {
                double th0 = Math.PI * 2.0 * seg / SEGMENTS;
                double th1 = Math.PI * 2.0 * (seg + 1) / SEGMENTS;
                float u0 = (float) seg / SEGMENTS;
                float u1 = (float) (seg + 1) / SEGMENTS;
                refractVertex(builder, pose, point(s0, th0, t, camera), a0, u0, s0);
                refractVertex(builder, pose, point(s1, th0, t, camera), a1, u0, s1);
                refractVertex(builder, pose, point(s1, th1, t, camera), a1, u1, s1);
                refractVertex(builder, pose, point(s0, th1, t, camera), a0, u1, s0);
            }
        }
    }

    private static void refractVertex(BufferBuilder builder, Matrix4f pose, Vec3 p, float strength, float u, float v) {
        builder.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .color(RiptideFx.TEAL[0], RiptideFx.TEAL[1], RiptideFx.TEAL[2], strength)
                .uv(u, v)
                .endVertex();
    }

    @Override
    public void renderGlow(VertexConsumer consumer, Matrix4f pose, Vec3 camera, float partialTick, boolean refracted) {
        float t = progress(partialTick);
        float fade = (float) Math.pow(1.0F - t, 1.6F);
        if (!refracted) {
            renderShell(consumer, pose, camera, t, fade * 0.14F);
        }
        float[] rimRgb = FxGeometry.lerp(RiptideFx.TEAL, RiptideFx.VIOLET, t);
        renderRing(consumer, pose, camera, 1.0F, t, 0.16F, FxGeometry.rgba(rimRgb, 0.85F * fade));
        renderRing(consumer, pose, camera, 1.0F, t, 0.05F, FxGeometry.rgba(RiptideFx.WHITE, 0.9F * fade));
        for (int k = 0; k < 2; k++) {
            float s = 0.35F + 0.6F * ((k * 0.5F + t * 1.5F) % 1.0F);
            renderRing(consumer, pose, camera, s, t, 0.07F, FxGeometry.rgba(RiptideFx.VIOLET, 0.5F * fade * (1.0F - s * 0.4F)));
        }
        for (int i = 0; i < STREAKS; i++) {
            double theta = Math.PI * 2.0 * (i + this.phase) / STREAKS;
            List<Vec3> streak = new ArrayList<>();
            for (int j = 0; j <= 6; j++) {
                float s = 0.12F + 0.88F * j / 6.0F;
                streak.add(point(s, theta + 0.25 * s, t, camera));
            }
            FxGeometry.ribbon(consumer, pose, streak, 0.015F, 0.07F,
                    FxGeometry.rgba(RiptideFx.WHITE, 0.0F), FxGeometry.rgba(RiptideFx.TEAL, 0.45F * fade));
        }
    }

    private void renderRing(VertexConsumer consumer, Matrix4f pose, Vec3 camera, float s, float t, float width, float[] color) {
        List<Vec3> ring = new ArrayList<>(SEGMENTS + 1);
        for (int seg = 0; seg <= SEGMENTS; seg++) {
            ring.add(point(s, Math.PI * 2.0 * seg / SEGMENTS, t, camera));
        }
        FxGeometry.ribbon(consumer, pose, ring, width, width, color, color);
    }

    private void renderShell(VertexConsumer consumer, Matrix4f pose, Vec3 camera, float t, float alpha) {
        for (int ring = 0; ring < RINGS; ring++) {
            float s0 = (float) ring / RINGS;
            float s1 = (float) (ring + 1) / RINGS;
            float[] c0 = FxGeometry.rgba(RiptideFx.TEAL, shellStrength(s0, alpha));
            float[] c1 = FxGeometry.rgba(RiptideFx.TEAL, shellStrength(s1, alpha));
            for (int seg = 0; seg < SEGMENTS; seg++) {
                double th0 = Math.PI * 2.0 * seg / SEGMENTS;
                double th1 = Math.PI * 2.0 * (seg + 1) / SEGMENTS;
                FxGeometry.quad(consumer, pose, point(s0, th0, t, camera), point(s1, th0, t, camera),
                        point(s1, th1, t, camera), point(s0, th1, t, camera), c0, c1, c1, c0);
            }
        }
    }
}
