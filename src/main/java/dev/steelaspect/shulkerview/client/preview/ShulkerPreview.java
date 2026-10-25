package dev.steelaspect.shulkerview.client.preview;

import dev.steelaspect.shulkerview.client.EnderChestCache;
import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

/**
 * Tooltip preview of a container item's contents, working like ShulkerBoxTooltip: the tooltip says "Shift: preview",
 * Shift shows the compact preview (same items merged, most first) and Alt+Shift the full one (every stack in its
 * slot). Shulkers are tinted with their colour; other containers with contents (chests, barrels, hoppers, furnaces,
 * ...) work too. Off when ShulkerBoxTooltip is installed. The preview for the last hovered contents is cached, so
 * hovering the same stack frame after frame allocates nothing.
 */
public final class ShulkerPreview {
    /** Dark teal of the ender chest. */
    private static final int ENDER_TINT = 0xFF3E8C88;
    /** Purple of the plain (undyed) shulker box. */
    private static final int PLAIN_TINT = 0xFF8E6A93;
    private static final boolean OTHER_PREVIEW_MOD = FabricLoader.getInstance().isModLoaded("shulkerboxtooltip");

    private static @Nullable ItemContainerContents lastContents;
    private static @Nullable Block lastBlock;
    private static boolean lastCompact, lastAlloy;
    private static @Nullable Optional<TooltipComponent> lastResult;

    private ShulkerPreview() {}

    public static void register() {
        TooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerPreviewData d ? d.client() : null);
    }

    /** True when another mod already previews shulkers, so this one stays out of the way. */
    public static boolean otherModPreviews() {
        return OTHER_PREVIEW_MOD;
    }

    private static @Nullable Block block(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        Block block = item.getBlock();
        if (block instanceof ShulkerBoxBlock) return block;
        if (block instanceof EnderChestBlock) return ShulkerViewConfig.INSTANCE.tooltipEnderChest ? block : null;
        return ShulkerViewConfig.INSTANCE.tooltipOtherContainers && slotCount(block) > 0 ? block : null;
    }

    /** Slots of the container this block item places (0 = not a container we preview). */
    private static int slotCount(Block block) {
        if (block instanceof ShulkerBoxBlock || block instanceof ChestBlock || block instanceof BarrelBlock
                || block instanceof EnderChestBlock) return 27;
        if (block instanceof DispenserBlock || block instanceof CrafterBlock) return 9; // dropper too
        if (block instanceof HopperBlock || block instanceof BrewingStandBlock) return 5;
        if (block instanceof AbstractFurnaceBlock) return 3;
        if (block instanceof ChiseledBookShelfBlock) return 6;
        return 0;
    }

    private static int tint(Block block) {
        if (!ShulkerViewConfig.INSTANCE.tooltipTint) return 0;
        if (block instanceof EnderChestBlock) return ENDER_TINT;
        if (!(block instanceof ShulkerBoxBlock box)) return 0;
        DyeColor color = box.getColor();
        return color == null ? PLAIN_TINT : color.getTextureDiffuseColor() | 0xFF000000;
    }

    private static @Nullable ItemContainerContents contents(ItemStack stack) {
        boolean ender = stack.getItem() instanceof BlockItem item && item.getBlock() instanceof EnderChestBlock;
        ItemContainerContents contents = ender ? EnderChestCache.contents() : stack.get(DataComponents.CONTAINER);
        return contents != null && contents.nonEmptyItems().iterator().hasNext() ? contents : null;
    }

    /** Preview on, no other preview mod, and this is a container item with something inside. */
    private static boolean previewable(ItemStack stack) {
        return ShulkerViewConfig.INSTANCE.tooltipPreview && !OTHER_PREVIEW_MOD && block(stack) != null && contents(stack) != null;
    }

    private static boolean shift() {
        return Minecraft.getInstance().hasShiftDown();
    }

    private static boolean alt() {
        return Minecraft.getInstance().hasAltDown();
    }

    /** Shown right now: always, or only with Shift held (the default). */
    private static boolean showingNow() {
        return !ShulkerViewConfig.INSTANCE.tooltipShiftOnly || shift();
    }

    /** Compact unless Alt is held (or the other way round when the player swapped them). */
    private static boolean compactNow() {
        return ShulkerViewConfig.INSTANCE.tooltipCompact != alt();
    }

    /** Whether vanilla's text list is left out for this stack (the preview and its hint replace it). */
    public static boolean replacesList(ItemStack stack) {
        return previewable(stack);
    }

    /** The grey hint line under the name: how to open the preview, or how to switch to the other view. */
    public static @Nullable Component hint(ItemStack stack) {
        if (!previewable(stack)) return null;
        ShulkerViewConfig c = ShulkerViewConfig.INSTANCE;
        String other = c.tooltipCompact ? "full" : "compact";
        if (!showingNow()) return Component.translatable("cytra-shulkerview.tooltip.shift", Component.translatable("cytra-shulkerview.tooltip." + other));
        if (compactNow() == c.tooltipCompact) {
            return Component.translatable(c.tooltipShiftOnly ? "cytra-shulkerview.tooltip.alt_shift" : "cytra-shulkerview.tooltip.alt",
                    Component.translatable("cytra-shulkerview.tooltip." + other));
        }
        return null;
    }

    /** The preview for this stack, or null to leave the tooltip alone. */
    public static @Nullable Optional<TooltipComponent> imageFor(ItemStack stack) {
        if (!previewable(stack) || !showingNow()) return null;
        Block block = block(stack);
        ItemContainerContents contents = contents(stack);
        boolean compact = compactNow();
        boolean alloy = ShulkerViewConfig.INSTANCE.tooltipStyle == ShulkerViewConfig.TooltipStyle.ALLOY;
        if (contents == lastContents && block == lastBlock && compact == lastCompact && alloy == lastAlloy && lastResult != null) {
            return lastResult;
        }
        lastContents = contents;
        lastBlock = block;
        lastCompact = compact;
        lastAlloy = alloy;
        int tint = tint(block);
        lastResult = Optional.of(compact ? compact(contents, tint, alloy) : full(contents, slotCount(block), tint, alloy));
        return lastResult;
    }

    private static ShulkerPreviewData full(ItemContainerContents contents, int size, int tint, boolean alloy) {
        NonNullList<ItemStack> slots = NonNullList.withSize(size, ItemStack.EMPTY);
        contents.copyInto(slots);
        return new ShulkerPreviewData(Collections.unmodifiableList(slots), null, Math.min(9, size), tint, alloy);
    }

    private static ShulkerPreviewData compact(ItemContainerContents contents, int tint, boolean alloy) {
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
        return new ShulkerPreviewData(Collections.unmodifiableList(sorted), counts, Math.min(9, sorted.size()), tint, alloy);
    }
}
