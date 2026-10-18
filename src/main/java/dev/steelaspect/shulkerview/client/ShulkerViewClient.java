package dev.steelaspect.shulkerview.client;

import net.fabricmc.api.ClientModInitializer;

public final class ShulkerViewClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ShulkerViewConfig.load();
        OpenedShulkerCache.register();
        PlacedShulkerLabels.register();
        NamedShulkerTextures.register();
    }
}
