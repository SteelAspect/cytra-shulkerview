package dev.steelaspect.shulkerview.client;

import dev.steelaspect.alloyui.widget.Options;

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

    private static ShulkerViewConfig c() {
        return ShulkerViewConfig.INSTANCE;
    }
}
