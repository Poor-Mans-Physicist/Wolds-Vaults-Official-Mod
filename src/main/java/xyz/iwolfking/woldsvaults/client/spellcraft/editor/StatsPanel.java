package xyz.iwolfking.woldsvaults.client.spellcraft.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftDraw;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftNames;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftPalette;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.StatText;
import xyz.iwolfking.woldsvaults.client.spellcraft.state.ClientSpellBar;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.SlotCostStats;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.StepStats;
import xyz.iwolfking.woldsvaults.config.SpellcraftConfig;
import xyz.iwolfking.woldsvaults.spellcraft.item.SpellcraftItemData;
import xyz.iwolfking.woldsvaults.spellcraft.item.StoredSpell;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellModel;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellcraftRules;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StatsPanel {
    static final int COOLDOWN_TEXT = 0xFF6A3A00;
    private static final int MANA_TEXT = 0xFF1F4FA0;
    private static final int HIDES_TEXT = 0xFF8A1A1A;
    private static final int DIVIDER = 0xFF8B8B8B;
    private static final int STEPPER_TEXT = 0xFF3A2A80;

    enum StatRow {
        MANA("screen.stat.mana"),
        COOLDOWN("screen.stat.cooldown"),
        CASTS("screen.stat.casts"),
        ARCANA("screen.stat.arcana"),
        SLOT("screen.stat.slot"),
        PRIORITY("screen.stat.priority");

        final String key;

        StatRow(String key) {
            this.key = key;
        }
    }

    private final SpellcraftScreen screen;
    private final SpellDrafts drafts;

    StatsPanel(SpellcraftScreen screen, SpellDrafts drafts) {
        this.screen = screen;
        this.drafts = drafts;
    }

    void render(PoseStack pose, SpellcraftConfig config, AbilityTree tree, @Nullable SpellDrafts.Draft draft,
                @Nullable SpellcraftRules.ItemCheck check, @Nullable SpellModel model, int x0, int y0, int x1, int y1) {
        Font font = this.screen.font();
        SpellcraftDraw.insetGrey(pose, x0, y0, x1, y1);
        int mid = (x0 + x1) / 2;
        int lw = mid - x0 - 8;
        int rw = x1 - mid - 8;
        GuiComponent.fill(pose, mid, y0 + 3, mid + 1, y1 - 3, DIVIDER);
        int rows = (y1 - y0 - 4) / EditorLayout.STAT_PITCH;
        List<String[]> left = new ArrayList<>();
        List<Integer> leftColors = new ArrayList<>();
        SlotCostStats.Numbers numbers = model == null || model.groups().isEmpty() ? null : SlotCostStats.spell(config, tree, model);
        String mana = "-";
        String cooldown = "-";
        if (numbers != null) {
            mana = numbers.hasMana() ? SpellcraftNames.tr("fmt.arrow", StatText.compact(Math.round(numbers.manaBefore())),
                    StatText.compact(Math.round(numbers.manaAfter()))) : "-";
            cooldown = SpellcraftNames.tr("fmt.arrow", StatText.seconds(numbers.cooldownBefore()),
                    SpellcraftNames.tr("fmt.seconds", StatText.seconds(numbers.cooldownAfter())));
        }
        addStat(left, leftColors, StatRow.MANA, mana, MANA_TEXT);
        addStat(left, leftColors, StatRow.COOLDOWN, cooldown, COOLDOWN_TEXT);
        addStat(left, leftColors, StatRow.CASTS, model == null ? "-" : String.valueOf(model.casts()), SpellcraftPalette.LABEL);
        addStat(left, leftColors, StatRow.ARCANA, model == null ? "-" : Math.round(model.arcana()) + " (x"
                + String.format(Locale.ROOT, "%.2f", model.escalation()) + ")", SpellcraftPalette.ARCANA_TEXT);
        addStat(left, leftColors, StatRow.SLOT, stepper(slotText(draft)), STEPPER_TEXT);
        addStat(left, leftColors, StatRow.PRIORITY, stepper(prioText(config, draft)), STEPPER_TEXT);
        for (int i = 0; i < Math.min(rows, left.size()); i++) {
            statRow(pose, x0 + 5, y0 + 4 + i * EditorLayout.STAT_PITCH, lw, left.get(i)[0], left.get(i)[1], leftColors.get(i));
        }
        int status = rows - 1;
        int rx = mid + 4;
        List<String> errors = this.drafts.errors(draft, check);
        if (!errors.isEmpty()) {
            List<String> wrapped = new ArrayList<>();
            for (String error : errors) {
                font.getSplitter().splitLines(error, rw, Style.EMPTY)
                        .forEach(line -> wrapped.add(line.getString()));
            }
            int room = status;
            boolean overflow = wrapped.size() > room;
            int shown = overflow ? room - 1 : wrapped.size();
            for (int i = 0; i < shown; i++) {
                font.draw(pose, wrapped.get(i), rx, y0 + 4 + i * EditorLayout.STAT_PITCH, SpellcraftPalette.BAD_TEXT);
            }
            if (overflow && room > 0) {
                font.draw(pose, SpellcraftNames.tr("screen.more_errors", wrapped.size() - shown), rx, y0 + 4 + shown * EditorLayout.STAT_PITCH, SpellcraftPalette.DARK);
            }
        } else if (model != null) {
            List<String[]> steps = stepRows(config, tree, model);
            for (int i = 0; i < Math.min(status, steps.size()); i++) {
                statRow(pose, rx + 1, y0 + 4 + i * EditorLayout.STAT_PITCH, rw, steps.get(i)[0], steps.get(i)[1], SpellcraftPalette.LABEL);
            }
        }
        if (status >= 0 && draft != null) {
            SpellDrafts.Status state = this.drafts.status(draft, check);
            font.draw(pose, font.plainSubstrByWidth(state.text(), rw), rx, y0 + 4 + status * EditorLayout.STAT_PITCH, state.color());
        }
    }

    private void addStat(List<String[]> rows, List<Integer> colors, StatRow row, String value, int color) {
        rows.add(new String[]{SpellcraftNames.tr(row.key), value});
        colors.add(color);
    }

    private void statRow(PoseStack pose, int x, int y, int width, String label, String value, int valueColor) {
        Font font = this.screen.font();
        int vw = font.width(value);
        String cutLabel = font.plainSubstrByWidth(label, Math.max(0, width - Math.min(vw, width / 2) - 4));
        font.draw(pose, cutLabel, x, y, SpellcraftPalette.LABEL);
        String cutValue = font.plainSubstrByWidth(value, width - font.width(cutLabel) - 4);
        font.draw(pose, cutValue, x + width - font.width(cutValue), y, valueColor);
    }

    /** Default-bar slots this spell would hide: abilities it casts that sit on another default slot. */
    private String hides(AbilityTree tree, @Nullable SpellDrafts.Draft draft, SpellModel model) {
        StoredSpell stored = this.drafts.stored(draft);
        int own = stored == null ? -1 : stored.slot();
        List<String> hidden = new ArrayList<>();
        for (int i = 0; i < SpellcraftItemData.SLOTS; i++) {
            String group = ClientSpellBar.defaultSlot(i);
            if (i != own && !group.isEmpty() && model.groups().contains(group)) {
                hidden.add(SpellcraftNames.tr("screen.slot_ref", SpellcraftNames.groupName(tree, group), i + 1));
            }
        }
        if (hidden.isEmpty()) {
            return "-";
        }
        return hidden.size() == 1 ? hidden.get(0) : SpellcraftNames.tr("screen.and_more", hidden.get(0), hidden.size() - 1);
    }

    private static String stepper(String value) {
        return value.equals("-") ? value : "< " + value + " >";
    }

    private String slotText(@Nullable SpellDrafts.Draft draft) {
        StoredSpell stored = this.drafts.stored(draft);
        if (stored == null) {
            return "-";
        }
        return stored.assigned() ? String.valueOf(stored.slot() + 1) : SpellcraftNames.tr("screen.slot_none");
    }

    private String prioText(SpellcraftConfig config, @Nullable SpellDrafts.Draft draft) {
        StoredSpell stored = this.drafts.stored(draft);
        return stored == null ? "-" : String.valueOf(SpellcraftRules.priority(config, stored));
    }

    /** The left stat column's row for a stat key, or -1 when the panel is too short to show it. */
    private int statRowIndex(StatRow row) {
        int index = row.ordinal();
        int rows = EditorLayout.statRows(this.screen.height);
        return index < rows ? index : -1;
    }

    private boolean overStatRow(double mouseX, double mouseY, StatRow key) {
        int row = statRowIndex(key);
        if (row < 0) {
            return false;
        }
        int x0 = EditorLayout.innerX0();
        int mid = EditorLayout.statsMid(this.screen.width);
        int y = EditorLayout.statRowY(row) - 1;
        return SpellcraftDraw.inside(mouseX, mouseY, x0 + 2, y, mid - x0 - 4, EditorLayout.STAT_PITCH);
    }

    /** One row per ability step and Bundled Spell orb, labelled by name, valued by {@link #stepValue}. */
    private List<String[]> stepRows(SpellcraftConfig config, AbilityTree tree, SpellModel model) {
        List<String[]> rows = new ArrayList<>();
        StepStats.SpellStats stats = StepStats.spell(config, tree, model);
        if (stats == null) {
            return rows;
        }
        for (StepStats.Row row : stats.rows()) {
            if (row instanceof StepStats.Step step) {
                rows.add(new String[]{SpellcraftNames.groupName(tree, step.group()), stepValue(step)});
            } else if (row instanceof StepStats.Orb orb) {
                List<String> parts = new ArrayList<>();
                if (orb.copies() > 1) {
                    parts.add(SpellcraftNames.tr("screen.step.casts", orb.copies()));
                }
                parts.add(SpellcraftNames.tr("screen.step.velocity", StatText.compact(orb.velocity())));
                parts.add(SpellcraftNames.tr("fmt.seconds", StatText.trim(orb.flightSeconds())));
                rows.add(new String[]{SpellcraftNames.tr("screen.step.bundle"), String.join(" ", parts)});
            }
        }
        return rows;
    }

    /**
     * A step's numbers in one short cell, most important first since the row cuts the end: the hit, the casts, the
     * radius, then Echo; without damage, the radius, velocity and duration.
     */
    private static String stepValue(StepStats.Step step) {
        List<String> parts = new ArrayList<>();
        if (step.hit() != null) {
            parts.add(StatText.hit(step.hit()));
            if (step.casts() > 1) {
                parts.add(SpellcraftNames.tr("screen.step.casts", step.casts()));
            }
            if (step.radius() >= 0.0F) {
                parts.add(SpellcraftNames.tr("screen.step.radius", StatText.compact(step.radius())));
            }
            if (step.echoPower() > 0.0F) {
                parts.add(SpellcraftNames.tr("screen.step.echo", Math.round(step.echoPower() * 100.0F)));
            }
        } else {
            if (step.radius() >= 0.0F) {
                parts.add(SpellcraftNames.tr("screen.step.radius", StatText.compact(step.radius())));
            }
            if (step.velocity() >= 0.0F) {
                parts.add(SpellcraftNames.tr("screen.step.velocity", StatText.compact(step.velocity())));
            }
            if (step.durationTicks() >= 0) {
                parts.add(SpellcraftNames.tr("fmt.seconds", StatText.seconds(step.durationTicks())));
            }
        }
        return parts.isEmpty() ? "-" : String.join(" ", parts);
    }

    @Nullable
    List<Component> stepperTooltip(@Nullable SpellDrafts.Draft draft, @Nullable SpellcraftRules.ItemCheck check, int mouseX, int mouseY) {
        if (overStatRow(mouseX, mouseY, StatRow.SLOT) || overStatRow(mouseX, mouseY, StatRow.PRIORITY)) {
            List<Component> lines = new ArrayList<>();
            boolean slot = overStatRow(mouseX, mouseY, StatRow.SLOT);
            if (this.drafts.stored(draft) == null) {
                lines.add(SpellcraftNames.colored(SpellcraftNames.tr("screen.place_unsaved"), SpellcraftPalette.GREY));
            } else {
                lines.add(SpellcraftNames.colored(SpellcraftNames.tr(slot ? "screen.slot_hint" : "screen.priority_hint"), SpellcraftPalette.WHITE));
                if (ClientSpellBar.source(SpellcraftItemData.key(this.drafts.stack())) == null) {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.tr("screen.not_equipped"), SpellcraftPalette.BAD));
                }
                SpellModel model = check == null ? null : check.models().get(draft.id > 0 ? draft.id : StoredSpell.UNSAVED_ID);
                if (slot && model != null) {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.tr("screen.stat.hides") + ": " + hides(this.screen.tree(), draft, model), HIDES_TEXT));
                }
            }
            lines.add(new TextComponent(SpellcraftNames.tr("screen.stepper_hint")).withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        return null;
    }

    List<Component> errorTooltip(@Nullable SpellDrafts.Draft draft, @Nullable SpellcraftRules.ItemCheck check, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        int sy0 = EditorLayout.statsTop();
        int mid = EditorLayout.statsMid(this.screen.width);
        List<String> errors = this.drafts.errors(draft, check);
        if (!errors.isEmpty() && SpellcraftDraw.inside(mouseX, mouseY, mid, sy0, EditorLayout.innerX1(this.screen.width) - mid,
                EditorLayout.statsBottom(this.screen.height) - sy0)) {
            errors.forEach(error -> lines.add(SpellcraftNames.colored(error, SpellcraftPalette.BAD)));
        }
        return lines;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            for (StatRow row : List.of(StatRow.SLOT, StatRow.PRIORITY)) {
                if (overStatRow(mouseX, mouseY, row)) {
                    this.drafts.stepPlacement(row == StatRow.SLOT, button == 0 ? 1 : -1);
                    return true;
                }
            }
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, int step) {
        for (StatRow row : List.of(StatRow.SLOT, StatRow.PRIORITY)) {
            if (overStatRow(mouseX, mouseY, row)) {
                this.drafts.stepPlacement(row == StatRow.SLOT, -step);
                return true;
            }
        }
        return false;
    }
}
