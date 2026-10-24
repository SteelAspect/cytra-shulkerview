package dev.steelaspect.shulkerview;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * What a container holds, boiled down to what gets drawn: the single item type inside (or EMPTY when mixed or
 * empty) and how full it is (0..1, each slot counts as full at that item's max stack size).
 */
public record ShulkerSummary(ItemStack icon, float fill, boolean empty) {
    public static final ShulkerSummary EMPTY = new ShulkerSummary(ItemStack.EMPTY, 0f, true);
    public static final String TAG_KEY = "cytra_shulkerview";
    public static final int SLOTS = 27;

    /** Client: set once a synced summary arrives, i.e. the server has this mod. Reset on disconnect. */
    public static volatile boolean serverSyncSeen;

    public static ShulkerSummary of(Iterable<ItemStack> stacks) {
        return of(stacks, SLOTS);
    }

    /** Summary of these stacks in a container with this many slots. */
    public static ShulkerSummary of(Iterable<ItemStack> stacks, int slots) {
        ItemStack first = ItemStack.EMPTY;
        boolean mixed = false;
        float fill = 0f;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            fill += (float) stack.getCount() / stack.getMaxStackSize();
            if (first.isEmpty()) {
                first = stack;
            } else if (!mixed && !ItemStack.isSameItem(first, stack)) {
                mixed = true;
            }
        }
        if (first.isEmpty()) return EMPTY;
        return new ShulkerSummary(mixed ? ItemStack.EMPTY : first.copyWithCount(1), Math.min(1f, fill / Math.max(1, slots)), false);
    }

    public boolean hasIcon() {
        return !icon.isEmpty();
    }

    public boolean isFull() {
        return fill >= 0.9999f;
    }

    /** Same picture on screen: same icon item and tags, same fill. */
    public boolean looksSame(ShulkerSummary other) {
        return other != null && empty == other.empty && fill == other.fill
                && ItemStack.isSameItemSameTags(icon, other.icon);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fill", fill);
        if (hasIcon()) tag.put("icon", icon.save(new CompoundTag()));
        return tag;
    }

    public static ShulkerSummary fromTag(CompoundTag tag) {
        float fill = tag.getFloat("fill");
        ItemStack icon = tag.contains("icon", CompoundTag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound("icon")) : ItemStack.EMPTY;
        if (fill <= 0f && icon.isEmpty()) return EMPTY;
        return new ShulkerSummary(icon, fill, false);
    }
}
