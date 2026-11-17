package xyz.iwolfking.woldsvaults.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;
import iskallia.vault.gear.attribute.VaultGearAttribute;
import iskallia.vault.gear.attribute.VaultGearAttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Set gear (config/the_vault/gear_sets.json): which uniques form each set, and what each piece-count tier grants.
 * A grant is a gear attribute and value added to the wearer's attribute snapshot while the tier is active. Also
 * holds the shared riptide, lightning, Simon and Bullet Bill tuning the set mechanics read.
 */
public class GearSetConfig extends PackAuthoredConfig {
    @Expose private Map<String, String> documentation;
    @Expose private Riptide riptide;
    @Expose private Lightning lightning;
    @Expose private Simon simon;
    @Expose private BulletBill bulletBill;
    @Expose private Map<String, GearSet> sets;

    private transient Map<ResourceLocation, ResourceLocation> setByPiece;
    private transient Map<ResourceLocation, ResourceLocation> basePieces;
    private transient Map<ResourceLocation, GearSet> setsById;

    public static class Riptide {
        @Expose public float hoverHorizontalFriction;
        @Expose public float hoverVerticalDamping;
        @Expose public float shockConeAhead;
        @Expose public float shockConeBehind;
        @Expose public float shockConeRadius;
        @Expose public int stormVisualBoltsPerDash;
    }

    public static class Lightning {
        @Expose public float chainRange;
        @Expose public int formDurationTicks;
        @Expose public float dashDistance;
        @Expose public float dashWidth;
        @Expose public int dashCooldownTicks;
        @Expose public int dashTravelTicks;
        @Expose public float arcLashRange;
        @Expose public float arcLashDamage;
        @Expose public float arcLashStrikeDamage;
        @Expose public float arcLashConeAngle;
        @Expose public int arcLashBolts;
        @Expose public float arcLashSpeed;
        @Expose public int arcLashCooldownTicks;
        @Expose public float rodRange;
        @Expose public float rodLockAngle;
        @Expose public float rodOvershoot;
        @Expose public float rodDamageMultiplier;
        @Expose public int rodCooldownTicks;
        @Expose public float thunderDamagePerSecond;
        @Expose public float thunderRadius;
    }

    public static class Simon {
        @Expose public int plantCooldownTicks;
        @Expose public float seedSpeed;
        @Expose public float seedRange;
        @Expose public float growRadius;
        @Expose public float growthPerStage;
        @Expose public float maxGrowthPerHit;
        @Expose public int shrinkTicks;
        @Expose public float leashRange;
        @Expose public float budRange;
        @Expose public float budSpacing;
        @Expose public int resonateMinCrystals;
        @Expose public int showTicks;
        @Expose public int showGapTicks;
        @Expose public int inputBaseTicks;
        @Expose public int inputTicksPerCrystal;
        @Expose public float clickRange;
        @Expose public float[] detonationValues;
        @Expose public float burstRadius;
        @Expose public float burstRadiusPerValue;
        @Expose public float failDamage;
        @Expose public float streakDamage;
        @Expose public float facetDamage;
        @Expose public int facetSlowTicks;
        @Expose public int shardFieldTicks;
        @Expose public float shardFieldRadius;
        @Expose public float shardFieldDamage;
        @Expose public float finaleRadius;
        @Expose public float finaleRadiusPerValue;
        @Expose public float finaleMaxRadius;
        @Expose public float brittleResistance;
        @Expose public float reflectRadius;
        @Expose public float reflectRange;
        @Expose public float arcaneReflectDamage;
        @Expose public float prismReach;
        @Expose public int prismaticTestRound;
    }

