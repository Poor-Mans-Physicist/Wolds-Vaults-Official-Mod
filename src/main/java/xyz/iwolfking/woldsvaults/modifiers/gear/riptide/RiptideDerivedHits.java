package xyz.iwolfking.woldsvaults.modifiers.gear.riptide;

import iskallia.vault.event.ActiveFlags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.mixins.LivingEntityAccessor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hits that riptide mechanics derive from a riptide contact's final damage (the shock cone, storm lightning). They
 * are queued and dealt at the end of the server tick, outside the contact's own hurt event, as a thorns-flagged
 * player hit, and the authored amount is restored at the end of the hurt chain, so there's no second pass of the
 * damage multipliers; armor and resistance still apply. The thorns flag alone doesn't stop Vault Hunters' lucky hit
 * roll, which only skips hits made under its chaining or area-attack flags, so every derived hit is also dealt under
 * the area-attack flag, as Vault Hunters' own ability splash damage is: no lucky hit, no on-hit procs such as stun or
 * cleave, and an area damage number. Invulnerability frames are cleared around the hit and restored, so the hit lands in full and does not
 * clip the next one.
 */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID)
public final class RiptideDerivedHits {
    private static final Deque<Pending> QUEUE = new ArrayDeque<>();

    private RiptideDerivedHits() {
    }

    private record Pending(ServerPlayer attacker, LivingEntity target, float amount) {
    }

    /** The damage source of a derived riptide hit; carries the amount it must land with. */
    public static class Source extends EntityDamageSource {
        private final float authoredAmount;

        public Source(ServerPlayer attacker, float authoredAmount) {
            super("player", attacker);
            this.authoredAmount = authoredAmount;
        }

        public float getAuthoredAmount() {
            return this.authoredAmount;
        }
    }

    public static void queue(ServerPlayer attacker, LivingEntity target, float amount) {
        if (amount > 0.0F) {
            QUEUE.add(new Pending(attacker, target, amount));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        int budget = QUEUE.size();
        while (budget-- > 0) {
            Pending hit = QUEUE.poll();
            if (hit == null) {
                return;
            }
            if (hit.attacker().isRemoved() || !hit.target().isAlive() || hit.target().level != hit.attacker().level) {
                continue;
            }
            deal(hit.attacker(), hit.target(), hit.amount());
        }
    }

    private static void deal(ServerPlayer attacker, LivingEntity target, float amount) {
        int savedInvulnerable = target.invulnerableTime;
        int savedHurtTime = target.hurtTime;
        float savedLastHurt = ((LivingEntityAccessor) target).getLastHurt();
        ActiveFlags.IS_THORNS_REFLECTING.push();
        ActiveFlags.IS_AOE_ATTACKING.push();
        try {
            target.invulnerableTime = 0;
            target.hurt(new Source(attacker, amount), amount);
        } finally {
            target.invulnerableTime = savedInvulnerable;
            target.hurtTime = savedHurtTime;
            ((LivingEntityAccessor) target).setLastHurt(savedLastHurt);
            ActiveFlags.IS_AOE_ATTACKING.pop();
            ActiveFlags.IS_THORNS_REFLECTING.pop();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreAuthoredAmount(LivingHurtEvent event) {
        if (event.getSource() instanceof Source source && event.getAmount() != source.getAuthoredAmount()) {
            event.setAmount(source.getAuthoredAmount());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        QUEUE.clear();
    }
}
