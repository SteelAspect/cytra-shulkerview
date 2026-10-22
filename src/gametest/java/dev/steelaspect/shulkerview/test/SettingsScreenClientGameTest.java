package dev.steelaspect.shulkerview.test;

import dev.steelaspect.alloyui.widget.SettingRow;
import dev.steelaspect.alloyui.widget.Widget;
import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import dev.steelaspect.shulkerview.client.screen.SettingsScreen;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The own settings screen (Alloy UI): every option has a row, a real click flips a switch and saves, Esc closes. */
public class SettingsScreenClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerViewTest");
    private static final int SCALE = 2;
    private static final String[] IDS = {"itemIcon", "itemBar", "itemIconSize", "placedLabels", "placedDistance",
            "placedIconSize", "namedTextures", "tooltipPreview", "tooltipShiftOnly", "tooltipCompact", "barColor", "fullBarColor"};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (Only.skip("settings")) return;
        if (Boolean.getBoolean("shulkerview.namedOnly")) return;
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(c -> {
            c.options.guiScale().set(SCALE);
            c.resizeDisplay();
        });
        context.setScreen(() -> new SettingsScreen(null));
        context.waitTicks(5);
        LOG.info("Screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of("30-settings")));

        for (String id : IDS) {
            check(context.computeOnClient(c -> c.screen instanceof SettingsScreen s && s.rowForTest(id) != null), "row for " + id);
        }

        boolean before = context.computeOnClient(c -> ShulkerViewConfig.INSTANCE.itemIcon);
        clickControl(context, "itemIcon");
        boolean after = context.computeOnClient(c -> ShulkerViewConfig.INSTANCE.itemIcon);
        check(after != before, "clicking the switch flips itemIcon");
        String saved = read(FabricLoader.getInstance().getConfigDir().resolve("cytra-shulkerview.json"));
        check(saved.contains("\"itemIcon\": " + after), "change saved at once");
        clickControl(context, "itemIcon");
        check(context.computeOnClient(c -> ShulkerViewConfig.INSTANCE.itemIcon) == before, "clicking again flips back");

        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        context.waitTicks(3);
        check(context.computeOnClient(c -> !(c.screen instanceof SettingsScreen)), "Esc closes the screen");
    }

    /** Moves the mouse onto the option's control and clicks it, like a player. */
    private static void clickControl(ClientGameTestContext context, String id) {
        double[] at = context.computeOnClient(c -> {
            if (!(c.screen instanceof SettingsScreen s)) return null;
            SettingRow row = s.rowForTest(id);
            if (row == null) return null;
            Widget w = row.control();
            float f = s.uiScale() * SCALE;
            return new double[] {(w.x() + w.w() / 2) * f, (w.y() + w.h() / 2) * f};
        });
        check(at != null, "control for " + id);
        context.getInput().setCursorPos(at[0], at[1]);
        context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Failed: " + what);
        LOG.info("OK: {}", what);
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (Exception e) {
            return "";
        }
    }
}
