package dev.steelaspect.shulkerview.test;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Places shulkers with different contents, checks the client got the summaries, and screenshots both looks. */
public class ShulkerViewClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            BlockPos feet = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
            BlockPos o = feet.offset(0, 0, 4);

            setShulker(world, o.offset(-3, 0, 0), "shulker_box[facing=up]", items("diamond", 64, 27));
            setShulker(world, o.offset(-1, 0, 0), "blue_shulker_box[facing=up]", items("spruce_log", 64, 13));
            setShulker(world, o.offset(1, 0, 0), "red_shulker_box[facing=up]",
                    "{slot:0b,id:\"minecraft:dirt\",count:64},{slot:1b,id:\"minecraft:stone\",count:64}".replace("slot", "Slot"));
            setShulker(world, o.offset(3, 0, 0), "lime_shulker_box[facing=east]", items("ender_pearl", 16, 20));
            setShulker(world, o.offset(5, 0, 0), "shulker_box[facing=up]", "");
            // Stacked wall, like a storage room.
            setShulker(world, o.offset(-3, 1, 0), "white_shulker_box[facing=up]", items("cobblestone", 64, 27));
            setShulker(world, o.offset(-1, 1, 0), "white_shulker_box[facing=up]", items("redstone", 64, 5));
            context.waitTicks(20);

            BlockPos diamonds = o.offset(-3, 0, 0);
            BlockPos logs = o.offset(-1, 0, 0);
            BlockPos mixed = o.offset(1, 0, 0);
            context.runOnClient(client -> {
                ShulkerSummary d = summary(client.level.getBlockEntity(diamonds));
                check(d != null && d.icon().is(Items.DIAMOND) && d.isFull(), "full diamonds summary: " + d);
                ShulkerSummary l = summary(client.level.getBlockEntity(logs));
                check(l != null && l.icon().is(Items.SPRUCE_LOG) && Math.abs(l.fill() - 13f / 27f) < 0.001f, "half logs summary: " + l);
                ShulkerSummary m = summary(client.level.getBlockEntity(mixed));
                check(m != null && !m.hasIcon() && !m.empty(), "mixed summary: " + m);
            });

            // Changing contents server side must reach the client.
            world.getServer().runCommand(String.format(Locale.ROOT, "item replace block %d %d %d container.20 with minecraft:dirt 64",
                    logs.getX(), logs.getY(), logs.getZ()));
            context.waitTicks(5);
            context.runOnClient(client -> {
                ShulkerSummary l = summary(client.level.getBlockEntity(logs));
                check(l != null && !l.hasIcon() && Math.abs(l.fill() - 14f / 27f) < 0.001f, "logs after adding dirt: " + l);
            });
            world.getServer().runCommand(String.format(Locale.ROOT, "item replace block %d %d %d container.20 with air",
                    logs.getX(), logs.getY(), logs.getZ()));
            context.waitTicks(5);

            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    o.getX() + 1.0, o.getY() + 2, o.getZ() - 3.0, o.getX() + 1.0, o.getY() + 0.5, o.getZ() + 0.5));
            context.waitTicks(10);
            shot(context, "01-placed-front");
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    o.getX() + 6.5, o.getY() + 1, o.getZ() - 2.5, o.getX() + 1.0, o.getY() + 0.5, o.getZ() + 0.5));
            context.waitTicks(10);
            shot(context, "02-placed-angle");

            // Fallback for servers without the mod: drop the synced summary, open the shulker, close it, remember it.
            BlockPos[] aimed = new BlockPos[1];
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    diamonds.getX() + 0.5, diamonds.getY(), diamonds.getZ() - 2.5, diamonds.getX() + 0.5, diamonds.getY() - 1.12 + 0.5, diamonds.getZ() + 0.5)); // tp aims from the feet
            context.waitTicks(5);
            context.runOnClient(client -> {
                aimed[0] = ((net.minecraft.world.phys.BlockHitResult) client.hitResult).getBlockPos().immutable();
                ShulkerSummaryHolder holder = (ShulkerSummaryHolder) client.level.getBlockEntity(aimed[0]);
                check(holder != null, "aiming at a shulker: " + aimed[0]);
                holder.cytra$setSummary(null);
            });
            context.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitTicks(10);
            context.runOnClient(client -> {
                check(client.screen instanceof net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen, "shulker screen opened");
                client.player.closeContainer();
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                ShulkerSummary c = dev.steelaspect.shulkerview.client.OpenedShulkerCache.get(aimed[0]);
                check(c != null && c.hasIcon() && c.isFull(), "remembered after opening: " + c);
            });

            // Shulker items in the inventory.
            world.getServer().runCommand("clear @p");
            give(world, "shulker_box", items("diamond", 64, 27));
            give(world, "blue_shulker_box", items("spruce_log", 64, 13));
            give(world, "red_shulker_box", "{slot:0,item:{id:\"minecraft:dirt\",count:64}},{slot:1,item:{id:\"minecraft:stone\",count:3}}");
            give(world, "lime_shulker_box", itemComponents("ender_pearl", 16, 27));
            give(world, "shulker_box", "");
            give(world, "white_shulker_box", itemComponents("enchanted_golden_apple", 64, 1));
            context.waitTicks(5);
            context.runOnClient(client -> client.setScreen(new InventoryScreen(client.player)));
            context.waitTicks(5);
            shot(context, "03-inventory");
            context.runOnClient(client -> client.setScreen(null));
        }
    }

    private static ShulkerSummary summary(Object be) {
        return be instanceof ShulkerBoxBlockEntity ? ((ShulkerSummaryHolder) be).cytra$getSummary() : null;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Failed: " + what);
        LOG.info("OK: {}", what);
    }

    /** Block entity NBT item list (Slot/id/count). */
    private static String items(String id, int count, int slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "{Slot:%db,id:\"minecraft:%s\",count:%d}", i, id, count));
        }
        return sb.toString();
    }

    /** minecraft:container component entries. */
    private static String itemComponents(String id, int count, int slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "{slot:%d,item:{id:\"minecraft:%s\",count:%d}}", i, id, count));
        }
        return sb.toString();
    }

    private static void setShulker(TestSingleplayerContext world, BlockPos pos, String block, String items) {
        world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:%s{Items:[%s]}",
                pos.getX(), pos.getY(), pos.getZ(), block, items));
    }

    private static void give(TestSingleplayerContext world, String item, String contents) {
        // Block-entity style lists ({Slot:..}) are converted to component entries for items.
        String entries = contents.contains("Slot:") ? contents.replaceAll("\\{Slot:(\\d+)b,id:(\"[^\"]+\"),count:(\\d+)\\}",
                "{slot:$1,item:{id:$2,count:$3}}") : contents;
        world.getServer().runCommand("give @p minecraft:" + item + "[container=[" + entries + "]]");
    }

    private static void shot(ClientGameTestContext context, String name) {
        LOG.info("Screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of(name)));
    }
}
