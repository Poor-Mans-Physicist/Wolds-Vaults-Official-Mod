package xyz.iwolfking.woldsvaults.client.spellcraft.stats;

import iskallia.vault.core.event.CommonEvents;
import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.init.ModEffects;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.mana.Mana;
import iskallia.vault.skill.ability.effect.ShieldBashAbility;
import iskallia.vault.skill.ability.effect.TauntCharmAbility;
import iskallia.vault.skill.ability.effect.TotemPlayerDamageAbility;
import iskallia.vault.skill.ability.effect.spi.AbstractIceBoltAbility;
import iskallia.vault.skill.ability.effect.spi.AbstractRampageAbility;
import iskallia.vault.skill.ability.effect.spi.core.Ability;
import iskallia.vault.skill.tree.AbilityTree;
import iskallia.vault.snapshot.AttributeSnapshot;
import iskallia.vault.snapshot.AttributeSnapshotHelper;
import iskallia.vault.util.calc.AbilityPowerHelper;
import iskallia.vault.util.calc.AreaOfEffectHelper;
import iskallia.vault.util.calc.AttributeLimitHelper;
import iskallia.vault.util.calc.BlockChanceHelper;
import iskallia.vault.util.calc.EffectDurationHelper;
import iskallia.vault.util.calc.PlayerStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.iwolfking.woldsvaults.spellcraft.scope.CastModifiers;
import xyz.iwolfking.woldsvaults.spellcraft.buff.BuffGlyphs;
import xyz.iwolfking.woldsvaults.spellcraft.glyph.GlyphStacking;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellModel;
import xyz.iwolfking.woldsvaults.config.SpellcraftConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftNames;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftPalette;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.StatText;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.MainEffectTable.Effects;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.MainEffectTable.Factor;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.MainEffectTable.Format;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.MainEffectTable.Main;

/**
 * What one press of a slot does at a training dummy, for the spell bar tooltip and the focus screen. The per-ability
 * numbers are VH's own description labels ({@code AbilityLabelBindingRegistry}, the values behind {@code $damage},
 * {@code $radius} ...), read against the player's live stats the way VH's label factory reads them; the step's glyphs
 * are then applied as the caster applies them.
 * <p>
 * Damage per hit is flat + attack damage x percent x (1 + Damage Increase) + ability power x percent + current mana x
 * percent. Damage Increase is counted because every attack-damage ability hit receives it (baked into the ability or in
 * the hurt event) and no ability-power hit does. Target-dependent multipliers (mob type, gods, armour, crits, distance
 * falloff) and temporary buffs are left out. Radius is VH's area-of-effect radius for the ability, with the step's area
 * factor multiplied into the AoE factor before VH's AoE limit clamps it; velocity is VH's throw power (blocks per tick,
 * shown per second) times the step's speed; duration is VH's effect-duration-adjusted duration.
 */
public final class StepStats {
    private static final String[] AREA_KEYS = {"adjustedRadius", "radius"};
    private static final String[] INTERVAL_KEYS = {"damageInterval", "stormInterval", "shockInterval"};
    private static final String[] COUNT_KEYS = {"shockCount", "boltCount", "numberOfJavelins", "numberOfBounces", "piercing", "maxTargets"};
    private static final String[] AMOUNT_KEYS = {"heal", "barrierPoints"};
    private static final Map<String, Effects> EFFECTS = MainEffectTable.effects();

    private StepStats() {
    }

    /** A damage value, or a min-max range for abilities that roll a random percent. */
    public record Hit(float min, float max) {
        public Hit times(float factor) {
            return new Hit(this.min * factor, this.max * factor);
        }

        public Hit plus(Hit other) {
            return new Hit(this.min + other.min, this.max + other.max);
        }

        public boolean isRange() {
            return this.max - this.min > Math.max(1.0E-3F, Math.abs(this.max) * 1.0E-3F);
        }
    }

    /**
     * One more number VH labels for the ability: the line's lang key (under {@code bar.stat.}), its formatted value and
     * the value's colour.
     */
    public record Extra(String key, String value, int color) {
    }

    /** A main effect's raw label value: a fraction for percents, ticks for times, else the number. */
    private record MainValue(Main main, float raw) {
    }

