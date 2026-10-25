package dev.steelaspect.shulkerview.client.preview;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What the tooltip preview shows: the stacks in slot order (EMPTY for free slots), or merged totals in compact mode,
 * how many columns, and the tint. Built once per contents and reused every frame, with its client component.
 */
public final class ShulkerPreviewData implements TooltipComponent {
    /** Full: one entry per slot. Compact: one per item type, most first. */
    final List<ItemStack> stacks;
    /** Compact mode only: the merged count of each entry. */
    final int @Nullable [] totals;
    final int columns;
    /** ARGB tint (the shulker's colour), or 0 for an untinted container. */
    final int tint;
    final boolean alloyStyle;
    @Nullable ClientShulkerPreview client;

    ShulkerPreviewData(List<ItemStack> stacks, int @Nullable [] totals, int columns, int tint, boolean alloyStyle) {
        this.stacks = stacks;
        this.totals = totals;
        this.columns = columns;
        this.tint = tint;
        this.alloyStyle = alloyStyle;
    }

    ClientShulkerPreview client() {
        if (client == null) client = new ClientShulkerPreview(this);
        return client;
    }
}
