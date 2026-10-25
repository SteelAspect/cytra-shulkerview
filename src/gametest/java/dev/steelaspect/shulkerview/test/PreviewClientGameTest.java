package dev.steelaspect.shulkerview.test;

import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tooltip preview, ShulkerBoxTooltip style: hint without Shift, compact with Shift, full with Alt+Shift, Alloy look,
 * a chest item, always-on mode, and a shulker inside a chest. Screenshots with the mouse over the item.
 */
public class PreviewClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");
    private static final int SCALE = 2;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (Only.skip("preview")) return;
        if (Boolean.getBoolean("shulkerview.namedOnly")) return;
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(c -> {
            c.options.guiScale().set(SCALE);
            c.resizeDisplay();
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:blue_shulker_box[custom_name=\"Loot\",container=["
                    + "{slot:0,item:{id:\"minecraft:gold_nugget\",count:3}},"
                    + "{slot:1,item:{id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:damage\":900,\"minecraft:enchantments\":{\"minecraft:sharpness\":5}}}},"
                    + "{slot:4,item:{id:\"minecraft:blue_concrete\",count:64}},"
                    + "{slot:13,item:{id:\"minecraft:rotten_flesh\",count:1}},"
                    + "{slot:22,item:{id:\"minecraft:blue_concrete\",count:2}},"
                    + "{slot:26,item:{id:\"minecraft:gold_nugget\",count:40}}]]");
            context.waitTicks(5);

            world.getServer().runCommand("give @p minecraft:chest[container=[{slot:0,item:{id:\"minecraft:iron_ingot\",count:32}},"
                    + "{slot:5,item:{id:\"minecraft:bread\",count:12}},{slot:20,item:{id:\"minecraft:iron_ingot\",count:64}}]]");
            context.waitTicks(5);

            // Default (like ShulkerBoxTooltip): name + "Shift: preview" hint, no vanilla list, no window.
            context.runOnClient(c -> {
                ItemStack stack = c.player.getInventory().getItem(0);
                String all = lines(c, stack);
                check(all.contains("Loot") && all.contains("Shift: preview") && !all.contains("Gold Nugget"), "hint instead of the list: " + all);
                check(stack.getTooltipImage().isEmpty(), "no window without Shift");
            });
            context.runOnClient(c -> c.setScreen(new InventoryScreen(c.player)));
            context.waitTicks(5);
            hoverSlotWith(context, 0);
            shot(context, "40-hint");

            // Shift: compact.
            context.getInput().holdKey(GLFW.GLFW_KEY_LEFT_SHIFT);
            context.waitTicks(2);
            context.runOnClient(c -> {
                ItemStack stack = c.player.getInventory().getItem(0);
                Optional<TooltipComponent> image = stack.getTooltipImage();
                check(image.isPresent() && image.get().getClass().getSimpleName().equals("ShulkerPreviewData"), "Shift: window");
                check(stack.getTooltipImage().get() == image.get(), "same window reused next frame (cached)");
                check(lines(c, stack).contains("Alt+Shift: every slot"), "Shift: hint for the full view");
            });
            shot(context, "41-shift-compact");

            // Alt+Shift: full, every stack in its slot.
            context.getInput().holdKey(GLFW.GLFW_KEY_LEFT_ALT);
            context.waitTicks(2);
            shot(context, "42-alt-shift-full");
            context.getInput().releaseKey(GLFW.GLFW_KEY_LEFT_ALT);

            ShulkerViewConfig.INSTANCE.tooltipStyle = ShulkerViewConfig.TooltipStyle.ALLOY;
            context.waitTicks(2);
            shot(context, "43-shift-alloy-style");
            ShulkerViewConfig.INSTANCE.tooltipStyle = ShulkerViewConfig.TooltipStyle.VANILLA;

            // Other containers: a chest item with contents.
            hoverSlotWith(context, 1);
            context.runOnClient(c -> check(c.player.getInventory().getItem(1).getTooltipImage().isPresent(), "chest item previews"));
            shot(context, "44-chest-item");
            context.getInput().releaseKey(GLFW.GLFW_KEY_LEFT_SHIFT);

            // Not Shift-only: shown straight away.
            ShulkerViewConfig.INSTANCE.tooltipShiftOnly = false;
            context.waitTicks(2);
            context.runOnClient(c -> check(c.player.getInventory().getItem(0).getTooltipImage().isPresent(), "always on: window without Shift"));
            ShulkerViewConfig.INSTANCE.tooltipShiftOnly = true;
            context.runOnClient(c -> c.setScreen(null));

            // A shulker inside a chest.
            BlockPos feet = world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition());
            BlockPos chest = feet.offset(0, 0, 2);
            world.getServer().runCommand(String.format(Locale.ROOT,
                    "setblock %d %d %d minecraft:chest{Items:[{Slot:10b,id:\"minecraft:red_shulker_box\",count:1,components:{\"minecraft:container\":["
                            + "{slot:0,item:{id:\"minecraft:redstone\",count:64}},{slot:8,item:{id:\"minecraft:repeater\",count:12}},"
                            + "{slot:18,item:{id:\"minecraft:comparator\",count:5}}]}}]}",
                    chest.getX(), chest.getY(), chest.getZ()));
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    chest.getX() + 0.5, chest.getY(), chest.getZ() - 1.5, chest.getX() + 0.5, chest.getY() - 1.12 + 0.5, chest.getZ() + 0.5));
            context.waitTicks(5);
            context.runOnClient(c -> c.gameMode.useItemOn(c.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chest),
                            net.minecraft.core.Direction.NORTH, chest, false)));
            context.waitTicks(10);
            check(context.computeOnClient(c -> c.screen instanceof ContainerScreen), "chest opened");
            hoverSlotWith(context, 10);
            context.getInput().holdKey(GLFW.GLFW_KEY_LEFT_SHIFT);
            context.waitTicks(2);
            shot(context, "45-shulker-in-chest");
            context.getInput().releaseKey(GLFW.GLFW_KEY_LEFT_SHIFT);
            context.runOnClient(c -> c.player.closeContainer());
        }
    }

    private static String lines(Minecraft c, ItemStack stack) {
        return stack.getTooltipLines(Item.TooltipContext.of(c.level), c.player, TooltipFlag.NORMAL).stream()
                .map(Component::getString).reduce("", (x, y) -> x + "|" + y);
    }

    /** Moves the mouse over the menu slot with this container index (0 = first slot of the open menu). */
    private static void hoverSlotWith(ClientGameTestContext context, int menuSlot) {
        double[] at = context.computeOnClient(c -> {
            if (!(c.screen instanceof AbstractContainerScreen<?> screen)) return null;
            Slot slot = null;
            for (Slot s : screen.getMenu().slots) {
                if (screen instanceof InventoryScreen ? s.container == c.player.getInventory() && s.getContainerSlot() == menuSlot
                        : s.index == menuSlot) {
                    slot = s;
                    break;
                }
            }
            if (slot == null) return null;
            int imageHeight = screen instanceof ContainerScreen ? 114 + 3 * 18 : 166;
            int left = (screen.width - 176) / 2, top = (screen.height - imageHeight) / 2;
            return new double[] {(left + slot.x + 8) * SCALE, (top + slot.y + 8) * SCALE};
        });
        check(at != null, "slot " + menuSlot + " found");
        context.getInput().setCursorPos(at[0], at[1]);
        context.waitTicks(3);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Failed: " + what);
        LOG.info("OK: {}", what);
    }

    private static void shot(ClientGameTestContext context, String name) {
        LOG.info("Screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of(name)));
    }
}
