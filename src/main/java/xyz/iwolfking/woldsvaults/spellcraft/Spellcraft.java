package xyz.iwolfking.woldsvaults.spellcraft;

import iskallia.vault.init.ModSounds;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import xyz.iwolfking.woldsvaults.spellcraft.cast.SpellCaster;
import xyz.iwolfking.woldsvaults.spellcraft.play.SpellCharge;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectileGlyphs;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;
import xyz.iwolfking.woldsvaults.spellcraft.item.StoredSpell;
import xyz.iwolfking.woldsvaults.spellcraft.roll.SpellcraftRoller;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellModel;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellcraftRules;
import xyz.iwolfking.woldsvaults.config.SpellcraftConfig;
import xyz.iwolfking.woldsvaults.init.ModConfigs;
import xyz.iwolfking.woldsvaults.spellcraft.bar.EffectiveBar;
import xyz.iwolfking.woldsvaults.spellcraft.play.SpellBars;
import xyz.iwolfking.woldsvaults.spellcraft.play.SpellCooldowns;
import xyz.iwolfking.woldsvaults.spellcraft.buff.LingeringBuffs;
import xyz.iwolfking.woldsvaults.spellcraft.gear.ConvertedEtchings;
import xyz.iwolfking.woldsvaults.spellcraft.gear.ConvertedAffixes;
import xyz.iwolfking.woldsvaults.spellcraft.roll.UniqueSetRoller;
import xyz.iwolfking.woldsvaults.spellcraft.source.EquippedSources;
import xyz.iwolfking.woldsvaults.spellcraft.source.GlyphView;

import javax.annotation.Nullable;
import java.util.List;
import xyz.iwolfking.woldsvaults.api.core.WoldLogger;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityCastTracker;
import net.minecraftforge.common.MinecraftForge;
import xyz.iwolfking.woldsvaults.spellcraft.buff.ToggleDiscounts;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectileSteering;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectileSizing;
import xyz.iwolfking.woldsvaults.spellcraft.projectile.ProjectilePhysics;
import xyz.iwolfking.woldsvaults.spellcraft.scope.DeferredReach;
import xyz.iwolfking.woldsvaults.spellcraft.buff.BuffGlyphs;
import xyz.iwolfking.woldsvaults.spellcraft.cast.SpellScheduler;
import xyz.iwolfking.woldsvaults.spellcraft.hit.ShotgunHits;
import xyz.iwolfking.woldsvaults.spellcraft.cast.SpellMessages;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;

/**
 * Spellcraft: focuses and wands roll arcana, glyph lines and depth; their owner builds augmented spells from the glyphs
 * the item's view offers and gives each spell a bar slot and a priority. While the item is equipped its spells overlay
 * those slots of the default bar. This class holds the entry points the bar and the focus screen call.
 */
public final class Spellcraft {
    private Spellcraft() {
    }

    /**
     * Binds the cast-effect handlers at setup, in this order, instead of from annotated subscribers, so they run after
     * every annotated handler of the same priority: elemental procs read a hit before glyph damage scales it.
     */
    public static void registerHandlers() {
        MinecraftForge.EVENT_BUS.register(ToggleDiscounts.class);
        MinecraftForge.EVENT_BUS.register(ProjectileSteering.class);
        MinecraftForge.EVENT_BUS.register(ProjectileSizing.class);
        MinecraftForge.EVENT_BUS.register(ProjectilePhysics.class);
        MinecraftForge.EVENT_BUS.register(DeferredReach.class);
        MinecraftForge.EVENT_BUS.register(BuffGlyphs.class);
        MinecraftForge.EVENT_BUS.register(ProjectileGlyphs.class);
        MinecraftForge.EVENT_BUS.register(SpellScheduler.class);
        MinecraftForge.EVENT_BUS.register(SpellCharge.class);
        MinecraftForge.EVENT_BUS.register(ShotgunHits.class);
    }

