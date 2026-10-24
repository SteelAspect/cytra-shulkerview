package dev.steelaspect.shulkerview.client;

import dev.steelaspect.alloyui.widget.Options;
import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;

/**
 * Every setting in one Alloy UI {@link Options} list, for the settings screen. Getters go through
 * {@link ShulkerViewConfig#INSTANCE} each time because loading the config replaces it. Every change saves at once.
 */
public final class ShulkerViewOptions {
    private ShulkerViewOptions() {}

    public static Options create() {
        Options o = new Options("cytra-shulkerview.option.");
        o.section("section.items")
                .toggle("itemIcon", () -> c().itemIcon, v -> c().itemIcon = v, true).onChange(ShulkerViewConfig::save)
                .toggle("itemBar", () -> c().itemBar, v -> c().itemBar = v, true).onChange(ShulkerViewConfig::save)
                .slider("itemIconSize", 0.25, 1.0, 0.05, () -> c().itemIconSize, v -> c().itemIconSize = v, 0.625)
                .onChange(ShulkerViewConfig::save);
        Component other = Component.translatable("cytra-shulkerview.option.tooltipPreview.other");
        Component off = Component.translatable("cytra-shulkerview.option.tooltipPreview.off");
        BooleanSupplier noPreview = () -> ShulkerPreview.otherModPreviews() || !c().tooltipPreview;
        o.section("section.tooltip")
                .toggle("tooltipPreview", () -> c().tooltipPreview, v -> c().tooltipPreview = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerPreview::otherModPreviews, other)
                .choice("tooltipKeyStyle", () -> List.of(false, true),
                        sbt -> Component.translatable("cytra-shulkerview.option.tooltipKeyStyle." + (sbt ? "sbt" : "alloy")),
                        () -> c().tooltipSbtKeys, v -> c().tooltipSbtKeys = v, false).onChange(ShulkerViewConfig::save)
                .disabledWhen(noPreview, off)
                .choice("tooltipFirstView", () -> List.of(false, true),
                        full -> Component.translatable("cytra-shulkerview.option.tooltipFirstView." + (full ? "full" : "compact")),
                        () -> c().tooltipFullByDefault, v -> c().tooltipFullByDefault = v, false).onChange(ShulkerViewConfig::save)
                .disabledWhen(noPreview, off)
                .choice("previewKey", () -> KEYS, ShulkerViewOptions::keyName,
                        () -> c().previewKey, v -> c().previewKey = v, "key.keyboard.left.shift").onChange(ShulkerViewConfig::save)
                .disabledWhen(noPreview, off)
                .choice("fullPreviewKey", () -> KEYS, ShulkerViewOptions::keyName,
                        () -> c().fullPreviewKey, v -> c().fullPreviewKey = v, "key.keyboard.left.alt").onChange(ShulkerViewConfig::save)
                .disabledWhen(() -> noPreview.getAsBoolean() || !c().tooltipSbtKeys, Component.translatable("cytra-shulkerview.option.fullPreviewKey.off"))
                .toggle("tooltipColored", () -> c().tooltipColored, v -> c().tooltipColored = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(noPreview, off)
                .toggle("tooltipLootTable", () -> c().tooltipLootTable, v -> c().tooltipLootTable = v, false).onChange(ShulkerViewConfig::save)
                .disabledWhen(noPreview, off);
        o.section("section.containers")
                .toggle("otherContainers", () -> c().otherContainers, v -> c().otherContainers = v, true).onChange(ShulkerViewConfig::save)
                .toggle("bundles", () -> c().bundles, v -> c().bundles = v, true).onChange(ShulkerViewConfig::save)
                .toggle("enderChestPreview", () -> c().enderChestPreview, v -> c().enderChestPreview = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(noPreview, off);
        o.section("section.placed")
                .toggle("placedLabels", () -> c().placedLabels, v -> c().placedLabels = v, true).onChange(ShulkerViewConfig::save)
                .number("placedDistance", 1, 128, 1, true, () -> c().placedDistance, v -> c().placedDistance = v.intValue(), 24)
                .onChange(ShulkerViewConfig::save)
                .slider("placedIconSize", 0.2, 0.8, 0.05, () -> c().placedIconSize, v -> c().placedIconSize = v, 0.4)
                .onChange(ShulkerViewConfig::save)
                .toggle("namedTextures", () -> c().namedTextures, v -> c().namedTextures = v, true).onChange(ShulkerViewConfig::save);
        o.section("section.colors")
                .color("barColor", () -> c().barColor, v -> c().barColor = v, 0xFF55BBFF, true).onChange(ShulkerViewConfig::save)
                .color("fullBarColor", () -> c().fullBarColor, v -> c().fullBarColor = v, 0xFF55FF55, true)
                .onChange(ShulkerViewConfig::save);
        return o;
    }

    /** Keys the preview keys can be set to: the modifier keys and a few others no menu uses. */
    static final List<String> KEYS = List.of(
            "key.keyboard.left.shift", "key.keyboard.right.shift", "key.keyboard.left.control", "key.keyboard.right.control",
            "key.keyboard.left.alt", "key.keyboard.right.alt", "key.keyboard.left.win", "key.keyboard.right.win",
            "key.keyboard.tab", "key.keyboard.caps.lock", "key.keyboard.grave.accent", "key.mouse.middle");

    private static Component keyName(String name) {
        return ShulkerPreview.key(name).getDisplayName();
    }

    private static ShulkerViewConfig c() {
        return ShulkerViewConfig.INSTANCE;
    }
}
