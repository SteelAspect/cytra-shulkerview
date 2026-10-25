package dev.steelaspect.shulkerview;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;

/**
 * What a shulker holds, boiled down to what gets drawn: the single item type inside (or EMPTY when mixed or
 * empty), how full it is (0..1, each slot counts as full at that item's max stack size) and the item types with the
 * most items, up to four, most first (for the mixed-shulker icon; one entry when it isn't mixed).
 */
public record ShulkerSummary(ItemStack icon, float fill, boolean empty, List<ItemStack> top) {
    public static final ShulkerSummary EMPTY = new ShulkerSummary(ItemStack.EMPTY, 0f, true, List.of());
    public static final int TOP = 4;
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
        List<ItemStack> kinds = new ArrayList<>(4);
        List<int[]> totals = new ArrayList<>(4);
        outer:
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            fill += (float) stack.getCount() / stack.getMaxStackSize();
            if (first.isEmpty()) {
                first = stack;
            } else if (!mixed && !ItemStack.isSameItem(first, stack)) {
                mixed = true;
            }
            for (int k = 0; k < kinds.size(); k++) {
                if (ItemStack.isSameItem(kinds.get(k), stack)) {
                    totals.get(k)[0] += stack.getCount();
                    continue outer;
                }
            }
            kinds.add(stack);
            totals.add(new int[] {stack.getCount()});
        }
        if (first.isEmpty()) return EMPTY;
        List<ItemStack> top;
        if (!mixed) {
            top = List.of(first.copyWithCount(1));
        } else {
            // Most items first; ties keep slot order (stable sort).
            Integer[] order = new Integer[kinds.size()];
            for (int k = 0; k < order.length; k++) order[k] = k;
            java.util.Arrays.sort(order, (a, b) -> Integer.compare(totals.get(b)[0], totals.get(a)[0]));
            List<ItemStack> list = new ArrayList<>(TOP);
            for (int k = 0; k < Math.min(TOP, order.length); k++) list.add(kinds.get(order[k]).copyWithCount(1));
            top = List.copyOf(list);
        }
        return new ShulkerSummary(mixed ? ItemStack.EMPTY : first.copyWithCount(1), Math.min(1f, fill / SLOTS), false, top);
    }

    public boolean hasIcon() {
        return !icon.isEmpty();
    }

    /**
     * The icons to draw: the single item when there's only one type; for a mixed shulker none, the main item, or up
     * to four (2x2), as chosen.
     */
    public List<ItemStack> icons(MixedIcon mode) {
        if (hasIcon()) return top.isEmpty() ? List.of(icon) : top;
        return switch (mode) {
            case NONE -> List.of();
            case MAIN -> top.isEmpty() ? List.of() : top.subList(0, 1);
            case TOP4 -> top;
        };
    }

    /** What mixed shulkers show as their icon. */
    public enum MixedIcon { NONE, MAIN, TOP4 }

    public boolean isFull() {
        return fill >= 0.9999f;
    }

    /** Same picture on screen: same icon item and components, same fill. */
    public boolean looksSame(ShulkerSummary other) {
        if (other == null || empty != other.empty || fill != other.fill || top.size() != other.top.size()) return false;
        if (!ItemStack.isSameItemSameComponents(icon, other.icon)) return false;
        for (int i = 0; i < top.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(top.get(i), other.top.get(i))) return false;
        }
        return true;
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fill", fill);
        if (hasIcon()) {
            tag.store("icon", ItemStack.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), icon);
        } else if (!top.isEmpty()) {
            // Mixed: the top item types, for clients that show a mixed-shulker icon. Older clients ignore the key.
            tag.store("top", ItemStack.CODEC.listOf(), registries.createSerializationContext(NbtOps.INSTANCE), top);
        }
        return tag;
    }

    public static ShulkerSummary fromInput(ValueInput input) {
        float fill = input.getFloatOr("fill", 0f);
        ItemStack icon = input.read("icon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (fill <= 0f && icon.isEmpty()) return EMPTY;
        List<ItemStack> top = !icon.isEmpty() ? List.of(icon) : input.read("top", ItemStack.CODEC.listOf()).map(List::copyOf).orElse(List.of());
        return new ShulkerSummary(icon, fill, false, top);
    }
}