    /** An ability's numbers before glyphs; negative radius, velocity or duration means the ability has none. */
    private record Base(@Nullable Hit hit, @Nullable Hit splinter, @Nullable Hit splash, float radius, boolean aoe, float splashRadius,
                       float velocity, int durationTicks, List<Extra> extras, @Nullable Effects effects, List<MainValue> mains) {
    }

    /** A row of the spell's stats: an ability step or a Bundled Spell orb, at its payload depth. */
    public sealed interface Row permits Step, Orb {
        int depth();
    }

    /**
     * One ability step after its glyphs. {@code casts} counts Shotgun copies and the orbs carrying it; {@code total} is
     * one hit from every cast plus the Echo replay, or null when the step casts once with no Echo; the charged values are
     * at full Charge, or null without Charge. {@code mains} are the ability's main effect lines after the step's buff
     * glyphs; {@code relevance} says which of the step's glyph stats apply to it.
     */
    public record Step(String group, int depth, @Nullable Hit hit, @Nullable Hit charged, @Nullable Hit splinter, @Nullable Hit splash,
                       int casts, float echoPower, @Nullable Hit total, @Nullable Hit chargedTotal, float radius, float splashRadius,
                       float velocity, int durationTicks, List<Extra> mains, List<Extra> extras, @Nullable GlyphStacking.Pool glyphs,
                       GlyphRelevance.Filter relevance) implements Row {
    }

    /** A Bundled Spell orb: how many orbs fly, their velocity in blocks per second, and the flight time in seconds. */
    public record Orb(int depth, int copies, float velocity, float flightSeconds, GlyphStacking.Pool glyphs,
                      GlyphRelevance.Filter relevance) implements Row {
    }

    /** Every row of a spell, and the whole press's damage when more than one step deals damage (else null). */
    public record SpellStats(List<Row> rows, @Nullable Hit total) {
    }

    @Nullable
    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    /** A plain ability's stats as one step without glyphs, or null when it isn't learned or there is no player. */
    @Nullable
    public static Step plain(AbilityTree tree, String group) {
        LocalPlayer player = player();
        Ability ability = AbilityLookup.ability(tree, group);
        if (player == null || ability == null) {
            return null;
        }
        String spec = AbilityLookup.specOf(tree).apply(group);
        return step(player, group, base(player, ability, spec), CastModifiers.NONE, BuffGlyphs.Factors.NONE, 1.0F, 1, false, 0, null,
                GlyphRelevance.ALL);
    }

    /** An augmented spell's rows: each ability step with its glyphs, each Bundled Spell orb and its payload. */
    @Nullable
    public static SpellStats spell(SpellcraftConfig config, AbilityTree tree, SpellModel model) {
        LocalPlayer player = player();
        if (player == null) {
            return null;
        }
        List<Row> rows = new ArrayList<>();
        walk(player, config, tree, model, model.steps(), 1.0F, 1, true, 0, rows);
        Hit total = null;
        int damaging = 0;
        for (Row row : rows) {
            if (row instanceof Step step && step.hit() != null) {
                Hit press = step.total() != null ? step.total() : step.hit().times(step.casts());
                total = total == null ? press : total.plus(press);
                damaging++;
            }
        }
        return new SpellStats(rows, damaging > 1 ? total : null);
    }

    private static void walk(LocalPlayer player, SpellcraftConfig config, AbilityTree tree, SpellModel model, List<SpellModel.Step> steps,
                             float damageFactor, int multiplicity, boolean root, int depth, List<Row> rows) {
        for (SpellModel.Step step : steps) {
            if (step instanceof SpellModel.Cast cast) {
                Ability ability = AbilityLookup.ability(tree, cast.group());
                Base base = ability == null ? null : base(player, ability, cast.spec());
                CastModifiers modifiers = CastModifiers.of(config, cast.glyphs());
                BuffGlyphs.Factors factors = ability == null ? BuffGlyphs.Factors.NONE : BuffGlyphs.factorsOf(ability, modifiers);
                rows.add(step(player, cast.group(), base, modifiers, factors, damageFactor, multiplicity, root, depth, model.stepPool(cast),
                        GlyphRelevance.of(config, model, cast)));
            } else if (step instanceof SpellModel.Bundle bundle) {
                CastModifiers orb = CastModifiers.of(config, bundle.glyphs());
                int copies = 1 + orb.shotgunCopies();
                rows.add(new Orb(depth, copies * multiplicity, (float) (config.tuning.bundleSpeed * orb.speed() * 20.0), bundle.glyph().potency(),
                        model.stepPool(bundle), GlyphRelevance.of(config, model, bundle)));
                walk(player, config, tree, model, bundle.payload(), damageFactor * orb.damage(), multiplicity * copies, false, depth + 1, rows);
            }
        }
    }

