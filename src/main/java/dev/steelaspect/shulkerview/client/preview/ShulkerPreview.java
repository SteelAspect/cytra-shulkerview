package dev.steelaspect.shulkerview.client.preview;

import com.mojang.blaze3d.platform.InputConstants;
import dev.steelaspect.shulkerview.client.ContainerContents;
import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Tooltip preview of a container item's contents (shulkers, chests and other containers, bundles, the ender chest),
 * replacing vanilla's "Item x 3" text list. Two views: compact (each item type once, with its total, most first) and
 * full (every slot where it is). Which one shows depends on the key style:
 * <ul>
 * <li>Alloy (default): the first view always shows on hover; holding the preview key (Shift) shows the other one.</li>
 * <li>ShulkerBoxTooltip style: a hint line on hover; preview key = first view, preview key + full key (Alt) = the other.</li>
 * </ul>
 * The first view is compact unless "full by default" is on. Off when ShulkerBoxTooltip is installed (it draws its own).
 * The preview for the last hovered contents is cached, so hovering the same stack frame after frame allocates nothing.
 */
public final class ShulkerPreview {
    public enum Mode { NONE, COMPACT, FULL }

    /** Purple of the plain (undyed) shulker box. */
    private static final int PLAIN_TINT = 0xFF8E6A93;
    /** Dark teal of the ender chest. */
    private static final int ENDER_TINT = 0xFF1C5A54;
    private static final boolean OTHER_PREVIEW_MOD = FabricLoader.getInstance().isModLoaded("shulkerboxtooltip");

    /** Smoke tests only: forced key state (bit 1 = preview key, bit 2 = full key), or -1 for the real keyboard. */
    public static volatile int testKeys = -1;

    private static ContainerContents.@Nullable Contents lastContents;
    private static @Nullable Mode lastMode;
    private static boolean lastColored;
    private static @Nullable Optional<TooltipComponent> lastResult;

    private ShulkerPreview() {}