    /** Registers the VH event listeners; called once from common setup. */
    public static void init() {
        AbilityCastTracker.register();
        ProjectileGlyphs.register();
        ConvertedAffixes.init();
        ConvertedEtchings.init();
        LingeringBuffs.init();
        SpellcraftRoller.setUniqueSets(new UniqueSetRoller());
    }

    private static boolean worksHere(SpellcraftConfig config, ServerPlayer player) {
        return config.worksOutsideVaults || player.getLevel().dimension().location().getNamespace().equals("the_vault");
    }

    /**
     * A key on a slot holding an augmented or implicit spell. Spells with Charge wind up while the key is held and cast on
     * release; others cast on release. A LOCKED slot, a spell with a structural error, or a spell any of whose ability
     * groups is still cooling down refuses with the reason. Inactive glyphs never refuse: they are skipped.
     */
    public static void onSpellKey(ServerPlayer player, EffectiveBar bar, EffectiveBar.Slot slot, boolean down) {
        SpellcraftConfig config = ModConfigs.SPELLCRAFT;
        SpellModel model = slot.model();
        if (config == null || model == null) {
            return;
        }
        if (!worksHere(config, player)) {
            if (!down) {
                SpellMessages.show(player, "Spells don't work here");
            }
            return;
        }
        String name = slot.spell() == null ? AbilityLookup.groupName(slot.group()) : slot.spell().name();
        if (slot.locked()) {
            if (!down) {
                SpellMessages.show(player, "Slot " + (slot.index() + 1) + " is locked: " + slot.lockReason());
            }
            return;
        }
        if (!model.isValid()) {
            if (!down) {
                SpellMessages.show(player, "Can't cast " + name + ": " + model.errors().get(0));
            }
            return;
        }
        boolean charges = model.hasCharge() && !slot.implicit() && slot.hand() != null;
        if (!charges) {
            if (!down && ready(player, slot)) {
                SpellCaster.castSlot(player, slot, -1.0F);
            }
            return;
        }
        if (down) {
            if (ready(player, slot)) {
                SpellCharge.start(player, slot.index(), slot.occupantKey(), slot.hand(), Math.round(config.tuning.chargeSeconds * 20.0F));
            }
        } else {
            float share = SpellCharge.release(player, slot.index(), slot.occupantKey());
            if (share >= 0.0F && ready(player, slot)) {
                SpellCaster.castSlot(player, slot, share);
            }
        }
    }

    /**
     * The all-groups gate: every ability group the slot holds must be learned and off cooldown. Otherwise plays VH's
     * cooldown sound and names the slowest group when it isn't the trigger.
     */
    private static boolean ready(ServerPlayer player, EffectiveBar.Slot slot) {
        AbilityTree tree = AbilityLookup.tree(player);
        for (String group : slot.groups()) {
            if (AbilityLookup.ability(tree, group) == null) {
                SpellMessages.show(player, AbilityLookup.groupName(group) + " is not learned");
                return false;
            }
        }
        SpellCooldowns.Blocking blocking = SpellCooldowns.blocking(tree, slot.groups());
        if (blocking == null) {
            return true;
        }
        player.level.playSound(null, player.blockPosition(), ModSounds.ABILITY_ON_COOLDOWN, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!blocking.group().equals(slot.group())) {
            SpellMessages.show(player, AbilityLookup.groupName(blocking.group()) + ": " + Math.max(1, Math.round(blocking.ticks() / 20.0F)) + " s");
        }
        return false;
    }

