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
 * holds the shared riptide tuning the set mechanics read on both sides.
 */
public class GearSetConfig extends PackAuthoredConfig {
    @Expose private Map<String, String> documentation;
    @Expose private Riptide riptide;
    @Expose private Map<String, GearSet> sets;

    private transient Map<ResourceLocation, ResourceLocation> setByPiece;
    private transient Map<ResourceLocation, GearSet> setsById;

    public static class Riptide {
        @Expose public float hoverHorizontalFriction;
        @Expose public float hoverVerticalDamping;
        @Expose public float shockConeAhead;
        @Expose public float shockConeBehind;
        @Expose public float shockConeRadius;
        @Expose public int stormVisualBoltsPerDash;
        @Expose public Float contactInflate;
        @Expose public Float stallSpeed;
        @Expose public Integer launchGraceTicks;

        /** Blocks the swept box that finds riptide contacts grows by on every side. */
        public float contactInflate() {
            if (this.contactInflate == null) {
                warnMissing("contactInflate", DEFAULT_CONTACT_INFLATE);
                this.contactInflate = DEFAULT_CONTACT_INFLATE;
            }
            return this.contactInflate;
        }

        /** Horizontal blocks per tick below which a dash that runs into a wall ends; faster dashes slide along it. */
        public float stallSpeed() {
            if (this.stallSpeed == null) {
                warnMissing("stallSpeed", DEFAULT_STALL_SPEED);
                this.stallSpeed = DEFAULT_STALL_SPEED;
            }
            return this.stallSpeed;
        }

        /** Ticks after a launch during which a wall never ends the dash. */
        public int launchGraceTicks() {
            if (this.launchGraceTicks == null) {
                warnMissing("launchGraceTicks", DEFAULT_LAUNCH_GRACE_TICKS);
                this.launchGraceTicks = DEFAULT_LAUNCH_GRACE_TICKS;
            }
            return this.launchGraceTicks;
        }

        private static void warnMissing(String key, Object value) {
            WoldsVaults.LOGGER.warn("gear_sets.json riptide section has no {}; using the built-in {}.", key, value);
        }
    }

    private static final float DEFAULT_CONTACT_INFLATE = 0.5F;
    private static final float DEFAULT_STALL_SPEED = 0.1F;
    private static final int DEFAULT_LAUNCH_GRACE_TICKS = 3;

    public static class GearSet {
        @Expose public String name;
        @Expose public String color;
        @Expose public String ancientColor;
        @Expose public List<String> pieces;
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
        this.documentation.put("sets", "Set id -> name, colours, pieces (unique ids from unique_gear) and tiers. A piece counts while it sits in the player's attribute snapshot (worn in its intended slot, not broken, not above the player's level). Each unique counts once.");
        this.documentation.put("tiers", "pieces = pieces needed; text = the tooltip line; grants = gear attributes added to the wearer's snapshot while the tier is active (values in the attribute's own units, 1.0 = 100%)");
        this.documentation.put("riptide", "Shared riptide tuning: hover friction is the extra per-tick horizontal speed multiplier during the hover, vertical damping the per-tick vertical one; the shock cone spans shockConeAhead blocks ahead of the player to shockConeBehind blocks behind, widening to shockConeRadius; contactInflate grows the box that finds dash contacts by that many blocks per side; a dash that hits a wall ends only once its horizontal speed is below stallSpeed blocks per tick, and never in its first launchGraceTicks ticks");

        this.riptide = new Riptide();
        this.riptide.hoverHorizontalFriction = 0.8F;
        this.riptide.hoverVerticalDamping = 0.5F;
        this.riptide.shockConeAhead = 2.0F;
        this.riptide.shockConeBehind = 3.0F;
        this.riptide.shockConeRadius = 4.0F;
        this.riptide.stormVisualBoltsPerDash = 3;
        this.riptide.contactInflate = DEFAULT_CONTACT_INFLATE;
        this.riptide.stallSpeed = DEFAULT_STALL_SPEED;
        this.riptide.launchGraceTicks = DEFAULT_LAUNCH_GRACE_TICKS;

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
                tier(6, "While dashing, a storm shrouds you: you hover briefly after each dash, take 75% less damage, and lightning strikes every enemy your dash or its shock cone hits for 5x damage (15x if chilled).",
                        grant("woldsvaults:riptide_hover", new JsonPrimitive(30)),
                        grant("woldsvaults:riptide_boost_damage_reduction", new JsonPrimitive(0.75F)),
                        grant("woldsvaults:riptide_bolt", new JsonPrimitive(5.0F)),
                        grant("woldsvaults:riptide_bolt_chilled", new JsonPrimitive(15.0F))));
        this.sets = new LinkedHashMap<>();
        this.sets.put("woldsvaults:tidebreaker", tidebreaker);
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
            });
        }
        this.setByPiece = byPiece;
        this.setsById = byId;
    }

    public Optional<ResourceLocation> getSetOf(ResourceLocation uniqueKey) {
        this.index();
        return Optional.ofNullable(this.setByPiece.get(uniqueKey));
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
