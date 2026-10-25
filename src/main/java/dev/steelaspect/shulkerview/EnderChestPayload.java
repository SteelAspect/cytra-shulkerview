package dev.steelaspect.shulkerview;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Server to client: the player's ender chest contents (27 slots, EMPTY for free ones), for the ender chest preview. */
public record EnderChestPayload(List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<EnderChestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("cytra-shulkerview", "ender_chest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnderChestPayload> CODEC =
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(EnderChestPayload::new, EnderChestPayload::items);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