    /**
     * Creates (id 0), changes or deletes a spell on the focus or wand in the player's inventory slot. The whole item is
     * checked against its glyph view first; a change that breaks any rule is refused with the reason. A changed spell
     * keeps its slot and priority.
     */
    public static void editSpell(ServerPlayer player, int inventorySlot, int spellId, String name, List<String> tokens, boolean delete) {
        SpellcraftConfig config = ModConfigs.SPELLCRAFT;
        ItemStack stack = carrier(player, inventorySlot);
        if (config == null || stack == null) {
            return;
        }
        if (delete) {
            SpellcraftItemData.removeSpell(stack, spellId);
            SpellBars.refresh(player);
            return;
        }
        String cleanName = name.strip();
        if (cleanName.isEmpty()) {
            cleanName = "Spell";
        }
        if (cleanName.length() > config.rules.maxNameLength) {
            cleanName = cleanName.substring(0, config.rules.maxNameLength);
        }
        StoredSpell stored = spellId > 0 ? SpellcraftItemData.spell(stack, spellId).orElse(null) : null;
        int slot = stored == null ? SpellcraftItemData.UNASSIGNED : stored.slot();
        int prio = stored == null ? 0 : stored.prio();
        int checkId = spellId > 0 ? spellId : Integer.MAX_VALUE;
        StoredSpell edited = new StoredSpell(checkId, cleanName, List.copyOf(tokens), slot, prio);
        if (!store(player, config, stack, SpellcraftRules.withEdit(stack, checkId, edited), "Spell not saved: ")) {
            return;
        }
        SpellcraftItemData.putSpell(stack, new StoredSpell(spellId > 0 ? spellId : 0, cleanName, List.copyOf(tokens), slot, prio));
        SpellBars.refresh(player);
    }

    /**
     * Moves a spell of the item in the player's inventory slot to a bar slot ({@link SpellcraftItemData#UNASSIGNED} takes
     * it off the bar) and sets its priority (0 keeps it). Refused when another spell of the item already uses the slot.
     */
    public static void placeSpell(ServerPlayer player, int inventorySlot, int spellId, int barSlot, int prio) {
        SpellcraftConfig config = ModConfigs.SPELLCRAFT;
        if (config == null) {
            WoldLogger.error("Spellcraft: no focus augments table; spell {} was not moved", spellId);
            return;
        }
        ItemStack stack = carrier(player, inventorySlot);
        if (stack == null) {
            return;
        }
        StoredSpell stored = SpellcraftItemData.spell(stack, spellId).orElse(null);
        if (stored == null) {
            refuse(player, "That spell is not on the item");
            return;
        }
        if (barSlot < SpellcraftItemData.UNASSIGNED || barSlot >= SpellcraftItemData.SLOTS || prio < 0 || prio > config.rules.priorities) {
            refuse(player, "Spell not moved: slot or priority out of range");
            return;
        }
        StoredSpell moved = stored.withSlot(barSlot).withPrio(prio > 0 ? prio : stored.prio());
        if (!store(player, config, stack, SpellcraftRules.withEdit(stack, spellId, moved), "Spell not moved: ")) {
            return;
        }
        SpellcraftItemData.putSpell(stack, moved);
        SpellBars.refresh(player);
    }

    private static boolean store(ServerPlayer player, SpellcraftConfig config, ItemStack stack, List<StoredSpell> spells,
                                 String refusal) {
        GlyphView view = EquippedSources.get(player).view(config, stack);
        SpellcraftRules.ItemCheck check = SpellcraftRules.checkEdit(config, stack, view, spells, AbilityLookup.specOf(AbilityLookup.tree(player)));
        if (!check.isValid()) {
            refuse(player, refusal + check.errors().get(0));
            return false;
        }
        return true;
    }

    @Nullable
    private static ItemStack carrier(ServerPlayer player, int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
            return null;
        }
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (!SpellcraftItemData.canCarry(stack) || !SpellcraftItemData.has(stack)) {
            refuse(player, "That item has no spellcraft");
            return null;
        }
        return stack;
    }

    /** A refused edit or placement: shown in chat (the action bar is hidden behind the focus screen) and logged. */
    private static void refuse(ServerPlayer player, String message) {
        WoldLogger.warn("Spellcraft: refused a spell edit by {}: {}", player.getGameProfile().getName(), message);
        player.displayClientMessage(new TextComponent(message).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }
}