    private static Step step(LocalPlayer player, String group, @Nullable Base base, CastModifiers modifiers, BuffGlyphs.Factors factors,
                             float damageFactor, int multiplicity, boolean root, int depth, @Nullable GlyphStacking.Pool glyphs,
                             GlyphRelevance.Filter relevance) {
        int casts = (1 + modifiers.shotgunCopies()) * multiplicity;
        float echo = modifiers.echoPower();
        if (base == null) {
            return new Step(group, depth, null, null, null, null, casts, echo, null, null, -1.0F, -1.0F, -1.0F, -1, List.of(), List.of(), glyphs,
                    relevance);
        }
        Effects effects = base.effects();
        float factor = modifiers.damage() * damageFactor * (effects == null ? 1.0F : value(effects.hit(), factors));
        Hit hit = base.hit() == null ? null : base.hit().times(factor);
        Hit charged = hit != null && root && modifiers.chargeBonus() > 0.0F ? hit.times(1.0F + modifiers.chargeBonus()) : null;
        Hit total = null;
        Hit chargedTotal = null;
        if (hit != null && (casts > 1 || echo > 0.0F)) {
            Hit echoed = hit.times(echo * casts);
            total = hit.times(casts).plus(echoed);
            chargedTotal = charged == null ? null : charged.times(casts).plus(echoed);
        }
        Hit splinter = base.splinter() == null ? null : base.splinter().times(factor);
        Hit splash = base.splash() == null ? null : base.splash().times(factor);
        float radius = base.radius() >= 0.0F && base.aoe() ? base.radius() * areaRatio(player, modifiers.area()) : base.radius();
        float velocity = base.velocity() >= 0.0F ? base.velocity() * modifiers.speed() : -1.0F;
        int duration = base.durationTicks() >= 0 && effects != null
                ? BuffGlyphs.scale(base.durationTicks(), value(effects.duration(), factors)) : base.durationTicks();
        List<Extra> mains = new ArrayList<>();
        for (MainValue main : base.mains()) {
            mains.add(format(main, value(main.main().factor(), factors)));
        }
        return new Step(group, depth, hit, charged, splinter, splash, casts, echo, total, chargedTotal, radius, base.splashRadius(), velocity,
                duration, mains, base.extras(), glyphs, relevance);
    }

    private static float value(Factor factor, BuffGlyphs.Factors factors) {
        return switch (factor) {
            case NONE -> 1.0F;
            case INTENSITY -> factors.intensity();
            case DURATION -> factors.duration();
            case UPSIDE -> factors.durationUpside();
            case DRAIN -> factors.drain();
        };
    }

    /** A main effect line at a factor, rounded as the server rounds it (integer stats and ticks to nearest, minimum 1). */
    private static Extra format(MainValue main, float factor) {
        Main def = main.main();
        int color = def.format() == Format.TICKS || def.factor() == Factor.DRAIN ? SpellcraftPalette.STAT_YELLOW : SpellcraftPalette.STAT_INTENSITY;
        String value = switch (def.format()) {
            case PERCENT -> SpellcraftNames.tr("fmt.percent", StatText.compact(main.raw() * factor * 100.0F));
            case NUMBER -> StatText.compact(main.raw() * factor);
            case INTEGER -> String.valueOf(BuffGlyphs.scale(Math.round(main.raw()), factor));
            case BLOCKS -> SpellcraftNames.tr("fmt.blocks", StatText.compact(main.raw() * factor));
            case TICKS -> SpellcraftNames.tr("fmt.seconds", StatText.seconds(BuffGlyphs.scale(Math.round(main.raw()), factor)));
            case PER_SECOND -> SpellcraftNames.tr("fmt.per_second", StatText.compact(main.raw() * factor));
        };
        return new Extra(def.key(), value, color);
    }