    public static void register() {
        TooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerPreviewData d ? d.client() : null);
        ItemTooltipCallback.EVENT.register((stack, flag, lines) -> extraLines(stack, lines));
    }

    /** True when another mod already previews shulkers, so this one stays out of the way. */
    public static boolean otherModPreviews() {
        return OTHER_PREVIEW_MOD;
    }

    private static boolean enabled() {
        return ShulkerViewConfig.INSTANCE.tooltipPreview && !OTHER_PREVIEW_MOD;
    }

    /** Which view the keys held right now ask for (NONE: no preview, vanilla tooltip). */
    public static Mode mode() {
        if (!enabled()) return Mode.NONE;
        ShulkerViewConfig c = ShulkerViewConfig.INSTANCE;
        boolean preview = held(c.previewKey, 1), full = held(c.fullPreviewKey, 2);
        boolean other;
        if (c.tooltipSbtKeys) {
            if (!preview) return Mode.NONE;
            other = full;
        } else {
            other = preview;
        }
        return c.tooltipFullByDefault != other ? Mode.FULL : Mode.COMPACT;
    }

    /** The key with this name, or UNKNOWN. */
    public static InputConstants.Key key(String name) {
        try {
            return InputConstants.getKey(name);
        } catch (IllegalArgumentException e) {
            return InputConstants.UNKNOWN;
        }
    }

    /** Whether this key is held (read from the keyboard, so it also works inside screens). */
    private static boolean held(String keyName, int testBit) {
        int test = testKeys;
        if (test >= 0) return (test & testBit) != 0;
        InputConstants.Key bound = key(keyName);
        if (bound.equals(InputConstants.UNKNOWN)) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (bound.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
        }
        int code = bound.getValue();
        if (InputConstants.isKeyDown(window, code)) return true;
        // Either Shift / Alt / Ctrl counts when the left one is bound.
        int other = switch (code) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case GLFW.GLFW_KEY_LEFT_ALT -> GLFW.GLFW_KEY_RIGHT_ALT;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            default -> -1;
        };
        return other >= 0 && InputConstants.isKeyDown(window, other);
    }

    private static ContainerContents.@Nullable Contents previewable(ItemStack stack) {
        if (!enabled()) return null;
        ContainerContents.Contents contents = ContainerContents.of(stack);
        return contents != null && contents.hasItems() ? contents : null;
    }

    /** Whether vanilla's shulker text list should be left out for this stack (the grid replaces it). */
    public static boolean replacesList(ItemStack stack) {
        return mode() != Mode.NONE && previewable(stack) != null;
    }

    /** The grid for this stack, or null to leave the tooltip alone. */
    public static @Nullable Optional<TooltipComponent> imageFor(ItemStack stack) {
        Mode mode = mode();
        if (mode == Mode.NONE) return null;
        ContainerContents.Contents contents = previewable(stack);
        if (contents == null) return null;
        boolean colored = ShulkerViewConfig.INSTANCE.tooltipColored;
        if (contents == lastContents && mode == lastMode && colored == lastColored && lastResult != null) return lastResult;
        lastContents = contents;
        lastMode = mode;
        lastColored = colored;
        Integer tint = colored ? tint(contents) : null;
        lastResult = Optional.of(mode == Mode.COMPACT ? compact(contents.slots(), tint) : full(contents, tint));
        return lastResult;
    }

    /** The colour the window takes: the shulker's dye colour, purple for a plain one, teal for the ender chest. */
    private static @Nullable Integer tint(ContainerContents.Contents contents) {
        return switch (contents.kind()) {
            case SHULKER -> contents.color() == null ? PLAIN_TINT : rgb(contents.color());
            case ENDER_CHEST -> ENDER_TINT;
            default -> null;
        };
    }

    private static int rgb(DyeColor color) {
        float[] c = color.getTextureDiffuseColors();
        return 0xFF000000 | Math.round(c[0] * 255) << 16 | Math.round(c[1] * 255) << 8 | Math.round(c[2] * 255);
    }

    /** Hint lines (ShulkerBoxTooltip key style) and the loot table line, under the vanilla tooltip. */
    private static void extraLines(ItemStack stack, List<Component> lines) {
        if (!enabled()) return;
        ShulkerViewConfig c = ShulkerViewConfig.INSTANCE;
        ContainerContents.Contents contents = ContainerContents.of(stack);
        if (contents == null) return;
        if (c.tooltipLootTable && contents.lootTable() != null) {
            lines.add(Component.translatable("cytra-shulkerview.tooltip.lootTable", contents.lootTable()).withStyle(ChatFormatting.GRAY));
        }
        if (!c.tooltipSbtKeys || !contents.hasItems()) return;
        Component previewKey = key(c.previewKey).getDisplayName();
        Mode mode = mode();
        if (mode == Mode.NONE) {
            lines.add(Component.translatable("cytra-shulkerview.tooltip.hint.preview", previewKey).withStyle(ChatFormatting.DARK_GRAY));
        } else if (!held(c.fullPreviewKey, 2)) {
            String other = c.tooltipFullByDefault ? "cytra-shulkerview.tooltip.hint.compact" : "cytra-shulkerview.tooltip.hint.full";
            lines.add(Component.translatable(other, key(c.fullPreviewKey).getDisplayName(), previewKey)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static ShulkerPreviewData full(ContainerContents.Contents contents, @Nullable Integer tint) {
        return new ShulkerPreviewData(contents.slots(), null, contents.columns(), tint);
    }

    private static ShulkerPreviewData compact(List<ItemStack> stacks, @Nullable Integer tint) {
        List<ItemStack> kinds = new ArrayList<>();
        List<int[]> totals = new ArrayList<>();
        outer:
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            for (int k = 0; k < kinds.size(); k++) {
                if (ItemStack.isSameItemSameTags(kinds.get(k), s)) {
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
        return new ShulkerPreviewData(Collections.unmodifiableList(sorted), counts, Math.max(1, Math.min(9, sorted.size())), tint);
    }
}
