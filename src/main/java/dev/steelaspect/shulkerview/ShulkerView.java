package dev.steelaspect.shulkerview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Common/server side: sends each player's ender chest contents to their client (when it has this mod) so the ender
 * chest item tooltip can preview it. Checked once a second, sent only when something changed.
 */
public final class ShulkerView implements ModInitializer {
    private static final int CHECK_INTERVAL = 20;
    private static final Map<UUID, List<ItemStack>> LAST_SENT = new HashMap<>();
    private static int timer;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(EnderChestPayload.TYPE, EnderChestPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> LAST_SENT.remove(handler.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SENT.remove(handler.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++timer < CHECK_INTERVAL) return;
            timer = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) syncEnderChest(player);
        });
    }

    private static void syncEnderChest(ServerPlayer player) {
        if (ShulkerSummary.testVanillaServer || !ServerPlayNetworking.canSend(player, EnderChestPayload.TYPE)) return;
        PlayerEnderChestContainer chest = player.getEnderChestInventory();
        List<ItemStack> last = LAST_SENT.get(player.getUUID());
        if (last != null && last.size() == chest.getContainerSize()) {
            boolean same = true;
            for (int i = 0; i < last.size() && same; i++) same = ItemStack.matches(last.get(i), chest.getItem(i));
            if (same) return;
        }
        List<ItemStack> items = new ArrayList<>(chest.getContainerSize());
        for (int i = 0; i < chest.getContainerSize(); i++) items.add(chest.getItem(i).copy());
        LAST_SENT.put(player.getUUID(), items);
        ServerPlayNetworking.send(player, new EnderChestPayload(items));
    }
}
