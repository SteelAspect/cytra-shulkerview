package dev.steelaspect.shulkerview.client.preview;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What the tooltip preview shows: the stacks in slot order (EMPTY for free slots), or merged totals in compact mode,
 * and the shulker's colour. Built once per shulker contents and reused every frame, with its client component.
 */
public final class ShulkerPreviewData implements TooltipComponent {
    /** Normal: 27 entries, one per slot. Compact: one per item type, most first. */
    final List<ItemStack> stacks;
    /** Compact mode only: the merged count of each entry. */
    final int @Nullable [] totals;
    /** ARGB tint, from the shulker's dye colour. */
    final int tint;
    @Nullable ClientShulkerPreview client;

    ShulkerPreviewData(List<ItemStack> stacks, int @Nullable [] totals, int tint) {
        this.stacks = stacks;
        this.totals = totals;
        this.tint = tint;
    }

    ClientShulkerPreview client() {
        if (client == null) client = new ClientShulkerPreview(this);
        return client;
    }
}
