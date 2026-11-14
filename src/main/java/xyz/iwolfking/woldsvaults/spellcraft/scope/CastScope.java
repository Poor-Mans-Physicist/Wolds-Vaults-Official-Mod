package xyz.iwolfking.woldsvaults.spellcraft.scope;

import net.minecraft.server.level.ServerPlayer;
import xyz.iwolfking.woldsvaults.spellcraft.ability.CastOrigin;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What is in effect while the spell caster runs one ability for one player: the step's glyph modifiers, the landing
 * point of a Bundled Spell (projectiles the cast spawns move there), the slot's billing, the ability spec being cast
 * (so its own hits can be told from procs) and the Shotgun volley member it is. VH computes area, mana,
 * cooldown and spawns projectiles synchronously inside the cast, so all of it only needs to apply for its duration.
 * Server thread only; nested casts restore the outer scope.
 */
public final class CastScope {
    private static Active current;

    private CastScope() {
    }

    public static <T> T call(ServerPlayer player, CastModifiers modifiers, @Nullable CastOrigin origin, @Nullable CastBilling billing,
                             @Nullable String spec, @Nullable ShotgunShot shot, Supplier<T> action) {
        Active outer = current;
        current = new Active(player, modifiers, origin, billing, spec, shot);
        try {
            return action.get();
        } finally {
            current = outer;
        }
    }

    public static Optional<Active> current() {
        return Optional.ofNullable(current);
    }

    /** The billing of the scope open for this player, or null outside a slot cast. */
    @Nullable
    public static CastBilling billing(ServerPlayer player) {
        return current != null && current.player() == player ? current.billing() : null;
    }

    public record Active(ServerPlayer player, CastModifiers modifiers, @Nullable CastOrigin origin, @Nullable CastBilling billing,
                         @Nullable String spec, @Nullable ShotgunShot shot) {
    }
}
