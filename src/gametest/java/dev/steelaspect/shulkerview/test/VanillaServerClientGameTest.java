package dev.steelaspect.shulkerview.test;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import dev.steelaspect.shulkerview.client.NamedShulkerTextures;
import dev.steelaspect.shulkerview.client.OpenedShulkerCache;
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
 * Client-only fallback: the server acts like one without this mod, so placed shulkers show nothing until the player
 * opens them; after that the label and (with -PalloyPack) the named texture show, taken from the shulker screen.
 */
public class VanillaServerClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");

    @Override
    public void runTest(ClientGameTestContext context) {
        String packPath = System.getProperty("shulkerview.alloyPack");
        if (Boolean.getBoolean("shulkerview.namedOnly") && packPath == null) return;
        String packId = packPath == null ? null : "file/" + Path.of(packPath).getFileName();
        if (packId != null) {
            context.runOnClient(client -> {
                try {
                    Files.copy(Path.of(packPath), client.getResourcePackDirectory().resolve(Path.of(packPath).getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            });
            setPack(context, packId, true);
        }

        ShulkerSummary.testVanillaServer = true;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            BlockPos feet = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
            BlockPos trash = feet.offset(0, 0, 3);
            BlockPos plain = feet.offset(2, 0, 3);
            world.getServer().runCommand(String.format(Locale.ROOT,
                    "setblock %d %d %d minecraft:red_shulker_box[facing=up]{CustomName:\"Trash\",Items:[{Slot:0b,id:\"minecraft:diamond\",count:64}]}",
                    trash.getX(), trash.getY(), trash.getZ()));
            world.getServer().runCommand(String.format(Locale.ROOT,
                    "setblock %d %d %d minecraft:shulker_box[facing=up]{Items:[{Slot:0b,id:\"minecraft:dirt\",count:64}]}",
                    plain.getX(), plain.getY(), plain.getZ()));
            context.waitTicks(20);

            context.runOnClient(client -> {
                ShulkerBoxBlockEntity be = (ShulkerBoxBlockEntity) client.level.getBlockEntity(trash);
                check(be != null && ((ShulkerSummaryHolder) be).cytra$getSummary() == null, "no synced summary from a vanilla server");
                check(be.getCustomName() == null, "no synced name from a vanilla server");
                check(OpenedShulkerCache.get(trash) == null && material(client, trash) == null, "nothing known before opening");
            });
            shot(context, "20-vanilla-before-open");

            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    trash.getX() + 0.5, trash.getY(), trash.getZ() - 2.5,
                    trash.getX() + 0.5, trash.getY() - 1.12 + 0.5, trash.getZ() + 0.5)); // tp aims from the feet
            context.waitTicks(5);
            context.runOnClient(client -> check(client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                    && hit.getBlockPos().equals(trash), "aiming at the Trash box"));
            context.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitTicks(10);
            context.runOnClient(client -> {
                check(client.screen instanceof net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen, "shulker screen opened");
                client.player.closeContainer();
            });
            context.waitTicks(40);

            context.runOnClient(client -> {
                ShulkerSummary c = OpenedShulkerCache.get(trash);
                check(c != null && c.hasIcon() && !c.empty(), "label remembered after opening: " + c);
                check("Trash".equals(OpenedShulkerCache.getName(trash)), "name from the screen title: " + OpenedShulkerCache.getName(trash));
                Material m = material(client, trash);
                if (packId != null) {
                    check(m != null && m.texture().getPath().equals("entity/shulker/trash"), "Trash texture after opening: " + m);
                } else {
                    check(m == null, "vanilla texture without the pack");
                }
                check(OpenedShulkerCache.get(plain) == null && material(client, plain) == null, "unopened box still unknown");
            });
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.1f %.1f %.1f",
                    trash.getX() + 1.0, trash.getY() + 1, trash.getZ() - 3.0, trash.getX() + 1.0, trash.getY() + 0.5, trash.getZ() + 0.5));
            context.waitTicks(10);
            shot(context, "21-vanilla-after-open");
        } finally {
            ShulkerSummary.testVanillaServer = false;
        }
        if (packId != null) setPack(context, packId, false);
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
            reload[0] = client.reloadResourcePacks();
        });
        context.waitFor(client -> reload[0].isDone(), 1200);
        context.waitTicks(40);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Failed: " + what);
        LOG.info("OK: {}", what);
    }

    private static void shot(ClientGameTestContext context, String name) {
        LOG.info("Screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of(name)));
    }
}
