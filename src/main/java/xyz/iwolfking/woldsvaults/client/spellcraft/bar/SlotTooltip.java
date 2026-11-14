package xyz.iwolfking.woldsvaults.client.spellcraft.bar;

import iskallia.vault.skill.base.Skill;
import iskallia.vault.skill.base.SpecializedSkill;
import iskallia.vault.skill.base.TieredSkill;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.GlyphRelevance;
import xyz.iwolfking.woldsvaults.spellcraft.glyph.GlyphStacking;
import xyz.iwolfking.woldsvaults.config.SpellcraftConfig;
import xyz.iwolfking.woldsvaults.spellcraft.play.SpellCooldowns;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.GlyphText;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftDraw;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftNames;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftPalette;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.StatText;
import xyz.iwolfking.woldsvaults.client.spellcraft.state.ClientSpellBar;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.SlotCostStats;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.StepStats;
import xyz.iwolfking.woldsvaults.init.ModConfigs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Style;

final class SlotTooltip {
    private static final int WRAP = 200;

    private SlotTooltip() {
    }

    static List<Component> tooltip(SpellBarStrip strip, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (SpellcraftDraw.inside(mouseX, mouseY, strip.pickX(0), strip.y0() + 2, SpellBarStrip.PICK, SpellBarStrip.PICK)) {
            lines.add(new TextComponent(SpellcraftNames.tr("bar.default")));
            return lines;
        }
        SpellBarView item = strip.host().effectiveView();
        if (item != null && SpellcraftDraw.inside(mouseX, mouseY, strip.pickX(1), strip.y0() + 2, SpellBarStrip.PICK, SpellBarStrip.PICK)) {
            lines.add(new TextComponent(SpellcraftNames.tr("bar.item"))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(SpellcraftPalette.ARCANE_GLOW & 0xFFFFFF))));
            return lines;
        }
        if (SpellcraftDraw.inside(mouseX, mouseY, strip.keysX(), strip.y0() + 4, SpellBarStrip.KEYS_W, SpellBarStrip.KEYS_H)) {
            lines.add(new TextComponent(SpellcraftNames.tr("bar.keys_hint")));
            return lines;
        }
        for (int i = 0; i < SpellcraftItemData.SLOTS; i++) {
            if (SpellcraftDraw.inside(mouseX, mouseY, SpellBarStrip.slotX(strip.x0(), i), strip.y0() + 4, 16, 16)) {
                return slotTooltip(strip.host(), strip.lastView(), i);
            }
        }
        return lines;
    }

    private static List<Component> slotTooltip(SpellBarStrip.Host host, SpellBarView view, int index) {
        List<Component> lines = new ArrayList<>();
        AbilityTree tree = host.tree();
        SpellcraftConfig config = ModConfigs.SPELLCRAFT;
        SpellBarView.Slot slot = view.slot(index);
        switch (slot.state()) {
            case ABILITY -> {
                lines.add(SpellcraftNames.colored(SpellcraftNames.abilityName(tree, slot.group()) + tierSuffix(tree, slot.group()), SpellcraftPalette.NAME_GOLD));
                StepStats.Step stats = StepStats.plain(tree, slot.group());
                if (stats != null) {
                    lines.addAll(statLines(stats, ""));
                }
                SlotCostStats.Numbers numbers = SlotCostStats.plain(tree, slot.group());
                if (numbers != null) {
                    lines.addAll(numberLines(numbers));
                }
            }
            case SPELL -> {
                lines.add(SpellcraftNames.colored(slot.spellName(), slot.implicit() ? SpellBarStrip.IMPLICIT_RING : SpellcraftPalette.ARCANE_GLOW));
                lines.add(spellTitle(tree, slot.groups()));
                if (slot.implicit()) {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.implicit"), SpellBarStrip.IMPLICIT_RING));
                } else if (!slot.sourceName().isEmpty()) {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.source", slot.sourceName(), slot.prio()), SpellcraftPalette.GREY));
                }
                if (slot.locked()) {
                    wrapped(lines, SpellcraftNames.tr("bar.locked", slot.lockReason()), SpellcraftPalette.BAD);
                }
                if (config != null && slot.model() != null && !slot.locked()) {
                    StepStats.SpellStats stats = StepStats.spell(config, tree, slot.model());
                    if (stats != null) {
                        lines.addAll(spellLines(tree, stats));
                    }
                    SlotCostStats.Numbers numbers = SlotCostStats.spell(config, tree, slot.model());
                    if (numbers != null) {
                        lines.addAll(numberLines(numbers));
                    }
                }
                if (slot.model() != null) {
                    for (var glyph : slot.model().glyphs()) {
                        if (!glyph.active()) {
                            lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.inactive_glyph", glyph.type().displayName(), GlyphText.inactiveReason(config, glyph)),
                                    SpellcraftPalette.DARK));
                        }
                    }
                }
                var source = ClientSpellBar.source(slot.itemKey());
                if (source != null && source.budgetInactive() > 0) {
                    wrapped(lines, SpellcraftNames.tr("bar.over_budget", source.name(), source.budgetInactive()), SpellcraftPalette.BAD);
                }
                for (String shadowed : slot.shadowed()) {
                    wrapped(lines, SpellcraftNames.tr("bar.shadowed", shadowed), SpellcraftPalette.DARK);
                }
            }
            case HIDDEN -> {
                int by = view.hiddenBy(index);
                String name = by < 0 ? "?" : view.slot(by).state() == SpellBarView.State.SPELL ? view.slot(by).spellName()
                        : SpellcraftNames.abilityName(tree, view.slot(by).group());
                lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.hidden", name, by + 1, SpellcraftNames.groupName(tree, slot.group())), SpellcraftPalette.BAD));
            }
            case EMPTY -> lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.empty"), SpellcraftPalette.GREY));
        }
        if (slot.unlearned()) {
            for (String group : slot.groups()) {
                if (!SpellBarView.learned(tree, group)) {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.unlearned", SpellcraftNames.groupName(tree, group)), SpellcraftPalette.BAD));
                }
            }
        }
        SpellCooldowns.Blocking blocking = slot.filled() ? SpellCooldowns.blocking(tree, slot.groups()) : null;
        if (blocking != null) {
            lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.waiting", SpellcraftNames.groupName(tree, blocking.group()),
                    StatText.seconds(blocking.ticks())), SpellcraftPalette.STAT_COOLDOWN));
        }
        String key = SpellcraftNames.keyName(index);
        String keyLine = key.isEmpty() ? SpellcraftNames.tr("bar.no_key", index + 1) : SpellcraftNames.tr("bar.key", index + 1, key);
        lines.add(SpellcraftNames.colored(keyLine, SpellcraftPalette.GREY));
        String hint;
        if (view.isEffective()) {
            hint = SpellcraftNames.tr("bar.hint_item");
        } else if (host.selectedGroup() != null) {
            hint = SpellcraftNames.tr("bar.hint_default", SpellcraftNames.abilityName(tree, host.selectedGroup()));
        } else {
            hint = SpellcraftNames.tr("bar.hint_choose");
        }
        lines.add(new TextComponent(hint).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    /**
     * "Augmented: Ice Bolt 21 + Nova 9": the prefix and the separators grey, each ability name gold as in plain ability
     * tooltips, each tier yellow.
     */
    private static Component spellTitle(AbilityTree tree, List<String> groups) {
        MutableComponent names = new TextComponent("");
        for (int i = 0; i < groups.size(); i++) {
            String group = groups.get(i);
            if (i > 0) {
                names.append(SpellcraftNames.colored(" + ", SpellcraftPalette.GREY));
            }
            names.append(SpellcraftNames.colored(SpellcraftNames.groupName(tree, group), SpellcraftPalette.NAME_GOLD));
            String tier = tierSuffix(tree, group);
            if (!tier.isEmpty()) {
                names.append(SpellcraftNames.colored(tier, SpellcraftPalette.STAT_YELLOW));
            }
        }
        return new TranslatableComponent("woldsvaults.spellcraft.bar.augmented", names)
                .withStyle(style -> style.withColor(TextColor.fromRgb(SpellcraftPalette.GREY & 0xFFFFFF)));
    }

    /** Adds {@code text} split into lines of at most {@link #WRAP} px, so long reasons stay inside narrow screens. */
    private static void wrapped(List<Component> lines, String text, int color) {
        Minecraft.getInstance().font.getSplitter().splitLines(text, WRAP, Style.EMPTY)
                .forEach(line -> lines.add(SpellcraftNames.colored(line.getString(), color)));
    }

    /** Mana (blue), cooldown (gold) and spell haste (violet) lines, the label grey and the numbers coloured. */
    private static List<Component> numberLines(SlotCostStats.Numbers numbers) {
        List<Component> lines = new ArrayList<>();
        if (numbers.hasMana()) {
            String before = StatText.compact(Math.round(numbers.manaBefore()));
            String after = StatText.compact(Math.round(numbers.manaAfter()));
            String mana = before.equals(after) ? after : SpellcraftNames.tr("fmt.arrow", before, after);
            lines.add(labelled(SpellcraftNames.tr("bar.mana_label"), mana, SpellcraftPalette.STAT_MANA));
        }
        String cooldown = numbers.cooldownBefore() == numbers.cooldownAfter()
                ? SpellcraftNames.tr("fmt.seconds", StatText.seconds(numbers.cooldownAfter()))
                : SpellcraftNames.tr("fmt.arrow", SpellcraftNames.tr("fmt.seconds", StatText.seconds(numbers.cooldownBefore())),
                SpellcraftNames.tr("fmt.seconds", StatText.seconds(numbers.cooldownAfter())));
        lines.add(labelled(SpellcraftNames.tr("bar.cooldown_label"), cooldown, SpellcraftPalette.STAT_COOLDOWN));
        Integer haste = SlotCostStats.hastePercent();
        if (haste != null && haste != 0) {
            lines.add(labelled(SpellcraftNames.tr("bar.haste_label"), (haste > 0 ? "+" : "") + haste + "%", SpellcraftPalette.STAT_COOLDOWN));
        }
        return lines;
    }

    /**
     * A spell's stat lines: with one row, that step's lines; with more, each ability step and Bundled Spell orb under its
     * own header, indented by payload depth, then the whole press's damage when several steps deal damage.
     */
    private static List<Component> spellLines(AbilityTree tree, StepStats.SpellStats stats) {
        List<Component> lines = new ArrayList<>();
        if (stats.rows().size() == 1 && stats.rows().get(0) instanceof StepStats.Step step) {
            lines.addAll(statLines(step, ""));
            return lines;
        }
        for (StepStats.Row row : stats.rows()) {
            String indent = "  ".repeat(row.depth());
            if (row instanceof StepStats.Step step) {
                lines.add(SpellcraftNames.colored(indent + SpellcraftNames.groupName(tree, step.group()), SpellcraftPalette.NAME_GOLD));
                lines.addAll(statLines(step, indent + "  "));
            } else if (row instanceof StepStats.Orb orb) {
                lines.add(SpellcraftNames.colored(indent + SpellcraftNames.tr("bar.stat.orb_step"), SpellcraftPalette.ARCANE_GLOW));
                String inner = indent + "  ";
                if (orb.copies() > 1) {
                    lines.add(labelled(inner + SpellcraftNames.tr("bar.stat.orbs"), String.valueOf(orb.copies()), SpellcraftPalette.STAT_PLAIN));
                }
                lines.add(labelled(inner + SpellcraftNames.tr("bar.stat.velocity"),
                        SpellcraftNames.tr("fmt.blocks_per_second", StatText.compact(orb.velocity())), SpellcraftPalette.STAT_YELLOW));
                lines.add(labelled(inner + SpellcraftNames.tr("bar.stat.flight"),
                        SpellcraftNames.tr("fmt.seconds", StatText.trim(orb.flightSeconds())), SpellcraftPalette.STAT_YELLOW));
                glyphLine(orb.glyphs(), orb.relevance(), inner, lines);
            }
        }
        if (stats.total() != null) {
            lines.add(labelled(SpellcraftNames.tr("bar.stat.spell_total"), StatText.hit(stats.total()), SpellcraftPalette.STAT_DAMAGE));
        }
        return lines;
    }

    /**
     * One step's stats, one per line: damage (and total, splinter, splash), the ability's main effects after its buff
     * glyphs, radius, velocity, duration, then VH's other numbers.
     */
    private static List<Component> statLines(StepStats.Step step, String indent) {
        List<Component> lines = new ArrayList<>();
        if (step.hit() != null) {
            String hit = StatText.hit(step.hit());
            if (step.charged() != null) {
                hit = SpellcraftNames.tr("fmt.charged", hit, StatText.hit(step.charged()));
            }
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.damage"), hit, SpellcraftPalette.STAT_DAMAGE));
        }
        if (step.total() != null) {
            String total = StatText.hit(step.total());
            int echo = Math.round(step.echoPower() * 100.0F);
            if (step.echoPower() > 0.0F) {
                total = step.casts() > 1 ? SpellcraftNames.tr("fmt.casts_echo", total, step.casts(), echo) : SpellcraftNames.tr("fmt.echo", total, echo);
            } else {
                total = SpellcraftNames.tr("fmt.casts", total, step.casts());
            }
            if (step.chargedTotal() != null) {
                total = SpellcraftNames.tr("fmt.charged", total, StatText.hit(step.chargedTotal()));
            }
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.total"), total, SpellcraftPalette.STAT_DAMAGE));
        }
        if (step.splinter() != null) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.splinter"), StatText.hit(step.splinter()), SpellcraftPalette.STAT_DAMAGE));
        }
        if (step.splash() != null) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.splash"), StatText.hit(step.splash()), SpellcraftPalette.STAT_DAMAGE));
        }
        for (StepStats.Extra main : step.mains()) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat." + main.key()), main.value(), main.color()));
        }
        if (step.radius() >= 0.0F) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.radius"),
                    SpellcraftNames.tr("fmt.blocks", StatText.compact(step.radius())), SpellcraftPalette.STAT_AREA));
        }
        if (step.splashRadius() >= 0.0F) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.splash_radius"),
                    SpellcraftNames.tr("fmt.blocks", StatText.compact(step.splashRadius())), SpellcraftPalette.STAT_AREA));
        }
        if (step.velocity() >= 0.0F) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.velocity"),
                    SpellcraftNames.tr("fmt.blocks_per_second", StatText.compact(step.velocity())), SpellcraftPalette.STAT_YELLOW));
        }
        if (step.durationTicks() >= 0) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat.duration"),
                    SpellcraftNames.tr("fmt.seconds", StatText.seconds(step.durationTicks())), SpellcraftPalette.STAT_YELLOW));
        }
        for (StepStats.Extra extra : step.extras()) {
            lines.add(labelled(indent + SpellcraftNames.tr("bar.stat." + extra.key()), extra.value(), extra.color()));
        }
        if (step.glyphs() != null) {
            glyphLine(step.glyphs(), step.relevance(), indent, lines);
        }
        return lines;
    }

    /**
     * The step's stacked glyph stats ("+48% dmg, +22% area; 0.86x area"), numbers coloured by stat, listing only the stats
     * that apply to the step; nothing when no glyph moves one.
     */
    private static void glyphLine(GlyphStacking.Pool pool, GlyphRelevance.Filter relevance, String indent, List<Component> lines) {
        String summary = GlyphText.poolSummary(pool, relevance);
        if (!summary.isEmpty()) {
            lines.add(SpellcraftNames.colored(indent + SpellcraftNames.tr("bar.stat.glyphs"), SpellcraftPalette.GREY).copy().append(GlyphText.tagged(summary)));
        }
    }

    private static Component labelled(String label, String value, int color) {
        return new TextComponent(label).withStyle(style -> style.withColor(TextColor.fromRgb(SpellcraftPalette.GREY & 0xFFFFFF)))
                .append(new TextComponent(value).withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
    }

    private static String tierSuffix(AbilityTree tree, String group) {
        Skill skill = tree.getForId(group).orElse(null);
        if (skill instanceof SpecializedSkill specialized && specialized.getSpecialization() instanceof TieredSkill tiered && tiered.isUnlocked()) {
            return " " + tiered.getActualTier();
        }
        return "";
    }
}
