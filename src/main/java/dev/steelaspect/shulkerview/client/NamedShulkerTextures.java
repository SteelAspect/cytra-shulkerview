package dev.steelaspect.shulkerview.client;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placed shulkers named Trash / Tools / Food / Blocks (any case) use the matching Alloy Pack texture
 * ({@code alloy:entity/shulker/<name>} in the shulker atlas), the same one the pack gives the item. Without the pack
 * (sprite missing) they render vanilla.
 *
 * <p>The name comes from the server (this mod adds it to the shulker's update tag) or, on servers without the mod,
 * from the title of the shulker screen the last time the player opened it.
 *
 * <p>Optimized Block Entities bakes closed shulkers into the chunk mesh by block state, which can't vary per box, so
 * named boxes are switched to its normal block entity rendering (its own forceEntity flag).
 */
public final class NamedShulkerTextures {
    private static final Logger LOGGER = LoggerFactory.getLogger("cytra-shulkerview");
    private static final Map<String, Material> MATERIALS = Map.of(
            "trash", material("trash"),
            "tools", material("tools"),
            "food", material("food"),
            "blocks", material("blocks"));
    private static final int OBE_SCAN_INTERVAL = 20;

    private static @Nullable Method obeForceEntity;
    private static @Nullable Method obeSetDirty;
    /** Boxes this mod told OBE to draw as block entities. */
    private static final Map<BlockEntity, Boolean> OBE_FORCED = new WeakHashMap<>();
    private static int obeTimer;

    private NamedShulkerTextures() {}

    private static Material material(String name) {
        return Sheets.SHULKER_MAPPER.apply(Identifier.fromNamespaceAndPath("alloy", name));
    }

    /** Texture for this placed shulker, or null for vanilla. */
    public static @Nullable Material materialFor(ShulkerBoxBlockEntity shulker) {
        if (!ShulkerViewConfig.INSTANCE.namedTextures) return null;
        Component name = shulker.getCustomName();
        String text = name != null ? name.getString() : OpenedShulkerCache.getName(shulker.getBlockPos());
        if (text == null) return null;
        Material material = MATERIALS.get(text.trim().toLowerCase(Locale.ROOT));
        if (material == null) return null;
        // Not in the atlas (Alloy Pack off) -> the atlas' missing sprite.
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(material);
        return !sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? material : null;
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("obe")) return;
        try {
            Class<?> ext = Class.forName("fr.madu59.obe.client.renderer.entity.ext.EntityExt");
            Class<?> manager = Class.forName("fr.madu59.obe.client.renderer.misc.RenderModeManager");
            obeForceEntity = ext.getMethod("forceEntity", boolean.class);
            obeSetDirty = manager.getMethod("setDirty", BlockPos.class);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Optimized Block Entities found but its API changed; named shulker textures only show while open", e);
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (obeForceEntity == null || client.level == null || client.player == null || --obeTimer > 0) return;
            obeTimer = OBE_SCAN_INTERVAL;
            updateObe(client.level, client.player.blockPosition(), client.options.getEffectiveRenderDistance());
        });
    }

    private static void updateObe(ClientLevel level, BlockPos around, int chunkRadius) {
        int pcx = around.getX() >> 4;
        int pcz = around.getZ() >> 4;
        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof ShulkerBoxBlockEntity shulker)) continue;
                    boolean want = materialFor(shulker) != null;
                    if (OBE_FORCED.getOrDefault(be, false) == want) continue;
                    try {
                        obeForceEntity.invoke(be, want);
                        obeSetDirty.invoke(null, be.getBlockPos());
                    } catch (ReflectiveOperationException | IllegalArgumentException e) {
                        LOGGER.warn("Couldn't switch Optimized Block Entities rendering for a named shulker", e);
                        obeForceEntity = null;
                        return;
                    }
                    if (want) OBE_FORCED.put(be, true); else OBE_FORCED.remove(be);
                }
            }
        }
    }
}
