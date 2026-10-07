package dev.steelaspect.shulkerview.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** config/cytra-shulkerview.json. Missing keys keep their defaults; the file is rewritten with all keys on load. */
public final class ShulkerViewConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("cytra-shulkerview");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cytra-shulkerview.json");

    public static ShulkerViewConfig INSTANCE = new ShulkerViewConfig();

    /** Item icon on shulker items in inventories/hotbar when every stack inside is the same item. */
    public boolean itemIcon = true;
    /** Fill bar on shulker items. */
    public boolean itemBar = true;
    /** Icon + fill bar on the side faces of placed shulkers. */
    public boolean placedLabels = true;
    /** Placed labels are only drawn within this many blocks. */
    public int placedDistance = 24;

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                ShulkerViewConfig loaded = GSON.fromJson(reader, ShulkerViewConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (Exception e) {
                LOGGER.warn("Could not read {}, using defaults", FILE, e);
            }
        }
        INSTANCE.placedDistance = Math.max(1, Math.min(128, INSTANCE.placedDistance));
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", FILE, e);
        }
    }
}