    public static class BulletBill {
        @Expose public int cooldownTicks;
        @Expose public int jetUpgrades;
        @Expose public float jetPower;
        @Expose public boolean flightStabilizers;
        @Expose public boolean smartHover;
        @Expose public int boostTicks;
        @Expose public float boostSpeed;
        @Expose public int spinTicks;
        @Expose public float detonationNoDashPercent;
        @Expose public float detonationMinSpeed;
        @Expose public float detonationRadius;
        @Expose public float detonationRadiusPerSpeed;
        @Expose public float detonationMaxRadius;
        @Expose public int detonationOvertimeTicks;
        @Expose public float banzaiScale;
        @Expose public float kingScale;
        @Expose public float giantReachShare;
        @Expose public float cruiseSpeed;
        @Expose public float kingHeatRadius;
        @Expose public float kingHeatDamage;
        @Expose public int kingHeatIntervalTicks;
    }

    public static class GearSet {
        @Expose public String name;
        @Expose public String color;
        @Expose public String ancientColor;
        @Expose public List<String> pieces;
        @Expose public Map<String, String> variants;
        @Expose public List<Tier> tiers;

        public int colorRgb() {
            return parseColor(this.color, 0x1C8F3A);
        }

        public int ancientColorRgb() {
            return parseColor(this.ancientColor == null ? this.color : this.ancientColor, this.colorRgb());
        }
    }

    public static class Tier {
        @Expose public int pieces;
        @Expose public String text;
        @Expose public List<Grant> grants;
    }

    public static class Grant {
        @Expose public String attribute;
        @Expose public JsonElement value;
    }

    @Override
    public String getName() {
        return "gear_sets";
    }

