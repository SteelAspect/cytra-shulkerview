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
 * Tooltip preview: the grid replaces vanilla's text list (name kept), compact mode, Shift only, and a shulker inside a
 * chest. Screenshots of each with the mouse over the shulker.
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

            // Lines: name kept, vanilla's "x 3" list gone; image is the grid.
            context.runOnClient(c -> {
                ItemStack stack = c.player.getInventory().getItem(0);
                List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(c.level), c.player, TooltipFlag.NORMAL);
                String all = lines.stream().map(Component::getString).reduce("", (a, b) -> a + "|" + b);
                check(all.contains("Loot"), "custom name kept: " + all);
                check(!all.contains("Gold Nugget"), "vanilla list replaced: " + all);
                Optional<TooltipComponent> image = stack.getTooltipImage();
                check(image.isPresent() && image.get().getClass().getSimpleName().equals("ShulkerPreviewData"), "grid image");
                check(stack.getTooltipImage().get() == image.get(), "same image reused next frame (cached)");
            });

            context.runOnClient(c -> c.setScreen(new InventoryScreen(c.player)));
            context.waitTicks(5);
            hoverSlotWith(context, 0);
            shot(context, "40-preview");

            ShulkerViewConfig.INSTANCE.tooltipCompact = true;
            context.waitTicks(2);
            shot(context, "41-preview-compact");
            ShulkerViewConfig.INSTANCE.tooltipCompact = false;

            ShulkerViewConfig.INSTANCE.tooltipShiftOnly = true;
            context.waitTicks(2);
            context.runOnClient(c -> {
                ItemStack stack = c.player.getInventory().getItem(0);
                String all = stack.getTooltipLines(Item.TooltipContext.of(c.level), c.player, TooltipFlag.NORMAL).stream()
                        .map(Component::getString).reduce("", (a, b) -> a + "|" + b);
                check(all.contains("Gold Nugget") && all.contains("Hold Shift"), "Shift only: vanilla list + hint: " + all);
                check(stack.getTooltipImage().isEmpty(), "Shift only: no grid without Shift");
            });
            shot(context, "42-preview-shift-only");
            ShulkerViewConfig.INSTANCE.tooltipShiftOnly = false;
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
            shot(context, "43-preview-in-chest");
            context.runOnClient(c -> c.player.closeContainer());
        }
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
