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
 * The slot grid drawn inside the tooltip, either like a vanilla container window (grey panel and slots, tinted with
 * the shulker's colour, like ShulkerBoxTooltip) or as an Alloy (Nocturne) panel. Each slot has its item, count,
 * durability bar and glint (the vanilla item renderer). Compact mode shows merged totals.
 */
final class ClientShulkerPreview implements ClientTooltipComponent {
    private static final int SLOT = 18, PAD = 7, MARGIN_TOP = 2;
    // Vanilla container window colours.
    private static final int V_BASE = 0xFFC6C6C6, V_LIGHT = 0xFFFFFFFF, V_DARK = 0xFF555555, V_EDGE = 0xFF000000;
    private static final int V_SLOT = 0xFF8B8B8B, V_SLOT_DARK = 0xFF373737, V_SLOT_LIGHT = 0xFFFFFFFF;

    private final ShulkerPreviewData data;
    private final int cols, rows;
    /** Count text per compact entry (null = let vanilla draw the count). Built once. */
    private final String[] counts;

    ClientShulkerPreview(ShulkerPreviewData data) {
        this.data = data;
        this.cols = Math.max(1, data.columns);
        this.rows = Math.max(1, (data.stacks.size() + cols - 1) / cols);
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
        return cols * SLOT + 2 * PAD;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics g) {
        int top = y + MARGIN_TOP;
        int w = getWidth(font), h = rows * SLOT + 2 * PAD;
        if (data.alloyStyle) {
            drawAlloy(g, x, top, w, h);
        } else {
            drawVanilla(g, x, top, w, h);
        }
        for (int i = 0; i < data.stacks.size(); i++) {
            ItemStack stack = data.stacks.get(i);
            if (stack.isEmpty()) continue;
            int sx = x + PAD + (i % cols) * SLOT + 1, sy = top + PAD + (i / cols) * SLOT + 1;
            g.renderItem(stack, sx, sy);
            g.renderItemDecorations(font, stack, sx, sy, counts[i]);
        }
    }

    private int tinted(int argb) {
        return data.tint == 0 ? argb : ARGB.multiply(argb, data.tint | 0xFF000000);
    }

    /** A vanilla container window: rounded black outline, white/dark bevel, grey body, sunken slots. */
    private void drawVanilla(GuiGraphics g, int x, int y, int w, int h) {
        int base = tinted(V_BASE), light = tinted(V_LIGHT), dark = tinted(V_DARK);
        g.fill(x + 1, y, x + w - 1, y + h, V_EDGE);
        g.fill(x, y + 1, x + w, y + h - 1, V_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, light);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, light);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, dark);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, dark);
        int slot = tinted(V_SLOT), slotDark = tinted(V_SLOT_DARK), slotLight = tinted(V_SLOT_LIGHT);
        for (int i = 0; i < rows * cols; i++) {
            int sx = x + PAD + (i % cols) * SLOT, sy = y + PAD + (i / cols) * SLOT;
            g.fill(sx, sy, sx + SLOT, sy + SLOT, slot);
            g.fill(sx, sy, sx + SLOT - 1, sy + 1, slotDark);
            g.fill(sx, sy, sx + 1, sy + SLOT - 1, slotDark);
            g.fill(sx + 1, sy + SLOT - 1, sx + SLOT, sy + SLOT, slotLight);
            g.fill(sx + SLOT - 1, sy + 1, sx + SLOT, sy + SLOT, slotLight);
        }
    }

    /** The Alloy (Nocturne) panel, tinted with the shulker's colour. */
    private void drawAlloy(GuiGraphics g, int x, int y, int w, int h) {
        Theme t = ThemeManager.get();
        int panel = data.tint == 0 ? t.color("surface") : ARGB.srgbLerp(0.28f, t.color("surface"), data.tint) | 0xFF000000;
        int slot = data.tint == 0 ? t.color("bg") : ARGB.srgbLerp(0.18f, t.color("bg"), data.tint) | 0xFF000000;
        Draw.panel(g, x, y, w, h, t.metricF("radius"), panel, t.color("border"), t.metricF("borderWidth"));
        for (int i = 0; i < rows * cols; i++) {
            int sx = x + PAD + (i % cols) * SLOT, sy = y + PAD + (i / cols) * SLOT;
            g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, slot);
        }
    }
}
