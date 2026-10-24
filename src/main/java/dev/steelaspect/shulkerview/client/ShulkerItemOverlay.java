package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

/** Icon + fill bar on container items (shulkers, Ctrl+picked chests and the like, bundles). */
public final class ShulkerItemOverlay {
    private ShulkerItemOverlay() {}

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        if (stack.isEmpty() || !(config.itemIcon || config.itemBar)) return;
        ContainerContents.Contents contents = ContainerContents.of(stack);
        // The ender chest's contents aren't the item's own, so its items get no overlay.
        if (contents == null || contents.kind() == ContainerContents.Kind.ENDER_CHEST) return;
        ShulkerSummary summary = contents.summary();
        if (summary.empty()) return;

        if (config.itemIcon && summary.hasIcon()) {
            graphics.pose().pushPose();
            // Centred, nudged up 1px so the bar stays readable underneath; in front of the item, behind its count.
            float scale = (float) config.itemIconSize;
            float offset = 8f * (1f - scale);
            graphics.pose().translate(x + offset, y + offset - 1f, 20f);
            graphics.pose().scale(scale, scale, 1f);
            graphics.renderFakeItem(summary.icon(), 0, 0);
            graphics.pose().popPose();
        }
        // Bundles already have vanilla's own fill bar.
        if (config.itemBar && contents.kind() != ContainerContents.Kind.BUNDLE) {
            int left = x + 2;
            int top = y + 13;
            int width = Math.max(1, Math.round(13 * summary.fill()));
            graphics.fill(RenderType.guiOverlay(), left, top, left + 13, top + 2, 0xFF000000);
            graphics.fill(RenderType.guiOverlay(), left, top, left + width, top + 1, ShulkerItems.barColor(summary));
        }
    }
}
