package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;

final class ShulkerItems {
    private ShulkerItems() {}

    /** Configured colours: filling (light blue by default) or full (green by default). Always opaque. */
    static int barColor(ShulkerSummary summary) {
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        return 0xFF000000 | (summary.isFull() ? config.fullBarColor : config.barColor);
    }
}
