package dev.steelaspect.shulkerview.test;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.client.EnderChestCache;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ender chest preview: the server sends the player's ender chest, and the ender chest item previews it with Shift.
 * Without the mod on the server, the contents are learnt by opening the ender chest once.
 */
public class EnderChestClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");
    private static final int SCALE = 2;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (Only.skip("ender") || Boolean.getBoolean("shulkerview.namedOnly")) return;
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(c -> {
            c.options.guiScale().set(SCALE);
            c.resizeDisplay();
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("item replace entity @p enderchest.0 with minecraft:diamond 64");
            world.getServer().runCommand("item replace entity @p enderchest.4 with minecraft:golden_apple 7");
            world.getServer().runCommand("item replace entity @p enderchest.26 with minecraft:netherite_pickaxe");
            world.getServer().runCommand("give @p minecraft:ender_chest");
            context.waitTicks(30);
            context.runOnClient(c -> {
                var contents = EnderChestCache.contents();
                check(contents != null && contents.nonEmptyItems().iterator().next().is(Items.DIAMOND), "server sent the ender chest: " + contents);
                ItemStack item = c.player.getInventory().getItem(0);
                check(item.is(Items.ENDER_CHEST) && item.getTooltipImage().isEmpty(), "no window without Shift");
            });

            context.runOnClient(c -> c.setScreen(new InventoryScreen(c.player)));
            context.waitTicks(5);
            hoverHotbar(context, 0);
            context.getInput().holdKey(GLFW.GLFW_KEY_LEFT_SHIFT);
            context.waitTicks(2);
            context.runOnClient(c -> check(c.player.getInventory().getItem(0).getTooltipImage().isPresent(), "Shift: ender chest window"));
            shot(context, "60-ender-compact");
            context.getInput().holdKey(GLFW.GLFW_KEY_LEFT_ALT);
            context.waitTicks(2);
            shot(context, "61-ender-full");
            context.getInput().releaseKey(GLFW.GLFW_KEY_LEFT_ALT);
            context.getInput().releaseKey(GLFW.GLFW_KEY_LEFT_SHIFT);

            // An ender chest change reaches the client.
            world.getServer().runCommand("item replace entity @p enderchest.0 with minecraft:emerald 3");
            context.waitTicks(30);
            context.runOnClient(c -> check(EnderChestCache.contents().nonEmptyItems().iterator().next().is(Items.EMERALD), "change synced"));
            context.runOnClient(c -> c.setScreen(null));
        }

        // Server without the mod: unknown until the player opens their ender chest.
        ShulkerSummary.testVanillaServer = true;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("item replace entity @p enderchest.3 with minecraft:redstone 64");
            context.waitTicks(30);
            context.runOnClient(c -> check(EnderChestCache.contents() == null, "nothing known on a vanilla server"));
            BlockPos feet = world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition());
            BlockPos chest = feet.offset(0, 0, 2);
            world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:ender_chest", chest.getX(), chest.getY(), chest.getZ()));
            context.waitTicks(5);
            context.runOnClient(c -> c.gameMode.useItemOn(c.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chest),
                            net.minecraft.core.Direction.NORTH, chest, false)));
            context.waitTicks(10);
            check(context.computeOnClient(c -> c.screen instanceof ContainerScreen), "ender chest opened");
            context.runOnClient(c -> c.player.closeContainer());
            context.waitTicks(3);
            context.runOnClient(c -> {
                var contents = EnderChestCache.contents();
                check(contents != null && contents.nonEmptyItems().iterator().next().is(Items.REDSTONE), "learnt from the screen: " + contents);
            });
        } finally {
            ShulkerSummary.testVanillaServer = false;
        }
    }

    private static void hoverHotbar(ClientGameTestContext context, int slot) {
        double[] at = context.computeOnClient(c -> {
            if (!(c.screen instanceof InventoryScreen screen)) return null;
            for (var s : screen.getMenu().slots) {
                if (s.container == c.player.getInventory() && s.getContainerSlot() == slot) {
                    int left = (screen.width - 176) / 2, top = (screen.height - 166) / 2;
                    return new double[] {(left + s.x + 8) * SCALE, (top + s.y + 8) * SCALE};
                }
            }
            return null;
        });
        check(at != null, "hotbar slot " + slot);
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
