package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.EnderChestPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

/**
 * The player's ender chest contents as last known: sent by the server when it has this mod, otherwise read from the
 * ender chest screen when the player closes it (remembered until they leave the server).
 */
public final class EnderChestCache {
    private static final int SLOTS = 27;
    private static @Nullable ItemContainerContents contents;

    private EnderChestCache() {}

    /** Last known contents, or null when unknown. */
    public static @Nullable ItemContainerContents contents() {
        return contents;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EnderChestPayload.TYPE, (payload, context) -> set(payload.items()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> contents = null);
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ContainerScreen chest) || !isEnderChest(screen.getTitle())) return;
            // Contents arrive a moment after the screen opens, so read them when it closes.
            ScreenEvents.remove(screen).register(closed -> {
                List<ItemStack> items = new ArrayList<>(SLOTS);
                for (int i = 0; i < SLOTS && i < chest.getMenu().slots.size(); i++) items.add(chest.getMenu().getSlot(i).getItem().copy());
                set(items);
            });
        });
    }

    private static boolean isEnderChest(net.minecraft.network.chat.Component title) {
        return title.getContents() instanceof TranslatableContents t && t.getKey().equals("container.enderchest");
    }

    private static void set(List<ItemStack> items) {
        ItemContainerContents next = ItemContainerContents.fromItems(items);
        // Keep the same object when nothing changed, so the tooltip preview's cache stays valid.
        if (!next.equals(contents)) contents = next;
    }
}
