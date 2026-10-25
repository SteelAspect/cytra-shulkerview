package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

final class ShulkerItems {
    private static final java.util.Map<ItemContainerContents, ShulkerSummary> CACHE = new java.util.WeakHashMap<>();

    private ShulkerItems() {}

    /** Summary of a shulker box item's contents, or null if the stack isn't a shulker box. */
    static @Nullable ShulkerSummary summarize(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        // Contents are immutable and compare by value, so equal boxes share one summary across frames.
        return CACHE.computeIfAbsent(contents, c -> ShulkerSummary.of(c.nonEmptyItems()));
    }

    /** Configured colours: filling (light blue by default) or full (green by default). Always opaque. */
    static int barColor(ShulkerSummary summary) {
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        return 0xFF000000 | (summary.isFull() ? config.fullBarColor : config.barColor);
    }
}