    /** VH's numbers for one ability against the player's current stats, before any glyph; {@code spec} picks its main effects. */
    private static Base base(LocalPlayer player, Ability ability, @Nullable String spec) {
        AttributeSnapshot snapshot = AttributeSnapshotHelper.getInstance().getSnapshot(player);
        float damageIncrease = snapshot.getAttributeValue(ModGearAttributes.DAMAGE_INCREASE, VaultGearAttributeTypeMerger.floatSum());
        float attackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.0F + damageIncrease);
        Hit hit = hit(player, ability, attackDamage);
        Float splinterPercent = VhAbilityLabels.percent(ability, "splinterDamage");
        Float splashPercent = VhAbilityLabels.percent(ability, "splashDamage");
        Hit splinter = hit == null || splinterPercent == null ? null : hit.times(splinterPercent);
        Hit splash = hit == null || splashPercent == null ? null : hit.times(splashPercent);
        float radius = -1.0F;
        boolean aoe = false;
        for (String key : AREA_KEYS) {
            Float value = VhAbilityLabels.number(ability, key);
            if (value != null) {
                aoe = key.equals("adjustedRadius");
                radius = aoe ? AreaOfEffectHelper.adjustAreaOfEffect(player, ability, value) : value;
                break;
            }
        }
        Float splashBase = VhAbilityLabels.number(ability, "splashRadius");
        float splashRadius = splashBase == null ? -1.0F : AreaOfEffectHelper.adjustAreaOfEffect(player, ability, splashBase);
        Float throwPower = VhAbilityLabels.number(ability, "throwPower");
        float velocity = throwPower == null ? -1.0F : throwPower * 20.0F;
        Effects effects = spec == null ? null : EFFECTS.get(spec);
        Integer adjusted = VhAbilityLabels.ticks(ability, "adjustedDuration");
        Integer plainDuration = VhAbilityLabels.ticks(ability, "duration");
        int duration = adjusted != null ? EffectDurationHelper.adjustEffectDurationFloor(player, adjusted) : plainDuration != null ? plainDuration : -1;
        if (duration < 0 && effects != null && effects.percentDuration()) {
            Float bound = VhAbilityLabels.percent(ability, "duration");
            duration = bound == null ? -1 : Math.round(bound * 100.0F);
        }
        List<MainValue> mains = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        if (effects != null) {
            for (Main main : effects.mains()) {
                Float raw = raw(ability, main);
                if (raw != null) {
                    mains.add(new MainValue(main, raw));
                    covered.add(main.label());
                } else {
                    VhAbilityLabels.missingLabel(ability, spec, main.label());
                }
            }
        }
        return new Base(hit, splinter, splash, radius, aoe, splashRadius, velocity, duration, extras(ability, covered), effects, mains);
    }

    @Nullable
    private static Float raw(Ability ability, Main main) {
        return switch (main.format()) {
            case PERCENT -> VhAbilityLabels.percent(ability, main.label());
            case NUMBER, INTEGER, BLOCKS -> VhAbilityLabels.number(ability, main.label());
            case TICKS -> {
                Integer ticks = VhAbilityLabels.ticks(ability, main.label());
                yield ticks == null ? null : ticks.floatValue();
            }
            case PER_SECOND -> {
                String value = VhAbilityLabels.binding(ability, main.label());
                yield value == null ? null : VhAbilityLabels.parse(ability, main.label(), value.endsWith("/s") ? value.substring(0, value.length() - 2) : value);
            }
        };
    }

    @Nullable
    private static Hit hit(LocalPlayer player, Ability ability, float attackDamage) {
        float flat = 0.0F;
        float adPercent = 0.0F;
        boolean any = false;
        if (ability instanceof AbstractIceBoltAbility bolt) {
            flat += (float) bolt.getDamagePerBolt();
            adPercent += bolt.getPercentAttackDamageDealt();
            any = true;
        } else if (ability instanceof ShieldBashAbility) {
            Float scalar = VhAbilityLabels.percent(ability, "damage");
            if (scalar != null) {
                adPercent += 1.0F + BlockChanceHelper.getBlockChance(player) * scalar;
                any = true;
            }
        } else if (!(ability instanceof AbstractRampageAbility || ability instanceof TotemPlayerDamageAbility || ability instanceof TauntCharmAbility)) {
            Float percent = VhAbilityLabels.percent(ability, "damage");
            if (percent != null) {
                adPercent += percent;
                any = true;
            }
        }
        for (String key : new String[]{"damageMultiplier", "shardAttackDamage"}) {
            Float percent = VhAbilityLabels.percent(ability, key);
            if (percent != null) {
                adPercent += percent;
                any = true;
            }
        }
        for (String key : new String[]{"baseDamage", "shardDamage"}) {
            Float value = VhAbilityLabels.number(ability, key);
            if (value != null) {
                flat += value;
                any = true;
            }
        }
        float apMin = 0.0F;
        float apMax = 0.0F;
        for (String key : new String[]{"ability_power", "minionDamage"}) {
            Float percent = VhAbilityLabels.percent(ability, key);
            if (percent != null) {
                apMin += percent;
                apMax += percent;
                any = true;
            }
        }
        Float rangeMin = VhAbilityLabels.percent(ability, "ability_power_min");
        Float rangeMax = VhAbilityLabels.percent(ability, "ability_power_max");
        if (rangeMin != null && rangeMax != null) {
            apMin += rangeMin;
            apMax += rangeMax;
            any = true;
        }
        Float manaPercent = VhAbilityLabels.percent(ability, "manaDamage");
        float mana = manaPercent == null ? 0.0F : manaPercent * Mana.get(player);
        any |= manaPercent != null;
        if (!any) {
            return null;
        }
        float abilityPower = apMin != 0.0F || apMax != 0.0F ? AbilityPowerHelper.getAbilityPower(player) : 0.0F;
        float fixed = flat + attackDamage * adPercent + mana;
        return new Hit(fixed + abilityPower * apMin, fixed + abilityPower * apMax);
    }

    private static List<Extra> extras(Ability ability, Set<String> covered) {
        List<Extra> extras = new ArrayList<>();
        Float distance = covered.contains("distance") ? null : VhAbilityLabels.number(ability, "distance");
        if (distance != null) {
            extras.add(new Extra("distance", SpellcraftNames.tr("fmt.blocks", StatText.compact(distance)), SpellcraftPalette.STAT_PLAIN));
        }
        for (String key : INTERVAL_KEYS) {
            Integer ticks = VhAbilityLabels.ticks(ability, key);
            if (ticks != null) {
                extras.add(new Extra("interval", SpellcraftNames.tr("fmt.seconds", StatText.seconds(ticks)), SpellcraftPalette.STAT_YELLOW));
                break;
            }
        }
        for (String key : COUNT_KEYS) {
            Float count = covered.contains(key) ? null : VhAbilityLabels.number(ability, key);
            if (count != null) {
                extras.add(new Extra(key, StatText.compact(count), SpellcraftPalette.STAT_PLAIN));
            }
        }
        for (String key : AMOUNT_KEYS) {
            Float amount = covered.contains(key) ? null : VhAbilityLabels.number(ability, key);
            if (amount != null) {
                extras.add(new Extra(key, StatText.compact(amount), SpellcraftPalette.STAT_INTENSITY));
            }
        }
        return extras;
    }

    /**
     * The step's area factor as VH would apply it: the AoE factor (1 + Area of Effect, after VH's area stat listeners)
     * times the glyph factor, clamped to VH's AoE limit, relative to the same factor without the glyph.
     */
    private static float areaRatio(LocalPlayer player, float factor) {
        if (factor == 1.0F || player.hasEffect(ModEffects.NULLIFY_AREA_OF_EFFECT)) {
            return 1.0F;
        }
        AttributeSnapshot snapshot = AttributeSnapshotHelper.getInstance().getSnapshot(player);
        float multiplier = 1.0F + snapshot.getAttributeValue(ModGearAttributes.AREA_OF_EFFECT, VaultGearAttributeTypeMerger.floatSum());
        multiplier = CommonEvents.PLAYER_STAT.invoke(PlayerStat.AREA_OF_EFFECT, player, multiplier).getValue();
        float limit = 1.0F + AttributeLimitHelper.getAreaOfEffectLimit(player);
        float plain = Mth.clamp(multiplier, 0.0F, limit);
        return plain <= 0.0F ? 0.0F : Mth.clamp(multiplier * factor, 0.0F, limit) / plain;
    }
}
