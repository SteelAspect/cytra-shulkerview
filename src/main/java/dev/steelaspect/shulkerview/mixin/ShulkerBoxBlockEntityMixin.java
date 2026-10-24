package dev.steelaspect.shulkerview.mixin;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server side: shulkers send a small summary (icon item + fill) to clients in their block entity update tag, which
 * vanilla already sends with chunks and on block entity updates. Vanilla clients ignore the extra key. Nothing about
 * how the shulker stores or moves items changes. The custom name rides along so named boxes can get their texture.
 * Client side: reads that summary back so the renderer can draw it.
 */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin extends RandomizableContainerBlockEntity implements ShulkerSummaryHolder {
    @Unique
    private @Nullable ShulkerSummary cytra$summary;

    protected ShulkerBoxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ShulkerSummary cytra$getSummary() {
        return cytra$summary;
    }

    @Override
    public void cytra$setSummary(@Nullable ShulkerSummary summary) {
        cytra$summary = summary;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        ShulkerSummary summary = ShulkerSummary.of(getItems());
        cytra$summary = summary;
        tag.put(ShulkerSummary.TAG_KEY, summary.toTag());
        // The name too (vanilla's own key, so the client's load reads it back), for named-box textures.
        Component name = getCustomName();
        if (name != null) tag.putString("CustomName", Component.Serializer.toJson(name));
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity) (Object) this);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void cytra$readSummary(CompoundTag tag, CallbackInfo ci) {
        // Only client update tags carry this key; saved worlds never do, so on the server this just clears the cache.
        cytra$summary = tag.contains(ShulkerSummary.TAG_KEY, CompoundTag.TAG_COMPOUND)
                ? ShulkerSummary.fromTag(tag.getCompound(ShulkerSummary.TAG_KEY)) : null;
        if (cytra$summary != null) ShulkerSummary.serverSyncSeen = true;
    }
}
