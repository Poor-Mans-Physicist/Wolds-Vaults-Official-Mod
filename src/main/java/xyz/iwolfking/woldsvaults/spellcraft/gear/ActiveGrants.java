package xyz.iwolfking.woldsvaults.spellcraft.gear;

import net.minecraft.server.level.ServerPlayer;
import xyz.iwolfking.woldsvaults.spellcraft.scope.CastScope;
import xyz.iwolfking.woldsvaults.spellcraft.scope.DeferredReach;
import xyz.iwolfking.woldsvaults.spellcraft.scope.GlyphGrants;

import javax.annotation.Nullable;

public final class ActiveGrants {
    private ActiveGrants() {
    }

    /**
     * The grants of the open cast scope's step, else of the deferred scope of one of the player's spell steps; null
     * outside both, so a step that grants nothing still decides the read.
     */
    @Nullable
    static GlyphGrants ofCast(ServerPlayer player) {
        CastScope.Active scope = CastScope.forPlayer(player);
        if (scope != null) {
            return scope.modifiers().grants();
        }
        DeferredReach.Reach reach = DeferredReach.current(player);
        if (reach != null) {
            return reach.glyphs().modifiers().grants();
        }
        return null;
    }
}
