package dev.steelaspect.shulkerview.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.steelaspect.shulkerview.ShulkerSummary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * What an item holds, read from its NBT (1.20.4 keeps item contents there, not in data components):
 * <ul>
 * <li>shulker boxes, chests, barrels, hoppers, furnaces... and modded blocks: {@code BlockEntityTag.Items} with a
 * {@code Slot} per stack (what Ctrl+pick block and shulker drops carry);</li>
 * <li>bundles: their own {@code Items} list, no slots, newest first;</li>
 * <li>ender chests: the player's ender chest as last seen ({@link EnderChestContents}).</li>
 * </ul>
 * Parsed contents are cached by the identity of the NBT list, so drawing the same stack frame after frame parses nothing.
 */
public final class ContainerContents {
    public enum Kind { SHULKER, CONTAINER, BUNDLE, ENDER_CHEST }

    /**
     * @param slots every slot in order (EMPTY where free); for bundles, the stacks in their list order
     * @param columns grid width for the full view
     * @param color dye colour of a shulker box, else null (plain shulker = null with kind SHULKER)
     * @param lootTable the loot table of an unopened container, else null
     */
    public record Contents(Kind kind, List<ItemStack> slots, int columns, @Nullable DyeColor color,
                           @Nullable String lootTable, ShulkerSummary summary) {
        public boolean hasItems() {
            return !summary.empty();
        }
    }

    private static final Cache<Tag, Contents> CACHE = CacheBuilder.newBuilder().weakKeys().maximumSize(1024).build();
    /** Slot count of each block item's block entity, -1 when it isn't a container (or couldn't be made). */
    private static final Map<Item, Integer> SLOT_COUNTS = Collections.synchronizedMap(new IdentityHashMap<>());

    private ContainerContents() {}

    public static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean isEnderChest(ItemStack stack) {
        return stack.getItem() instanceof BlockItem b && b.getBlock() instanceof EnderChestBlock;
    }

    /** Contents of this stack, or null when it holds no item list this mod knows (or its kind is turned off). */
    public static @Nullable Contents of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        Item item = stack.getItem();
        if (item instanceof BundleItem) {
            if (!config.bundles) return null;
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("Items", Tag.TAG_LIST)) return null;
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            return cached(list, () -> bundle(stack, list));
        }
        if (!(item instanceof BlockItem blockItem)) return null;
        Block block = blockItem.getBlock();
        if (block instanceof EnderChestBlock) {
            return config.enderChestPreview ? EnderChestContents.get() : null;
        }
        boolean shulker = block instanceof ShulkerBoxBlock;
        if (!shulker && !config.otherContainers) return null;
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data == null) return null;
        String loot = data.contains("LootTable", Tag.TAG_STRING) ? data.getString("LootTable") : null;
        if (!data.contains("Items", Tag.TAG_LIST)) {
            // An unopened loot container: nothing to show yet, but the loot table line still wants it.
            if (loot == null) return null;
            return cached(data, () -> container(blockItem, shulker, new ListTag(), loot));
        }
        ListTag list = data.getList("Items", Tag.TAG_COMPOUND);
        return cached(list, () -> container(blockItem, shulker, list, loot));
    }

    private static Contents cached(Tag key, java.util.concurrent.Callable<Contents> make) {
        try {
            return CACHE.get(key, make);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private static Contents container(BlockItem item, boolean shulker, ListTag list, @Nullable String loot) {
        int size = shulker ? ShulkerSummary.SLOTS : slotCount(item);
        int maxSlot = -1;
        for (int i = 0; i < list.size(); i++) maxSlot = Math.max(maxSlot, list.getCompound(i).getByte("Slot") & 255);
        if (size <= 0) {
            // Not a Container block entity we could make: size it from the highest slot used.
            size = maxSlot < 9 ? Math.max(1, maxSlot + 1) : (maxSlot / 9 + 1) * 9;
        }
        size = Math.max(size, maxSlot + 1);
        NonNullList<ItemStack> slots = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            slots.set(slot, ItemStack.of(entry));
        }
        DyeColor color = shulker && item.getBlock() instanceof ShulkerBoxBlock box ? box.getColor() : null;
        Kind kind = shulker ? Kind.SHULKER : Kind.CONTAINER;
        return new Contents(kind, Collections.unmodifiableList(slots), columns(size), color, loot, ShulkerSummary.of(slots, size));
    }

    private static Contents bundle(ItemStack bundle, ListTag list) {
        List<ItemStack> stacks = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.of(list.getCompound(i));
            if (!s.isEmpty()) stacks.add(s);
        }
        ShulkerSummary counted = ShulkerSummary.of(stacks, 1);
        // Bundles fill by weight (64), which vanilla already works out.
        ShulkerSummary summary = counted.empty() ? counted
                : new ShulkerSummary(counted.icon(), Math.min(1f, BundleItem.getFullnessDisplay(bundle)), false);
        return new Contents(Kind.BUNDLE, Collections.unmodifiableList(stacks), Math.max(1, Math.min(9, stacks.size())), null, null, summary);
    }

    /** Contents of the ender chest as a container (called by {@link EnderChestContents}). */
    static Contents enderChest(List<ItemStack> slots) {
        return new Contents(Kind.ENDER_CHEST, Collections.unmodifiableList(slots), 9, null, null,
                ShulkerSummary.of(slots, slots.size()));
    }

    /** Grid width: dispensers and droppers (9 slots) as 3x3, small containers in one row, the rest 9 wide. */
    static int columns(int size) {
        if (size == 9) return 3;
        return Math.max(1, Math.min(9, size));
    }

    /** Slots of this block item's container, from a throwaway block entity (vanilla and modded blocks alike). */
    static int slotCount(BlockItem item) {
        Integer known = SLOT_COUNTS.get(item);
        if (known != null) return known;
        int size = -1;
        Block block = item.getBlock();
        if (block instanceof EntityBlock entityBlock) {
            try {
                BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                if (be instanceof Container container) size = container.getContainerSize();
            } catch (Throwable ignored) {
                // A modded block entity that needs a level to be made: fall back to the slots in the NBT.
            }
        }
        SLOT_COUNTS.put(item, size);
        return size;
    }
}
