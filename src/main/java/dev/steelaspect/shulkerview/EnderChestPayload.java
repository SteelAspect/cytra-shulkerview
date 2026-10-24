package dev.steelaspect.shulkerview;

import java.util.List;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Server to client: the player's ender chest contents, every slot in order (sent on join and when they change). */
public record EnderChestPayload(List<ItemStack> items) implements FabricPacket {
    public static final PacketType<EnderChestPayload> TYPE = PacketType.create(
            new ResourceLocation("cytra-shulkerview", "ender_chest"),
            buf -> new EnderChestPayload(buf.readList(FriendlyByteBuf::readItem)));

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