    @Override
    protected void reset() {
        this.documentation = new LinkedHashMap<>();
        this.documentation.put("sets", "Set id -> name, colours, pieces (unique ids from unique_gear) and tiers. A piece counts while it sits in the player's attribute snapshot (worn in its intended slot, not broken, not above the player's level). Each unique counts once. variants (optional) = unique id -> the piece of the set it stands in for, counting as that piece.");
        this.documentation.put("tiers", "pieces = pieces needed; text = the tooltip line; grants = gear attributes added to the wearer's snapshot while the tier is active (values in the attribute's own units, 1.0 = 100%)");
        this.documentation.put("riptide", "Shared riptide tuning: hover friction is the extra per-tick horizontal speed multiplier during the hover, vertical damping the per-tick vertical one; the shock cone spans shockConeAhead blocks ahead of the player to shockConeBehind blocks behind, widening to shockConeRadius");
        this.documentation.put("lightning", "Shared lightning tuning: chainRange = how far a channeling strike jumps to the next enemy; throwing a trident with the lightning dash grant turns the thrower into lightning for formDurationTicks, during which each tap of a movement key sends them dashDistance blocks that way as a bolt of lightning, taking dashTravelTicks, striking enemies within dashWidth of the path, then waiting dashCooldownTicks after reappearing before the next dash. Left click is Arc Lash: arcLashBolts spinning lightning bolts thrown out evenly across arcLashConeAngle degrees either side of the look, flying arcLashSpeed blocks a tick for up to arcLashRange, striking each enemy they pass through (once per throw) for arcLashDamage of the trident's damage and calling a channeling strike (vanilla lightning) down on it for arcLashStrikeDamage of a channeling strike's damage, once per arcLashCooldownTicks. Right click is Lightning Rod: marks the enemy aimed at within rodRange (once per rodCooldownTicks); a dash within rodLockAngle degrees of the target locks on, running through it and on for rodOvershoot of a dash, and strikes it for rodDamageMultiplier of the dash damage. Middle click is Thunderstrike: ends the form with a giant bolt on the player striking every enemy within thunderRadius (across) for the trident's damage times thunderDamagePerSecond per second of form left");

        this.documentation.put("simon", "Simon's Set tuning. Planting (simon_crystals grant = planted crystals at once): right click shoots a prism seed seedSpeed blocks a tick, out to seedRange, once per plantCooldownTicks; it plants a Seed-stage crystal where it lands, replacing the oldest planted one. Damage the wearer deals to an enemy within growRadius of a crystal feeds it (once a tick) damage / attack damage points, at most maxGrowthPerHit, and growthPerStage points grow it a stage (Seed, Shard, Cluster, Geode); a crystal unfed for shrinkTicks loses a stage, and a budded Seed vanishes. Crystals also vanish when their owner is more than leashRange away. Brittle: each crystal costs brittleResistance Resistance. Budding (simon_budding grant = most crystals at once, planted, budded and incoming seeds together): a Lucky Hit from the wearer's own swing near any of their crystals (within growRadius) makes their planted crystal throw a new seed on to open ground it can see, up to budRange from it and at least budSpacing from every other crystal and incoming seed. Resonate (simon_sequence grant = burst damage per detonation value, times ability power): sneak right click with resonateMinCrystals Shard-or-bigger crystals lights them one by one, biggest first, each for showTicks with showGapTicks between; then the wearer has inputBaseTicks plus inputTicksPerCrystal per crystal to aim and left click them (within clickRange) in that order. The sequence shows every crystal once, then repeats, until it is one step longer than the last sequence got right, for as long as the streak lasts; each sequence in the streak adds streakDamage to every explosion. Each correct click bursts that crystal on every enemy within burstRadius plus burstRadiusPerValue per detonation value (detonationValues by stage), shattering it on its last turn; a wrong click or running out of time bursts the rest for failDamage of their damage and ends the streak. Facets (simon_facets grant = most facets): a Lucky Hit from a swing near a Geode gives it a facet: 1 = facetDamage more burst damage, 2 = also slows for facetSlowTicks, 3 = also leaves a shard field of shardFieldRadius for shardFieldTicks, hitting every half second for shardFieldDamage of the burst. simon_prismatic = the chance a budded seed is Prismatic (stands in for the next crystal in a sequence); simon_fracture = each correct click rolls the wearer's Lucky Hit chance to burst twice; simon_finale = a finished chain collapses into one blast at its middle for ability power times this times the chain's total detonation value, on a radius of finaleRadius plus finaleRadiusPerValue per value, at most finaleMaxRadius. Arcane beams (Arcane, Arcane Rail and Arcane: Prism) that enter a crystal (a column as wide as reflectRadius for a Geode, less for smaller stages) bounce from its middle to the nearest crystal within reflectRange that they haven't visited, then on to the nearest enemy, dealing arcaneReflectDamage of the beam's damage; where the beam hits the crystal nudges the bounce, landing that far off centre on the next crystal's face (or the enemy's body). Other abilities don't interact with crystals. A wearer's Prism beams reach prismReach times as far. prismaticTestRound is a testing aid (0 = off): in that round (1 = the first sequence, 2 = the one after a sequence got right, and so on), the wearer's first budded seed is Prismatic whatever the chance, unless one of their crystals or seeds already is");

        this.documentation.put("bulletBill","Bullet Bill tuning (bullet_bill grant = ticks the form lasts). Double tapping jump turns the wearer into a Bullet Bill, then cooldownTicks from when the form ends before the next. The flight is PneumaticCraft's jet boots with jump as the thrust key. Holding jump flies where the wearer looks at 0.3 blocks a tick per jetUpgrades (the boots' upgrade count, 1 to 5), times jetPower (0 to 1), times a speed multiplier that starts at 1, rises in a dive and falls four times more slowly in a climb (0.8 to 4.2), spooling up over the first second. With jump let go the jets carry on the same way at cruiseSpeed blocks a tick, speed multiplier included, spooling back down over a second; with cruiseSpeed 0 it hovers instead: sinking at 0.1 less 0.02 per jetUpgrades blocks a tick (5 holds its height), or 0.45 while sneaking, with sideways speed dying off on air drag. flightStabilizers (needs jetUpgrades 4 or more) stops the wearer dead on letting go, and whenever no movement key is held; smartHover only hovers after the first thrust, until the wearer lands. Hitting a wall above 2 blocks a tick with the jets running (speed x1.5 on Normal, x2 on Hard) hurts like flying into a wall. No fall damage while a Bullet Bill, nor on the landing after. bullet_bill_dash grant = more Bullet Dash damage while a Bullet Bill: the dash also fires the jets for boostTicks at boostSpeed times the speed, whether or not jump is held, with a spin over spinTicks and a red pulse. bullet_bill_detonation grant = detonation damage: right click as a Bullet Bill arms it, and the next time it hits a block going at least detonationMinSpeed blocks a tick it explodes and the form ends, hitting every enemy within detonationRadius plus detonationRadiusPerSpeed per block a tick (at most detonationMaxRadius) for one Bullet Dash hit (attack damage times the learned tier's percent, or detonationNoDashPercent if Bullet Dash isn't learned, with damage multipliers and the bullet_bill_dash bonus) times its speed in blocks a tick times the grant. An armed Bullet Bill flies on past the end of its time until it crashes, or explodes where it is once detonationOvertimeTicks more have gone by. Worn in place of the helmet, the Banzai Bill Helmet makes the Bill banzaiScale times larger and the King Bill Helmet kingScale times, tinted light red; the larger size is the drawn model, Bullet Dash's reach and the detonation's radius (and its cap), not the collision box; giantReachShare is how much of the extra size the reach and radius get (1 = all of it, so 5 times the size is 5 times the reach; 0 = none). A King Bill also burns every enemy within kingHeatRadius every kingHeatIntervalTicks, for kingHeatDamage of a Bullet Dash hit per second");

        this.riptide = new Riptide();
        this.riptide.hoverHorizontalFriction = 0.8F;
        this.riptide.hoverVerticalDamping = 0.5F;
        this.riptide.shockConeAhead = 2.0F;
        this.riptide.shockConeBehind = 3.0F;
        this.riptide.shockConeRadius = 4.0F;
        this.riptide.stormVisualBoltsPerDash = 3;

        this.lightning = new Lightning();
        this.lightning.chainRange = 6.0F;
        this.lightning.formDurationTicks = 200;
        this.lightning.dashDistance = 7.0F;
        this.lightning.dashWidth = 1.5F;
        this.lightning.dashCooldownTicks = 0;
        this.lightning.dashTravelTicks = 2;
        this.lightning.arcLashRange = 12.0F;
        this.lightning.arcLashDamage = 0.5F;
        this.lightning.arcLashStrikeDamage = 0.05F;
        this.lightning.arcLashConeAngle = 30.0F;
        this.lightning.arcLashBolts = 5;
        this.lightning.arcLashSpeed = 1.2F;
        this.lightning.arcLashCooldownTicks = 10;
        this.lightning.rodRange = 24.0F;
        this.lightning.rodLockAngle = 60.0F;
        this.lightning.rodOvershoot = 0.5F;
        this.lightning.rodDamageMultiplier = 1.5F;
        this.lightning.rodCooldownTicks = 10;
        this.lightning.thunderDamagePerSecond = 1.0F;
        this.lightning.thunderRadius = 10.0F;

        this.simon = new Simon();
        this.simon.plantCooldownTicks = 80;
        this.simon.seedSpeed = 1.5F;
        this.simon.seedRange = 32.0F;
        this.simon.growRadius = 6.0F;
        this.simon.growthPerStage = 4.0F;
        this.simon.maxGrowthPerHit = 1.5F;
        this.simon.shrinkTicks = 200;
        this.simon.leashRange = 48.0F;
        this.simon.budRange = 6.0F;
        this.simon.budSpacing = 3.0F;
        this.simon.resonateMinCrystals = 3;
        this.simon.showTicks = 8;
        this.simon.showGapTicks = 4;
        this.simon.inputBaseTicks = 60;
        this.simon.inputTicksPerCrystal = 30;
        this.simon.clickRange = 24.0F;
        this.simon.detonationValues = new float[]{0.0F, 1.0F, 2.0F, 3.5F};
        this.simon.burstRadius = 3.0F;
        this.simon.burstRadiusPerValue = 0.75F;
        this.simon.failDamage = 0.5F;
        this.simon.streakDamage = 0.1F;
        this.simon.facetDamage = 0.25F;
        this.simon.facetSlowTicks = 40;
        this.simon.shardFieldTicks = 80;
        this.simon.shardFieldRadius = 3.0F;
        this.simon.shardFieldDamage = 0.15F;
        this.simon.finaleRadius = 4.0F;
        this.simon.finaleRadiusPerValue = 0.75F;
        this.simon.finaleMaxRadius = 14.0F;
        this.simon.brittleResistance = 0.02F;
        this.simon.reflectRadius = 1.25F;
        this.simon.reflectRange = 24.0F;
        this.simon.arcaneReflectDamage = 1.0F;
        this.simon.prismReach = 1.5F;
        // testing aid, off for packs
        this.simon.prismaticTestRound = 0;

        this.bulletBill = new BulletBill();
        this.bulletBill.cooldownTicks = 400;
        this.bulletBill.jetUpgrades = 5;
        this.bulletBill.jetPower = 1.0F;
        this.bulletBill.flightStabilizers = false;
        this.bulletBill.smartHover = false;
        this.bulletBill.boostTicks = 20;
        this.bulletBill.boostSpeed = 1.75F;
        this.bulletBill.spinTicks = 20;
        this.bulletBill.detonationNoDashPercent = 0.25F;
        this.bulletBill.detonationMinSpeed = 0.5F;
        this.bulletBill.detonationRadius = 4.0F;
        this.bulletBill.detonationRadiusPerSpeed = 1.5F;
        this.bulletBill.detonationMaxRadius = 14.0F;
        this.bulletBill.detonationOvertimeTicks = 200;
        this.bulletBill.banzaiScale = 5.0F;
        this.bulletBill.kingScale = 10.0F;
        this.bulletBill.giantReachShare = 1.0F;
        this.bulletBill.cruiseSpeed = 0.7F;
        this.bulletBill.kingHeatRadius = 12.0F;
        this.bulletBill.kingHeatDamage = 0.5F;
        this.bulletBill.kingHeatIntervalTicks = 10;

        GearSet tidebreaker = new GearSet();
        tidebreaker.name = "Tidebreaker";
        tidebreaker.color = "#1C8F3A";
        tidebreaker.ancientColor = "#1C8F3A";
        tidebreaker.pieces = List.of(
                "woldsvaults:tidebreaker_reign", "woldsvaults:tidebreaker_heart", "woldsvaults:tidebreaker_leggings",
                "woldsvaults:tidebreaker_greaves", "woldsvaults:tidebreaker_tideripper", "woldsvaults:tidebreaker_hurricane");
        tidebreaker.tiers = List.of(
                tier(2, "Riptide pierces through enemies and deals 100% more damage.",
                        grant("woldsvaults:riptide_pierce", new JsonPrimitive(true)),
                        grant("woldsvaults:riptide_damage_more", new JsonPrimitive(1.0F))),
                tier(4, "Riptide releases a shock cone, striking enemies from 2 blocks ahead of you to a 4-block radius 3 blocks behind you.",
                        grant("woldsvaults:riptide_shock_cone", new JsonPrimitive(1.0F))),
                tier(6, "While dashing, a storm shrouds you: you hover briefly after each dash, take 75% less damage, and lightning strikes every enemy you hit for 5x damage (15x if chilled).",
                        grant("woldsvaults:riptide_hover", new JsonPrimitive(30)),
                        grant("woldsvaults:riptide_boost_damage_reduction", new JsonPrimitive(0.75F)),
                        grant("woldsvaults:riptide_bolt", new JsonPrimitive(5.0F)),
                        grant("woldsvaults:riptide_bolt_chilled", new JsonPrimitive(15.0F))));

        GearSet lightningFiend = new GearSet();
        lightningFiend.name = "Lightning Fiend";
        lightningFiend.color = "#6FCFFF";
        lightningFiend.ancientColor = "#6FCFFF";
        lightningFiend.pieces = List.of(
                "woldsvaults:lightning_fiend_crown", "woldsvaults:lightning_fiend_mantle", "woldsvaults:lightning_fiend_legguards",
                "woldsvaults:lightning_fiend_talons", "woldsvaults:lightning_fiend_orb", "woldsvaults:lightning_fiend_pure_lightning");
        lightningFiend.tiers = List.of(
                tier(2, "Channeling strikes deal 50% more damage and chain to 2 more enemies within 6 blocks.",
                        grant("woldsvaults:channeling_damage_more", new JsonPrimitive(0.5F)),
                        grant("woldsvaults:channeling_chain", new JsonPrimitive(2))),
                tier(4, "Your thrown trident pierces up to 3 enemies and calls a channeling strike down on each one it passes through.",
                        grant("woldsvaults:thrown_trident_pierce", new JsonPrimitive(3))),
                tier(6, "Throwing your trident turns you into lightning for 10 seconds: tap a movement key, jump or sneak to dash 7 blocks that way, striking every enemy in your path for 10% of a channeling strike's damage (0.5 second cooldown between dashes). Left click throws a fan of spinning lightning bolts that strike everything they pass through and call lightning down on it; right click marks one as a lightning rod your dashes lock on to; middle click ends the form with a giant lightning strike around you, stronger the more time you have left.",
                        grant("woldsvaults:lightning_dash", new JsonPrimitive(0.1F))));

        GearSet simon = new GearSet();
        simon.name = "Simon's Set";
        simon.color = "#FFD23F";
        simon.ancientColor = "#FFD23F";
        simon.pieces = List.of(
                "woldsvaults:simons_helmet", "woldsvaults:simons_chestplate", "woldsvaults:simons_leggings",
                "woldsvaults:simons_boots");
        simon.tiers = List.of(
                tier(2, "Right click to shoot a prism seed: it grows into a crystal where it lands (one at a time, 4 second cooldown). Damage you deal within 6 blocks of a crystal grows it: Seed, Shard, Cluster, Geode. Unfed crystals shrink a stage every 10 seconds. Arcane, Rail and Prism beams refract from crystal to crystal, and your Prism beams reach 50% further. Brittle: each crystal you have costs 2% Resistance.",
                        grant("woldsvaults:simon_crystals", new JsonPrimitive(1))),
                tier(3, "Lucky Hits from your swings near your crystals make the one you planted bud a new seed around it (up to 4 crystals). With 3 or more Shards, sneak and right click to Resonate: the crystals light up biggest first, then repeat; aim and left click them in that order and each turn bursts for 100% of your ability power per detonation value (Shard 1, Cluster 2, Geode 3.5). Every sequence you get right in a row makes the next one a step longer and the explosions 10% stronger. A wrong click shatters the rest for half and ends the streak.",
                        grant("woldsvaults:simon_budding", new JsonPrimitive(4)),
                        grant("woldsvaults:simon_sequence", new JsonPrimitive(1.0F))),
                tier(4, "Lucky Hits from your swings near a Geode give it a facet (up to 3): +25% burst damage, then a slowing burst, then a Flawless crystal that leaves a shard field. Budded seeds have a 5% chance to be Prismatic, standing in for any crystal in the sequence. A Lucky Hit roll on each correct click fractures the crystal so it bursts twice, and finishing a chain collapses every burst into one final blast.",
                        grant("woldsvaults:simon_facets", new JsonPrimitive(3)),
                        grant("woldsvaults:simon_prismatic", new JsonPrimitive(0.05F)),
                        grant("woldsvaults:simon_fracture", new JsonPrimitive(true)),
                        grant("woldsvaults:simon_finale", new JsonPrimitive(1.0F))));

        GearSet bulletBill = new GearSet();
        bulletBill.name = "Bullet Bill";
        bulletBill.color = "#B9B3C6";
        bulletBill.ancientColor = "#B9B3C6";
        bulletBill.pieces = List.of("woldsvaults:bullet_bill_helmet", "woldsvaults:bullet_bill_leggings");
        bulletBill.variants = new LinkedHashMap<>();
        bulletBill.variants.put("woldsvaults:banzai_bill_helmet", "woldsvaults:bullet_bill_helmet");
        bulletBill.variants.put("woldsvaults:king_bill_helmet", "woldsvaults:bullet_bill_helmet");
        bulletBill.tiers = List.of(
                tier(1, "Double tap jump to become a Bullet Bill for 5 seconds. You fly where you look; hold jump to fire your jets and accelerate, picking up speed in a dive and losing it in a climb. No fall damage while a Bullet Bill or on the landing after. 20 second cooldown once it ends. A Banzai Bill Helmet makes your Bullet Bill 5 times larger; a King Bill Helmet makes it 10 times larger, light red, and burns enemies around it.",
                        grant("woldsvaults:bullet_bill", new JsonPrimitive(100))),
                tier(2, "Your Bullet Bill flies for 5 seconds longer. Bullet Dash as a Bullet Bill sends you into a spin: you pulse red and fly 75% faster for a second, and the dash deals 100% more damage. Right click while flying to arm your detonation: the next time you crash into a block you explode, hitting every enemy nearby for your Bullet Dash damage times your speed in blocks per tick. The faster you crash, the wider the blast. Once armed you keep flying past the end of your time until you crash, or explode where you are 10 seconds later. A bigger Bullet Bill reaches further with both.",
                        grant("woldsvaults:bullet_bill", new JsonPrimitive(100)),
                        grant("woldsvaults:bullet_bill_dash", new JsonPrimitive(1.0F)),
                        grant("woldsvaults:bullet_bill_detonation", new JsonPrimitive(1.0F))));

        this.sets = new LinkedHashMap<>();
        this.sets.put("woldsvaults:tidebreaker", tidebreaker);
        this.sets.put("woldsvaults:lightning_fiend", lightningFiend);
        this.sets.put("woldsvaults:simons_set", simon);
        this.sets.put("woldsvaults:bullet_bill", bulletBill);
    }

