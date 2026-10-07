package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ShulkerItemOverlay {
    private static final float ICON_SCALE = 0.625f; // 10px icon on the 16px slot

    private ShulkerItemOverlay() {}

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        if (stack.isEmpty() || !(config.itemIcon || config.itemBar)) return;
        ShulkerSummary summary = ShulkerItems.summarize(stack);
        if (summary == null || summary.empty()) return;

        if (config.itemIcon && summary.hasIcon()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 3, y + 2);
            graphics.pose().scale(ICON_SCALE, ICON_SCALE);
            graphics.renderFakeItem(summary.icon(), 0, 0);
            graphics.pose().popMatrix();
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
