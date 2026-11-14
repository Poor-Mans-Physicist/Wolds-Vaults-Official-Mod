package xyz.iwolfking.woldsvaults.client.spellcraft.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.gui.framework.ScreenTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import xyz.iwolfking.woldsvaults.client.spellcraft.bar.SpellBarStrip;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.GlyphText;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftDraw;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftNames;
import xyz.iwolfking.woldsvaults.client.spellcraft.gui.SpellcraftPalette;
import xyz.iwolfking.woldsvaults.client.spellcraft.stats.GlyphRelevance;
import xyz.iwolfking.woldsvaults.config.SpellcraftConfig;
import xyz.iwolfking.woldsvaults.spellcraft.ability.AbilityLookup;
import xyz.iwolfking.woldsvaults.spellcraft.glyph.GlyphType;
import xyz.iwolfking.woldsvaults.spellcraft.item.StoredSpell;
import xyz.iwolfking.woldsvaults.spellcraft.source.GlyphViewLine;
import xyz.iwolfking.woldsvaults.spellcraft.spell.PlacedGlyph;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellModel;
import xyz.iwolfking.woldsvaults.spellcraft.spell.SpellcraftRules;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class WeavePanel {
    private static final int PAYLOAD = 0xFFB46CF0;
    private static final int THREAD_DIM = 0xFF3A2A55;
    private static final int PLUS = 0xFF8A8A8A;

    private final SpellcraftScreen screen;
    private final SpellDrafts drafts;
    private final SpellBarStrip strip;

    WeavePanel(SpellcraftScreen screen, SpellDrafts drafts, SpellBarStrip strip) {
        this.screen = screen;
        this.drafts = drafts;
        this.strip = strip;
    }

    private static int payloadStart(SpellDrafts.Draft draft) {
        int bundle = draft.tokens.indexOf(GlyphType.BUNDLED_SPELL.id());
        if (bundle < 0) {
            return -1;
        }
        for (int i = bundle + 1; i < draft.tokens.size(); i++) {
            if (GlyphType.byId(draft.tokens.get(i)) == null) {
                return i;
            }
        }
        return -1;
    }

    void render(PoseStack pose, @Nullable SpellDrafts.Draft draft, @Nullable SpellModel model, int x0, int y0, int x1, int y1, int mouseX,
                int mouseY) {
        Font font = this.screen.font();
        SpellcraftDraw.starfield(pose, x0, y0, x1, y1);
        List<EditorLayout.Cell> cells = EditorLayout.cells(draft == null ? 0 : draft.tokens.size(), this.drafts.weaveScroll(), x0, y0, x1);
        int size = draft == null ? 0 : draft.tokens.size();
        for (int k = 1; k < cells.size(); k++) {
            EditorLayout.Cell a = cells.get(k - 1);
            EditorLayout.Cell b = cells.get(k);
            if (a.y() != b.y()) {
                continue;
            }
            boolean toAdd = b.index() == size;
            GuiComponent.fill(pose, a.x() + 17, a.y() + 7, b.x() - 1, a.y() + 9, toAdd ? THREAD_DIM : SpellcraftPalette.ARCANE_LINE);
            GuiComponent.fill(pose, a.x() + 17, a.y() + 7, b.x() - 1, a.y() + 8, toAdd ? THREAD_DIM : SpellcraftPalette.ARCANE_GLOW);
        }
        int payload = draft == null ? -1 : payloadStart(draft);
        if (payload >= 0) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int rowY = Integer.MIN_VALUE;
            for (EditorLayout.Cell cell : cells) {
                if (cell.index() < payload) {
                    continue;
                }
                if (cell.y() != rowY) {
                    if (rowY != Integer.MIN_VALUE) {
                        SpellcraftDraw.ring(pose, minX - 3, rowY - 3, maxX + 20, rowY + 20, PAYLOAD);
                    }
                    rowY = cell.y();
                    minX = cell.x();
                }
                maxX = cell.x();
            }
            if (rowY != Integer.MIN_VALUE) {
                SpellcraftDraw.ring(pose, minX - 3, rowY - 3, maxX + 20, rowY + 20, PAYLOAD);
            }
            EditorLayout.Cell first = cells.stream().filter(cell -> cell.index() == payload).findFirst().orElse(null);
            if (first != null && first.y() == y0 + EditorLayout.WEAVE_TOP) {
                SpellcraftDraw.smallText(pose, font, SpellcraftNames.tr("screen.payload"), first.x() - 1, first.y() - 10, PAYLOAD, SpellBarStrip.KEY_SCALE);
            }
        }
        for (EditorLayout.Cell cell : cells) {
            int x = cell.x();
            int y = cell.y();
            if (cell.index() == size) {
                SpellcraftDraw.slot(pose, x, y, true);
                GuiComponent.fill(pose, x + 5, y + 7, x + 11, y + 9, PLUS);
                GuiComponent.fill(pose, x + 7, y + 5, x + 9, y + 11, PLUS);
            } else {
                String token = draft.tokens.get(cell.index());
                GlyphType type = GlyphType.byId(token);
                SpellcraftDraw.slot(pose, x, y, false);
                if (type != null) {
                    PlacedGlyph glyph = model == null ? null : model.glyphAt(cell.index());
                    SpellcraftDraw.rune(pose, type, x, y, 16, glyph != null && !glyph.active());
                } else {
                    String spec = AbilityLookup.specOf(this.screen.tree()).apply(token);
                    SpellcraftDraw.ability(pose, spec != null ? spec : token, x, y, 16, 1.0F);
                }
            }
            boolean marked = this.drafts.insertAt() == SpellDrafts.END ? cell.index() == size : cell.index() == this.drafts.insertAt() - 1;
            if (marked) {
                RenderSystem.enableBlend();
                ScreenTextures.INSET_SLOT_SELECT_FRAME.blit(pose, x - 3, y - 3);
            }
            if (SpellcraftDraw.inside(mouseX, mouseY, x, y, 16, 16) && !this.strip.popupOpen()) {
                GuiComponent.fill(pose, x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }
        int perRow = EditorLayout.perRow(x0, x1);
        int rows = EditorLayout.weaveRows(size, perRow);
        if (rows > EditorLayout.WEAVE_ROWS) {
            String more = SpellcraftNames.tr("screen.weave_rows", this.drafts.weaveScroll() + 1, Math.min(rows, this.drafts.weaveScroll() + EditorLayout.WEAVE_ROWS), rows);
            float w = font.width(more) * SpellBarStrip.KEY_SCALE;
            SpellcraftDraw.smallText(pose, font, more, x1 - 3 - w, y1 - 8, SpellcraftPalette.MUTED, SpellBarStrip.KEY_SCALE);
        }
    }

    @Nullable
    List<Component> tooltipAt(SpellcraftConfig config, @Nullable SpellDrafts.Draft draft, @Nullable SpellcraftRules.ItemCheck check, int mouseX, int mouseY) {
        int wy0 = EditorLayout.weaveTop();
        int rx0 = EditorLayout.innerX0();
        int rx1 = EditorLayout.innerX1(this.screen.width);
        for (EditorLayout.Cell cell : EditorLayout.cells(draft == null ? 0 : draft.tokens.size(), this.drafts.weaveScroll(), rx0, wy0, rx1)) {
            if (!SpellcraftDraw.inside(mouseX, mouseY, cell.x(), cell.y(), 16, 16)) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            if (draft == null || cell.index() >= draft.tokens.size()) {
                lines.add(new TextComponent(SpellcraftNames.tr("screen.add_hint")).withStyle(ChatFormatting.GRAY));
            } else {
                String token = draft.tokens.get(cell.index());
                GlyphType type = GlyphType.byId(token);
                if (type != null) {
                    SpellModel model = check == null ? null : check.models().get(draft.id > 0 ? draft.id : StoredSpell.UNSAVED_ID);
                    PlacedGlyph glyph = model == null ? null : model.glyphAt(cell.index());
                    GlyphViewLine line = this.drafts.view(config).get(type);
                    GlyphRelevance.Filter relevance = model == null ? GlyphRelevance.ALL : GlyphRelevance.forToken(config, model, cell.index());
                    EditorTooltip.glyphLines(config, type, line == null ? 0.0F : line.potency(), line == null ? 0.0F : line.quality(), relevance, lines);
                    if (glyph != null && !glyph.active()) {
                        lines.add(SpellcraftNames.colored(GlyphText.inactiveReason(config, glyph), SpellcraftPalette.BAD));
                    }
                } else {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.abilityName(this.screen.tree(), token), SpellcraftPalette.NAME_GOLD));
                    SpellModel model = check == null ? null : check.models().get(draft.id > 0 ? draft.id : StoredSpell.UNSAVED_ID);
                    SpellModel.Cast cast = model == null ? null : castOf(model.steps(), token);
                    String summary = cast == null ? "" : GlyphText.poolSummary(model.stepPool(cast), GlyphRelevance.of(config, model, cast));
                    if (!summary.isEmpty()) {
                        lines.add(SpellcraftNames.colored(SpellcraftNames.tr("bar.stat.glyphs"), SpellcraftPalette.GREY).copy().append(GlyphText.tagged(summary)));
                    }
                }
                int payload = payloadStart(draft);
                if (payload >= 0 && cell.index() >= payload && (type == null || !config.glyph(type).spellWide())) {
                    lines.add(SpellcraftNames.colored(SpellcraftNames.tr("screen.in_payload"), PAYLOAD));
                }
                lines.add(new TextComponent(SpellcraftNames.tr("screen.cell_hint")).withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }
        return null;
    }

    @Nullable
    private static SpellModel.Cast castOf(List<SpellModel.Step> steps, String group) {
        for (SpellModel.Step step : steps) {
            if (step instanceof SpellModel.Cast cast && cast.group().equals(group)) {
                return cast;
            }
            if (step instanceof SpellModel.Bundle bundle) {
                SpellModel.Cast inner = castOf(bundle.payload(), group);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        SpellDrafts.Draft draft = this.drafts.draft();
        int wy0 = EditorLayout.weaveTop();
        int rx0 = EditorLayout.innerX0();
        int rx1 = EditorLayout.innerX1(this.screen.width);
        for (EditorLayout.Cell cell : EditorLayout.cells(draft == null ? 0 : draft.tokens.size(), this.drafts.weaveScroll(), rx0, wy0, rx1)) {
            if (!SpellcraftDraw.inside(mouseX, mouseY, cell.x(), cell.y(), 16, 16)) {
                continue;
            }
            int size = draft == null ? 0 : draft.tokens.size();
            if (button == 0) {
                this.drafts.setInsertAt(cell.index() >= size ? SpellDrafts.END : cell.index() + 1);
                SpellcraftDraw.click();
            } else if (button == 1 && draft != null && cell.index() < size) {
                this.drafts.removeToken(draft, cell.index());
                SpellcraftDraw.click();
            }
            return true;
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, int step) {
        int rx0 = EditorLayout.innerX0();
        int rx1 = EditorLayout.innerX1(this.screen.width);
        if (EditorLayout.overWeave(mouseX, mouseY, this.screen.width)) {
            SpellDrafts.Draft draft = this.drafts.draft();
            int perRow = EditorLayout.perRow(rx0, rx1);
            int rows = EditorLayout.weaveRows(draft == null ? 0 : draft.tokens.size(), perRow);
            this.drafts.setWeaveScroll(Math.max(0, Math.min(Math.max(0, rows - EditorLayout.WEAVE_ROWS), this.drafts.weaveScroll() + step)));
            return true;
        }
        return false;
    }
}
