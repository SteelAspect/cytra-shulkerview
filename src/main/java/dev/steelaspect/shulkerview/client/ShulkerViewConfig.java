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

    /** Item icon on shulker (and other container) items in inventories/hotbar when every stack inside is the same item. */
    public boolean itemIcon = true;
    /** Fill bar on shulker items. */
    public boolean itemBar = true;
    /** Icon + fill bar on the side faces of placed shulkers. */
    public boolean placedLabels = true;
    /** Placed labels are only drawn within this many blocks. */
    public int placedDistance = 24;
    /** Fill bar colour while the shulker is filling (ARGB). */
    public int barColor = 0xFF55BBFF;
    /** Fill bar colour when the shulker is completely full (ARGB). */
    public int fullBarColor = 0xFF55FF55;
    /** Icon size on shulker items, as a fraction of the 16px slot. */
    public double itemIconSize = 0.625;
    /** Icon size on placed shulker labels, as a fraction of the block face. */
    public double placedIconSize = 0.4;
    /** Placed shulkers named Trash / Tools / Food / Blocks use the Alloy Pack texture for that name (if the pack is on). */
    public boolean namedTextures = true;
    /** Hovering a container item shows its contents as a grid instead of vanilla's text list. */
    public boolean tooltipPreview = true;
    /**
     * Key style. Off (Alloy): the preview always shows on hover, the preview key switches to the other view.
     * On (ShulkerBoxTooltip style): a hint line on hover, preview key = compact, preview key + full key = full.
     */
    public boolean tooltipSbtKeys = false;
    /**
     * Preview keys, as key names ({@code key.keyboard.left.shift}). Kept here and not as vanilla key mappings: on
     * 1.20.4 a key can drive only one mapping, so a second one on Shift would take it away from Sneak.
     */
    public String previewKey = "key.keyboard.left.shift";
    public String fullPreviewKey = "key.keyboard.left.alt";
    /** The view shown first is the full slot grid instead of the compact one (the key then switches to compact). */
    public boolean tooltipFullByDefault = false;
    /** The preview window takes the shulker box's dye colour (otherwise Nocturne's own colours). */
    public boolean tooltipColored = true;
    /** A "Loot table: ..." line on unopened loot containers. */
    public boolean tooltipLootTable = false;
    /** Icon, fill bar and preview for chests, barrels, hoppers, furnaces... (and modded containers) carrying items. */
    public boolean otherContainers = true;
    /** Icon and preview for bundles. */
    public boolean bundles = true;
    /** Preview of the player's ender chest on ender chest items (live when the server has this mod). */
    public boolean enderChestPreview = true;

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
        INSTANCE.itemIconSize = Math.max(0.25, Math.min(1.0, INSTANCE.itemIconSize));
        INSTANCE.placedIconSize = Math.max(0.2, Math.min(0.8, INSTANCE.placedIconSize));
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
