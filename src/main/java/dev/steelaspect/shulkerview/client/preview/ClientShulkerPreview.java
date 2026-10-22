package dev.steelaspect.shulkerview.client.preview;

import dev.steelaspect.alloyui.render.Draw;
import dev.steelaspect.alloyui.theme.Theme;
import dev.steelaspect.alloyui.theme.ThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/**
 * The 9-wide slot grid drawn inside the tooltip: a Nocturne panel tinted with the shulker's colour, each slot with its
 * item, count, durability bar and glint (the vanilla item renderer). Compact mode shows merged totals.
 */
final class ClientShulkerPreview implements ClientTooltipComponent {
    private static final int COLS = 9, SLOT = 18, PAD = 4, MARGIN_TOP = 2;

    private final ShulkerPreviewData data;
    private final int rows;
    /** Count text per compact entry (null = let vanilla draw the count). Built once. */
    private final String[] counts;

    ClientShulkerPreview(ShulkerPreviewData data) {
        this.data = data;
        this.rows = Math.max(1, (data.stacks.size() + COLS - 1) / COLS);
        this.counts = new String[data.stacks.size()];
        if (data.totals != null) {
            for (int i = 0; i < counts.length; i++) counts[i] = data.totals[i] > 1 ? shortCount(data.totals[i]) : null;
        }
    }

    static String shortCount(int n) {
        if (n < 1000) return Integer.toString(n);
        if (n < 10_000) return String.format(java.util.Locale.ROOT, "%.1fk", n / 1000f).replace(".0k", "k");
        return (n / 1000) + "k";
    }

    @Override
    public int getHeight(Font font) {
        return MARGIN_TOP + rows * SLOT + 2 * PAD;
    }

    @Override
    public int getWidth(Font font) {
        return COLS * SLOT + 2 * PAD;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics g) {
        Theme t = ThemeManager.get();
        int top = y + MARGIN_TOP;
        int w = getWidth(font), h = rows * SLOT + 2 * PAD;
        int panel = ARGB.srgbLerp(0.28f, t.color("surface"), data.tint) | 0xFF000000;
        int slot = ARGB.srgbLerp(0.18f, t.color("bg"), data.tint) | 0xFF000000;
        Draw.panel(g, x, top, w, h, t.metricF("radius"), panel, t.color("border"), t.metricF("borderWidth"));
        for (int i = 0; i < rows * COLS; i++) {
            int sx = x + PAD + (i % COLS) * SLOT, sy = top + PAD + (i / COLS) * SLOT;
            g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, slot);
        }
        for (int i = 0; i < data.stacks.size(); i++) {
            ItemStack stack = data.stacks.get(i);
            if (stack.isEmpty()) continue;
            int sx = x + PAD + (i % COLS) * SLOT + 1, sy = top + PAD + (i / COLS) * SLOT + 1;
            g.renderItem(stack, sx, sy);
            g.renderItemDecorations(font, stack, sx, sy, counts[i]);
        }
    }
}
