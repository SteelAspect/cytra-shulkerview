package dev.steelaspect.shulkerview.client.preview;

import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

/**
 * Tooltip preview of a shulker item's 27 slots, replacing vanilla's "Item x 3" text list. Off when ShulkerBoxTooltip
 * is installed (it draws its own). The preview for the last hovered contents is cached, so hovering the same stack
 * frame after frame allocates nothing.
 */
public final class ShulkerPreview {
    /** Purple of the plain (undyed) shulker box. */
    private static final int PLAIN_TINT = 0xFF8E6A93;
    private static final boolean OTHER_PREVIEW_MOD = FabricLoader.getInstance().isModLoaded("shulkerboxtooltip");

    private static @Nullable ItemContainerContents lastContents;
    private static int lastTint;
    private static boolean lastCompact;
    private static @Nullable Optional<TooltipComponent> lastResult;

    private ShulkerPreview() {}

    public static void register() {
        TooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerPreviewData d ? d.client() : null);
    }

    /** True when another mod already previews shulkers, so this one stays out of the way. */
    public static boolean otherModPreviews() {
        return OTHER_PREVIEW_MOD;
    }

    /** The shulker's colour, or null when this isn't a shulker item. */
    private static @Nullable Integer tint(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock box)) return null;
        DyeColor color = box.getColor();
        return color == null ? PLAIN_TINT : color.getTextureDiffuseColor() | 0xFF000000;
    }

    private static boolean enabled() {
        ShulkerViewConfig c = ShulkerViewConfig.INSTANCE;
        return c.tooltipPreview && !OTHER_PREVIEW_MOD;
    }

    /** Preview on (setting), and Shift held when it's set to Shift only. */
    private static boolean showingNow() {
        return enabled() && (!ShulkerViewConfig.INSTANCE.tooltipShiftOnly || Minecraft.getInstance().hasShiftDown());
    }

    /** Shulker item with something inside. */
    private static boolean hasContents(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents.nonEmptyItems().iterator().hasNext();
    }

    /** Whether vanilla's text list should be left out for this stack (the grid replaces it). */
    public static boolean replacesList(ItemStack stack) {
        return showingNow() && tint(stack) != null && hasContents(stack);
    }

    /** Shift-only mode, Shift not held: a hint line goes under vanilla's list. */
    public static boolean wantsShiftHint(ItemStack stack) {
        return enabled() && ShulkerViewConfig.INSTANCE.tooltipShiftOnly && !Minecraft.getInstance().hasShiftDown()
                && tint(stack) != null && hasContents(stack);
    }

    /** The grid for this stack, or null to leave the tooltip alone. */
    public static @Nullable Optional<TooltipComponent> imageFor(ItemStack stack) {
        if (!showingNow()) return null;
        Integer tint = tint(stack);
        if (tint == null) return null;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || !contents.nonEmptyItems().iterator().hasNext()) return null;
        boolean compact = ShulkerViewConfig.INSTANCE.tooltipCompact;
        if (contents == lastContents && tint == lastTint && compact == lastCompact && lastResult != null) return lastResult;
        lastContents = contents;
        lastTint = tint;
        lastCompact = compact;
        lastResult = Optional.of(compact ? compact(contents, tint) : slots(contents, tint));
        return lastResult;
    }

    private static ShulkerPreviewData slots(ItemContainerContents contents, int tint) {
        net.minecraft.core.NonNullList<ItemStack> slots = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(slots);
        return new ShulkerPreviewData(Collections.unmodifiableList(slots), null, tint);
    }

    private static ShulkerPreviewData compact(ItemContainerContents contents, int tint) {
        List<ItemStack> kinds = new ArrayList<>();
        List<int[]> totals = new ArrayList<>();
        outer:
        for (ItemStack s : contents.nonEmptyItems()) {
            for (int k = 0; k < kinds.size(); k++) {
                if (ItemStack.isSameItemSameComponents(kinds.get(k), s)) {
                    totals.get(k)[0] += s.getCount();
                    continue outer;
                }
            }
            kinds.add(s.copyWithCount(1));
            totals.add(new int[] {s.getCount()});
        }
        Integer[] order = new Integer[kinds.size()];
        for (int k = 0; k < order.length; k++) order[k] = k;
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(totals.get(b)[0], totals.get(a)[0]));
        List<ItemStack> sorted = new ArrayList<>(order.length);
        int[] counts = new int[order.length];
        for (int k = 0; k < order.length; k++) {
            sorted.add(kinds.get(order[k]));
            counts[k] = totals.get(order[k])[0];
        }
        return new ShulkerPreviewData(Collections.unmodifiableList(sorted), counts, tint);
    }
}
