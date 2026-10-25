package dev.steelaspect.shulkerview.test;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mixed shulkers: synced top item types, the main-item / 2x2 icons on items and placed boxes, and lid labels (screenshots). */
public class MixedIconClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");

    @Override
    public void runTest(ClientGameTestContext context) {
        if (Only.skip("mixed") || Boolean.getBoolean("shulkerview.namedOnly")) return;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            BlockPos feet = world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition());
            BlockPos o = feet.offset(0, 0, 3);
            // Redstone most, then repeaters, comparators, observers, then a few torches (5th, left out).
            String items = "{Slot:0b,id:\"minecraft:repeater\",count:20},{Slot:1b,id:\"minecraft:redstone\",count:64},"
                    + "{Slot:2b,id:\"minecraft:redstone\",count:64},{Slot:3b,id:\"minecraft:comparator\",count:12},"
                    + "{Slot:4b,id:\"minecraft:observer\",count:8},{Slot:5b,id:\"minecraft:redstone_torch\",count:2}";
            world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:red_shulker_box[facing=up]{Items:[%s]}",
                    o.getX(), o.getY(), o.getZ(), items));
            world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:shulker_box[facing=up]{Items:[%s]}",
                    o.getX() + 2, o.getY(), o.getZ(), items.replace("observer", "dirt").replace("comparator", "stone")));
            context.waitTicks(20);

            context.runOnClient(c -> {
                ShulkerSummary s = ((ShulkerSummaryHolder) c.level.getBlockEntity(o)).cytra$getSummary();
                check(s != null && !s.hasIcon() && s.top().size() == 4, "synced top 4: " + s);
                check(s.top().get(0).is(Items.REDSTONE) && s.top().get(1).is(Items.REPEATER)
                        && s.top().get(2).is(Items.COMPARATOR) && s.top().get(3).is(Items.OBSERVER), "most items first: " + s.top());
                check(s.icons(ShulkerSummary.MixedIcon.NONE).isEmpty(), "none: no icon");
                check(s.icons(ShulkerSummary.MixedIcon.MAIN).size() == 1, "main: one icon");
            });

            String give = "give @p minecraft:red_shulker_box[container=[{slot:0,item:{id:\"minecraft:repeater\",count:20}},"
                    + "{slot:1,item:{id:\"minecraft:redstone\",count:64}},{slot:2,item:{id:\"minecraft:comparator\",count:12}},"
                    + "{slot:3,item:{id:\"minecraft:observer\",count:8}}]]";
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand(give);
            world.getServer().runCommand("give @p minecraft:blue_shulker_box[container=[{slot:0,item:{id:\"minecraft:dirt\",count:64}},"
                    + "{slot:1,item:{id:\"minecraft:stone\",count:3}}]]");
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    o.getX() + 1.5, o.getY() + 1, o.getZ() - 2.5, o.getX() + 1.5, o.getY() + 0.5, o.getZ() + 0.5));

            for (ShulkerSummary.MixedIcon mode : ShulkerSummary.MixedIcon.values()) {
                ShulkerViewConfig.INSTANCE.mixedIcon = mode;
                context.waitTicks(5);
                String name = mode.name().toLowerCase(Locale.ROOT);
                shot(context, "50-mixed-placed-" + name);
                context.runOnClient(c -> c.setScreen(new InventoryScreen(c.player)));
                context.waitTicks(3);
                shot(context, "51-mixed-items-" + name);
                context.runOnClient(c -> c.setScreen(null));
            }
            // From above: labels on the lids too.
            ShulkerViewConfig.INSTANCE.mixedIcon = ShulkerSummary.MixedIcon.TOP4;
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    o.getX() + 1.5, o.getY() + 3, o.getZ() - 1.5, o.getX() + 1.5, o.getY() - 0.5, o.getZ() + 0.5));
            context.waitTicks(5);
            shot(context, "52-lids-top4");
            ShulkerViewConfig.INSTANCE.placedLabelsLid = false;
            context.waitTicks(3);
            shot(context, "53-lids-off");
            ShulkerViewConfig.INSTANCE.placedLabelsLid = true;
            ShulkerViewConfig.INSTANCE.mixedIcon = ShulkerSummary.MixedIcon.NONE;
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Failed: " + what);
        LOG.info("OK: {}", what);
    }

    private static void shot(ClientGameTestContext context, String name) {
        LOG.info("Screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of(name)));
    }
}
