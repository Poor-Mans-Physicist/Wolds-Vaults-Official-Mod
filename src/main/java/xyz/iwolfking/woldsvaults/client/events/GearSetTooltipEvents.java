package xyz.iwolfking.woldsvaults.client.events;

import iskallia.vault.config.UniqueGearConfig;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.snapshot.AttributeSnapshotHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.api.gear.sets.GearSetHelper;
import xyz.iwolfking.woldsvaults.config.GearSetConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The set block at the bottom of a set piece's tooltip: set name and worn count, every piece (set colour when worn,
 * grey when not), then each tier's text (set colour when active, grey when not). Worn and active come from the
 * client's own attribute snapshot, so the tooltip matches what the server applies.
 */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GearSetTooltipEvents {
    private static final int WRAP_WIDTH = 200;
    private static final String INDENT = "    ";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSetPieceTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation setId = GearSetHelper.setOf(stack).orElse(null);
        if (setId == null) {
            return;
        }
        GearSetConfig.GearSet set = GearSetHelper.config().getSet(setId).orElse(null);
        if (set == null) {
            return;
        }
        Set<ResourceLocation> worn = wornPieces(setId);
        Style active = Style.EMPTY.withColor(TextColor.fromRgb(set.colorRgb()));
        Style inactive = Style.EMPTY.withColor(ChatFormatting.GRAY);

        List<Component> block = new ArrayList<>();
        block.add(TextComponent.EMPTY);
        block.add(new TextComponent(set.name + " (" + worn.size() + "/" + set.pieces.size() + ")").setStyle(active));
        for (String piece : set.pieces) {
            ResourceLocation pieceId = ResourceLocation.tryParse(piece);
            boolean isWorn = pieceId != null && worn.contains(pieceId);
            block.add(new TextComponent("  " + pieceName(pieceId, piece)).setStyle(isWorn ? active : inactive));
        }
        for (GearSetConfig.Tier tier : set.tiers) {
            Style style = worn.size() >= tier.pieces ? active : inactive;
            List<String> lines = wrap("(" + tier.pieces + ") " + tier.text);
            for (int i = 0; i < lines.size(); i++) {
                block.add(new TextComponent((i == 0 ? "" : INDENT) + lines.get(i)).setStyle(style));
            }
        }

        List<Component> tooltip = event.getToolTip();
        int at = tooltip.size();
        if (event.getFlags().isAdvanced() && stack.getItem().getRegistryName() != null) {
            String registryLine = stack.getItem().getRegistryName().toString();
            for (int i = tooltip.size() - 1; i >= 0; i--) {
                if (registryLine.equals(tooltip.get(i).getString())) {
                    at = i;
                    break;
                }
            }
        }
        tooltip.addAll(at, block);
    }

    private static Set<ResourceLocation> wornPieces(ResourceLocation setId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Collections.emptySet();
        }
        return GearSetHelper.wornPieces(AttributeSnapshotHelper.getInstance().getSnapshot(minecraft.player), minecraft.player)
                .getOrDefault(setId, Collections.emptySet());
    }

    private static String pieceName(ResourceLocation pieceId, String fallback) {
        if (pieceId == null || !ModConfigs.isInitialized() || ModConfigs.UNIQUE_GEAR == null) {
            return fallback;
        }
        return ModConfigs.UNIQUE_GEAR.getEntry(pieceId).map(UniqueGearConfig.Entry::getName).orElse(fallback);
    }

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        Minecraft.getInstance().font.getSplitter().splitLines(text, WRAP_WIDTH, Style.EMPTY)
                .forEach((FormattedText part) -> lines.add(part.getString().strip()));
        return lines;
    }
}
