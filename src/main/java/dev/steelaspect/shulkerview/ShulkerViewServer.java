package dev.steelaspect.shulkerview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Optional server side (also runs in singleplayer): sends each player with Shulker View their own ender chest contents,
 * so hovering an ender chest item previews it. Clients without the mod are never sent anything.
 */
public final class ShulkerViewServer implements ModInitializer {
    /** How often (ticks) ender chests are checked for changes. */
    private static final int CHECK_INTERVAL = 5;
    /** What each player was last sent. */
    private static final Map<UUID, List<ItemStack>> SENT = new HashMap<>();
    private static int timer;

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SENT.remove(handler.getPlayer().getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(ShulkerViewServer::tick);
    }

    private static void tick(MinecraftServer server) {
        if (--timer > 0) return;
        timer = CHECK_INTERVAL;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(player, EnderChestPayload.TYPE)) continue;
            PlayerEnderChestContainer chest = player.getEnderChestInventory();
            List<ItemStack> last = SENT.get(player.getUUID());
            if (last != null && same(last, chest)) continue;
            List<ItemStack> now = new ArrayList<>(chest.getContainerSize());
            for (int i = 0; i < chest.getContainerSize(); i++) now.add(chest.getItem(i).copy());
            SENT.put(player.getUUID(), now);
            ServerPlayNetworking.send(player, new EnderChestPayload(now));
        }
    }

    private static boolean same(List<ItemStack> last, PlayerEnderChestContainer chest) {
        if (last.size() != chest.getContainerSize()) return false;
        for (int i = 0; i < last.size(); i++) {
            if (!ItemStack.matches(last.get(i), chest.getItem(i))) return false;
        }
        return true;
    }
}
