package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

final class ShulkerItems {
    private ShulkerItems() {}

    /** Summary of a shulker box item's contents, or null if the stack isn't a shulker box. */
    static @Nullable ShulkerSummary summarize(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return ShulkerSummary.of(contents.nonEmptyItems());
    }

    /** Light blue while filling, green when completely full. */
    static int barColor(ShulkerSummary summary) {
        return summary.isFull() ? 0xFF55FF55 : 0xFF55BBFF;
    }
}
