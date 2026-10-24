package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.EnderChestPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The player's ender chest as the client last knew it: sent by the server when it has Shulker View, otherwise read
 * from the ender chest screen each time the player closes it (until then, ender chest items show no preview).
 */
public final class EnderChestContents {
    private static ContainerContents.@Nullable Contents contents;
    private static boolean fromServer;

    private EnderChestContents() {}

    public static ContainerContents.@Nullable Contents get() {
        return contents;
    }

    /** True once the server has sent the ender chest (it has this mod). */
    public static boolean fromServer() {
        return fromServer;
    }

    static void set(List<ItemStack> slots, boolean server) {
        contents = ContainerContents.enderChest(new ArrayList<>(slots));
        fromServer |= server;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EnderChestPayload.TYPE, (packet, player, sender) -> set(packet.items(), true));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ContainerScreen chest) || fromServer || !isEnderChest(screen)) return;
            // Contents arrive a moment after the screen opens, so read them when it closes.
            ScreenEvents.remove(screen).register(closed -> {
                ChestMenu menu = chest.getMenu();
                int size = menu.getContainer().getContainerSize();
                List<ItemStack> slots = new ArrayList<>(size);
                for (int i = 0; i < size; i++) slots.add(menu.getSlot(i).getItem().copy());
                set(slots, false);
            });
        });
        // Cleared on join too: a direct server switch may skip the disconnect event.
        ClientPlayConnectionEvents.INIT.register((handler, client) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    private static void clear() {
        contents = null;
        fromServer = false;
    }

    private static boolean isEnderChest(net.minecraft.client.gui.screens.Screen screen) {
        return screen.getTitle().getContents() instanceof TranslatableContents t && t.getKey().equals("container.enderchest");
    }
}
