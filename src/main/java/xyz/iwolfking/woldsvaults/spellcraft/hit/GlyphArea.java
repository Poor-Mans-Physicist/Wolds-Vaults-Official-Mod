package xyz.iwolfking.woldsvaults.spellcraft.hit;

import iskallia.vault.core.event.CommonEvents;
import iskallia.vault.core.event.common.PlayerStatEvent;
import iskallia.vault.util.calc.PlayerStat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import xyz.iwolfking.woldsvaults.spellcraft.scope.CastModifiers;
import xyz.iwolfking.woldsvaults.spellcraft.scope.CastScope;
import xyz.iwolfking.woldsvaults.spellcraft.scope.DeferredReach;
import xyz.iwolfking.woldsvaults.spellcraft.scope.ProjectileGlyphTags;

import javax.annotation.Nullable;

public final class GlyphArea {
    private static final Object LISTENER_OWNER = new Object();

    private GlyphArea() {
    }

    /** Registers the VH PLAYER_STAT listener; called once from common setup. */
    public static void register() {
        CommonEvents.PLAYER_STAT.register(LISTENER_OWNER, GlyphArea::onPlayerStat);
    }

    private static void onPlayerStat(PlayerStatEvent.Data data) {
        Entity entity = data.getEntity();
        if (entity == null) {
            return;
        }
        if (data.getStat() == PlayerStat.AREA_OF_EFFECT) {
            CastModifiers scope = scopeFor(entity);
            Projectile ticking = scope == null ? GlyphDamage.tickingProjectileOf(entity) : null;
            float factor = scope != null ? scope.area() : ticking != null ? ProjectileGlyphTags.area(ticking) : deferredArea(entity);
            if (factor != 1.0F) {
                data.setValue(data.getValue() * factor);
            }
        }
    }

    private static float deferredArea(Entity entity) {
        DeferredReach.Reach reach = DeferredReach.current(entity);
        if (reach == null || !OwnHits.hasSourceVia(reach.glyphs().spec(), reach.carriers())) {
            return 1.0F;
        }
        return reach.glyphs().modifiers().area();
    }

    @Nullable
    private static CastModifiers scopeFor(Entity entity) {
        CastScope.Active scope = CastScope.forPlayer(entity);
        return scope == null ? null : scope.modifiers();
    }
}
