package dev.steelaspect.shulkerview.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import dev.steelaspect.shulkerview.client.screen.SettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public final class ShulkerViewClient implements ClientModInitializer {
    private static final String CATEGORY = "key.category.cytra-shulkerview.main";
    /** Unbound by default (no hotkey unless the player sets one). */
    public static final KeyMapping SETTINGS = new KeyMapping("key.cytra-shulkerview.settings", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), CATEGORY);

    @Override
    public void onInitializeClient() {
        ShulkerViewConfig.load();
        OpenedShulkerCache.register();
        EnderChestContents.register();
        PlacedShulkerLabels.register();
        NamedShulkerTextures.register();
        ShulkerPreview.register();
        KeyBindingHelper.registerKeyBinding(SETTINGS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SETTINGS.consumeClick()) {
                if (client.screen == null) client.setScreen(new SettingsScreen(null));
            }
        });
    }
}
