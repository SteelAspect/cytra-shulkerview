package dev.steelaspect.shulkerview;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;

/**
 * What a shulker holds, boiled down to what gets drawn: the single item type inside (or EMPTY when mixed or
 * empty) and how full it is (0..1, each slot counts as full at that item's max stack size).
 */
public record ShulkerSummary(ItemStack icon, float fill, boolean empty) {
    public static final ShulkerSummary EMPTY = new ShulkerSummary(ItemStack.EMPTY, 0f, true);
    public static final String TAG_KEY = "cytra_shulkerview";
    public static final int SLOTS = 27;

    /** Client: set once a synced summary arrives, i.e. the server has this mod. Reset on disconnect. */
    public static volatile boolean serverSyncSeen;

    /** Client game tests only: the integrated server acts like a vanilla one (sends no summary, name or update packets). */
    public static volatile boolean testVanillaServer;

    public static ShulkerSummary of(Iterable<ItemStack> stacks) {
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
        return new ShulkerSummary(mixed ? ItemStack.EMPTY : first.copyWithCount(1), Math.min(1f, fill / SLOTS), false);
    }

    public boolean hasIcon() {
        return !icon.isEmpty();
    }

    public boolean isFull() {
        return fill >= 0.9999f;
    }

    /** Same picture on screen: same icon item and components, same fill. */
    public boolean looksSame(ShulkerSummary other) {
        return other != null && empty == other.empty && fill == other.fill
                && ItemStack.isSameItemSameComponents(icon, other.icon);
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fill", fill);
        if (hasIcon()) {
            tag.store("icon", ItemStack.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), icon);
        }
        return tag;
    }

    public static ShulkerSummary fromInput(ValueInput input) {
        float fill = input.getFloatOr("fill", 0f);
        ItemStack icon = input.read("icon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (fill <= 0f && icon.isEmpty()) return EMPTY;
        return new ShulkerSummary(icon, fill, false);
    }
}
