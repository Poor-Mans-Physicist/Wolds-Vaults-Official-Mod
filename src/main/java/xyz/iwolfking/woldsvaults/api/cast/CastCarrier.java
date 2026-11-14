package xyz.iwolfking.woldsvaults.api.cast;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

/**
 * What delivers a hit relative to the cast. CAST and PROJECTILE are reached by the cast scope and projectile tags;
 * the others by the cast's deferred glyphs, each only through its own carrier (a scheduled task reaches SCHEDULED
 * sources, an entity's tick ENTITY and MINION sources, a block entity's tick BLOCK_ENTITY sources, a damage-over-time
 * tick DOT sources, an ability's own later window EVENT sources, a minion's hit MINION sources).
 */
public enum CastCarrier {
    CAST,
    PROJECTILE,
    ENTITY,
    BLOCK_ENTITY,
    SCHEDULED,
    DOT,
    MINION,
    TOGGLE,
    EVENT;

    /** Whether the hit happens after the cast's own call and outside a tagged projectile's tick. */
    public boolean deferred() {
        return this != CAST && this != PROJECTILE;
    }

    /** The hit sources a deferred carrier delivers, or null for the cast itself (any source). */
    @Nullable
    public Set<CastCarrier> reaches() {
        return switch (this) {
            case CAST, PROJECTILE -> null;
            case ENTITY -> EnumSet.of(ENTITY, MINION);
            default -> EnumSet.of(this);
        };
    }
}