    private static Tier tier(int pieces, String text, Grant... grants) {
        Tier tier = new Tier();
        tier.pieces = pieces;
        tier.text = text;
        tier.grants = List.of(grants);
        return tier;
    }

    private static Grant grant(String attribute, JsonElement value) {
        Grant grant = new Grant();
        grant.attribute = attribute;
        grant.value = value;
        return grant;
    }

    private static int parseColor(@Nullable String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
        } catch (NumberFormatException e) {
            WoldsVaults.LOGGER.error("gear_sets.json: colour {} is not a hex colour; using #{}.", hex, Integer.toHexString(fallback));
            return fallback;
        }
    }

    private void index() {
        if (this.setByPiece != null) {
            return;
        }
        Map<ResourceLocation, ResourceLocation> byPiece = new LinkedHashMap<>();
        Map<ResourceLocation, GearSet> byId = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> bases = new LinkedHashMap<>();
        if (this.sets != null) {
            this.sets.forEach((id, set) -> {
                ResourceLocation setId = ResourceLocation.tryParse(id);
                if (setId == null || set == null || set.pieces == null || set.tiers == null) {
                    WoldsVaults.LOGGER.error("gear_sets.json: set {} is malformed (needs a valid id, pieces and tiers); skipping it.", id);
                    return;
                }
                byId.put(setId, set);
                for (String piece : set.pieces) {
                    ResourceLocation pieceId = ResourceLocation.tryParse(piece);
                    if (pieceId == null) {
                        WoldsVaults.LOGGER.error("gear_sets.json: set {} lists an invalid piece id {}; skipping that piece.", id, piece);
                        continue;
                    }
                    ResourceLocation previous = byPiece.put(pieceId, setId);
                    if (previous != null) {
                        WoldsVaults.LOGGER.error("gear_sets.json: {} is listed in both {} and {}; it counts for {} only.", pieceId, previous, setId, setId);
                    }
                }
                if (set.variants != null) {
                    set.variants.forEach((variant, piece) -> {
                        ResourceLocation variantId = ResourceLocation.tryParse(variant);
                        ResourceLocation pieceId = ResourceLocation.tryParse(piece);
                        if (variantId == null || pieceId == null) {
                            WoldsVaults.LOGGER.error("gear_sets.json: set {} has an invalid variant {} -> {}; skipping it.", id, variant, piece);
                            return;
                        }
                        byPiece.put(variantId, setId);
                        bases.put(variantId, pieceId);
                    });
                }
            });
        }
        this.basePieces = bases;
        this.setByPiece = byPiece;
        this.setsById = byId;
    }

    public Optional<ResourceLocation> getSetOf(ResourceLocation uniqueKey) {
        this.index();
        return Optional.ofNullable(this.setByPiece.get(uniqueKey));
    }

    // the piece of its set that a unique counts as: itself, unless it's a variant of another
    public ResourceLocation basePiece(ResourceLocation uniqueKey) {
        this.index();
        return this.basePieces.getOrDefault(uniqueKey, uniqueKey);
    }

    public Optional<GearSet> getSet(ResourceLocation setId) {
        this.index();
        return Optional.ofNullable(this.setsById.get(setId));
    }

    public Map<ResourceLocation, GearSet> getSets() {
        this.index();
        return Collections.unmodifiableMap(this.setsById);
    }

    public Riptide getRiptide() {
        if (this.riptide == null) {
            WoldsVaults.LOGGER.error("gear_sets.json has no riptide section; using the built-in tuning.");
            GearSetConfig defaults = new GearSetConfig();
            defaults.reset();
            this.riptide = defaults.riptide;
        }
        return this.riptide;
    }

    public Lightning getLightning() {
        if (this.lightning == null) {
            WoldsVaults.LOGGER.error("gear_sets.json has no lightning section; using the built-in tuning.");
            GearSetConfig defaults = new GearSetConfig();
            defaults.reset();
            this.lightning = defaults.lightning;
        }
        return this.lightning;
    }

    public Simon getSimon() {
        if (this.simon == null) {
            WoldsVaults.LOGGER.error("gear_sets.json has no simon section; using the built-in tuning.");
            GearSetConfig defaults = new GearSetConfig();
            defaults.reset();
            this.simon = defaults.simon;
        }
        return this.simon;
    }

    public BulletBill getBulletBill() {
        if (this.bulletBill == null) {
            WoldsVaults.LOGGER.error("gear_sets.json has no bulletBill section; using the built-in tuning.");
            GearSetConfig defaults = new GearSetConfig();
            defaults.reset();
            this.bulletBill = defaults.bulletBill;
        }
        return this.bulletBill;
    }

    /** The grants of one tier resolved to registered attributes; unknown attributes are logged and skipped. */
    public static List<ResolvedGrant> resolve(ResourceLocation setId, Tier tier) {
        List<ResolvedGrant> resolved = new ArrayList<>();
        if (tier.grants == null) {
            return resolved;
        }
        for (Grant grant : tier.grants) {
            ResourceLocation attributeId = grant.attribute == null ? null : ResourceLocation.tryParse(grant.attribute);
            VaultGearAttribute<?> attribute = attributeId == null ? null : VaultGearAttributeRegistry.getAttribute(attributeId);
            if (attribute == null || grant.value == null || !grant.value.isJsonPrimitive()) {
                WoldsVaults.LOGGER.error("gear_sets.json: set {} tier {} grants {} = {}, which is not a registered attribute with a plain value; skipping it.", setId, tier.pieces, grant.attribute, grant.value);
                continue;
            }
            JsonPrimitive primitive = grant.value.getAsJsonPrimitive();
            Object raw = primitive.isBoolean() ? primitive.getAsBoolean() : primitive.isNumber() ? primitive.getAsNumber() : primitive.getAsString();
            Object value;
            try {
                value = attribute.getType().cast(raw);
            } catch (RuntimeException e) {
                WoldsVaults.LOGGER.error("gear_sets.json: set {} tier {} grant {} = {} does not fit the attribute's type; skipping it.", setId, tier.pieces, grant.attribute, grant.value);
                continue;
            }
            resolved.add(new ResolvedGrant(attribute, value));
        }
        return resolved;
    }

    public record ResolvedGrant(VaultGearAttribute<?> attribute, Object value) {
    }
}
