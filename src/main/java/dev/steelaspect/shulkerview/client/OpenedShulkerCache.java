package dev.steelaspect.shulkerview.client;

import dev.steelaspect.shulkerview.ShulkerSummary;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Fallback for servers without this mod: the client can't see inside placed shulkers, so remember what was in each
 * shulker the player opened (keyed by position, cleared on disconnect / when the player breaks it).
 */
public final class OpenedShulkerCache {
    private static final Map<BlockPos, ShulkerSummary> CACHE = new HashMap<>();
    private static final int OPEN_WINDOW_TICKS = 40;

    private static @Nullable BlockPos lastClicked;
    private static int ticksSinceClick = Integer.MAX_VALUE;

    private OpenedShulkerCache() {}

    public static @Nullable ShulkerSummary get(BlockPos pos) {
        return CACHE.get(pos);
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() && level.getBlockEntity(hit.getBlockPos()) instanceof ShulkerBoxBlockEntity) {
                lastClicked = hit.getBlockPos().immutable();
                ticksSinceClick = 0;
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksSinceClick < Integer.MAX_VALUE) ticksSinceClick++;
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ShulkerBoxScreen shulkerScreen) || lastClicked == null || ticksSinceClick > OPEN_WINDOW_TICKS) {
                return;
            }
            BlockPos pos = lastClicked;
            lastClicked = null;
            // Contents arrive a moment after the screen opens, so read them when it closes.
            ScreenEvents.remove(screen).register(closed -> CACHE.put(pos, summarize(shulkerScreen.getMenu())));
        });
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> CACHE.remove(pos));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CACHE.clear());
    }

    private static ShulkerSummary summarize(ShulkerBoxMenu menu) {
        ItemStack[] stacks = new ItemStack[ShulkerSummary.SLOTS];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = menu.getSlot(i).getItem();
        }
        return ShulkerSummary.of(java.util.Arrays.asList(stacks));
    }

}
