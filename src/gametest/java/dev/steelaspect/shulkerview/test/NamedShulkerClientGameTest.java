package dev.steelaspect.shulkerview.test;

import dev.steelaspect.shulkerview.client.NamedShulkerTextures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placed shulkers named Trash / tools / FOOD / Blocks get the Alloy Pack texture; other names and unnamed boxes
 * don't, and nothing changes without the pack. Needs -PalloyPack=<Alloy Pack zip>; skipped otherwise.
 */
public class NamedShulkerClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");

    @Override
    public void runTest(ClientGameTestContext context) {
        String packPath = System.getProperty("shulkerview.alloyPack");
        if (packPath == null) {
            LOG.info("No -PalloyPack, named shulker test skipped");
            return;
        }
        String packId = "file/" + Path.of(packPath).getFileName();
        context.runOnClient(client -> {
            try {
                Files.copy(Path.of(packPath), client.getResourcePackDirectory().resolve(Path.of(packPath).getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        setPack(context, packId, true);

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            BlockPos feet = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
            BlockPos o = feet.offset(0, 0, 4);
            String[][] boxes = {
                    {"red_shulker_box", "Trash", "trash"},
                    {"gray_shulker_box", "tools", "tools"},
                    {"green_shulker_box", "FOOD", "food"},
                    {"brown_shulker_box", "Blocks", "blocks"},
                    {"red_shulker_box", "Other", null},
                    {"red_shulker_box", null, null},
            };
            List<BlockPos> spots = new ArrayList<>();
            for (int i = 0; i < boxes.length; i++) {
                BlockPos pos = o.offset(-5 + i * 2, 0, 0);
                spots.add(pos);
                String name = boxes[i][1] == null ? "" : "{CustomName:\"" + boxes[i][1] + "\"}";
                world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:%s[facing=up]%s",
                        pos.getX(), pos.getY(), pos.getZ(), boxes[i][0], name));
            }
            // A second row facing the camera, so the lid's side is visible too.
            for (int i = 0; i < 4; i++) {
                BlockPos pos = o.offset(-5 + i * 2, 1, 1);
                world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:%s[facing=north]{CustomName:\"%s\"}",
                        pos.getX(), pos.getY(), pos.getZ(), boxes[i][0], boxes[i][1]));
            }
            context.waitTicks(30);

            context.runOnClient(client -> {
                for (int i = 0; i < boxes.length; i++) {
                    Material m = material(client, spots.get(i));
                    String want = boxes[i][2];
                    check(want == null ? m == null : m != null && m.texture().getPath().equals("entity/shulker/" + want),
                            boxes[i][1] + " -> " + (m == null ? "vanilla" : m.texture()));
                }
            });

            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    o.getX() + 0.0, o.getY() + 2, o.getZ() - 4.0, o.getX() + 0.0, o.getY() + 0.5, o.getZ() + 0.5));
            context.waitTicks(30);
            shot(context, "10-named-with-pack");

            // Open one: the named texture must stay on while the lid moves.
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    spots.get(0).getX() + 0.5, spots.get(0).getY(), spots.get(0).getZ() - 2.5,
                    spots.get(0).getX() + 0.5, spots.get(0).getY() - 1.12 + 0.5, spots.get(0).getZ() + 0.5));
            context.waitTicks(5);
            context.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitTicks(10);
            context.runOnClient(client -> client.player.closeContainer());
            context.waitTicks(3);
            shot(context, "11-named-closing");
            context.waitTicks(40);
            shot(context, "12-named-closed-again");

            setPack(context, packId, false);
            context.runOnClient(client -> {
                for (BlockPos pos : spots) check(material(client, pos) == null, "vanilla without the pack at " + pos.toShortString());
            });
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    o.getX() + 0.0, o.getY() + 2, o.getZ() - 4.0, o.getX() + 0.0, o.getY() + 0.5, o.getZ() + 0.5));
            context.waitTicks(30);
            shot(context, "13-named-without-pack");
        }
    }

    private static Material material(Minecraft client, BlockPos pos) {
        return client.level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulker ? NamedShulkerTextures.materialFor(shulker) : null;
    }

    private static void setPack(ClientGameTestContext context, String packId, boolean on) {
        CompletableFuture<?>[] reload = new CompletableFuture<?>[1];
        context.runOnClient(client -> {
            var repo = client.getResourcePackRepository();
            repo.reload();
            List<String> selected = new ArrayList<>(repo.getSelectedIds());
            selected.remove(packId);
            if (on) selected.add(packId);
            repo.setSelected(selected);
            check(repo.getSelectedIds().contains(packId) == on, "Alloy Pack " + (on ? "on" : "off"));
            reload[0] = client.reloadResourcePacks();
        });
        context.waitFor(client -> reload[0].isDone(), 1200);
        context.waitTicks(40); // let the loading overlay fade
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Failed: " + what);
        LOG.info("OK: {}", what);
    }

    private static void shot(ClientGameTestContext context, String name) {
        LOG.info("Screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of(name)));
    }
}
