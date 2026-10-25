package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ShulkerItemOverlay {
    private ShulkerItemOverlay() {}

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        if (stack.isEmpty() || !(config.itemIcon || config.itemBar)) return;
        ShulkerSummary summary = ShulkerItems.summarize(stack);
        if (summary == null || summary.empty()) return;

        List<ItemStack> icons = config.itemIcon ? summary.icons(config.mixedIcon) : List.of();
        if (icons.size() == 1) {
            graphics.pose().pushMatrix();
            // Centred, nudged up 1px so the bar stays readable underneath.
            float scale = (float) config.itemIconSize;
            float offset = 8f * (1f - scale);
            graphics.pose().translate(x + offset, y + offset - 1f);
            graphics.pose().scale(scale, scale);
            graphics.renderFakeItem(icons.get(0), 0, 0);
            graphics.pose().popMatrix();
        } else if (icons.size() > 1) {
            // 2x2 in the slot above the bar, in reading order (most items top-left).
            for (int i = 0; i < icons.size(); i++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + 1f + (i % 2) * 7f, y + (i / 2) * 6.5f);
                graphics.pose().scale(0.44f, 0.44f);
                graphics.renderFakeItem(icons.get(i), 0, 0);
                graphics.pose().popMatrix();
            }
        }
        if (config.itemBar) {
            int left = x + 2;
            int top = y + 13;
            int width = Math.max(1, Math.round(13 * summary.fill()));
            graphics.fill(left, top, left + 13, top + 2, 0xFF000000);
            graphics.fill(left, top, left + width, top + 1, ShulkerItems.barColor(summary));
        }
    }
}
