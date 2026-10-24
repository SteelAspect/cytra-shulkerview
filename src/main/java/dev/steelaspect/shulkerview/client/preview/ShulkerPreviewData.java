package dev.steelaspect.shulkerview.client.preview;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What the tooltip preview shows: the stacks in slot order (EMPTY for free slots), or merged totals in compact mode,
 * the grid width and the window colour. Built once per contents and view, reused every frame with its client component.
 */
public final class ShulkerPreviewData implements TooltipComponent {
    /** Full: one entry per slot. Compact: one per item type, most first. */
    final List<ItemStack> stacks;
    /** Compact mode only: the merged count of each entry. */
    final int @Nullable [] totals;
    final int columns;
    /** ARGB colour the window takes (a shulker's dye colour), or null for plain Nocturne. */
    final @Nullable Integer tint;
    @Nullable ClientShulkerPreview client;

    ShulkerPreviewData(List<ItemStack> stacks, int @Nullable [] totals, int columns, @Nullable Integer tint) {
        this.stacks = stacks;
        this.totals = totals;
        this.columns = columns;
        this.tint = tint;
    }

    public boolean compact() {
        return totals != null;
    }

    public int size() {
        return stacks.size();
    }

    public int columns() {
        return columns;
    }

    public @Nullable Integer tint() {
        return tint;
    }

    ClientShulkerPreview client() {
        if (client == null) client = new ClientShulkerPreview(this);
        return client;
    }
}
