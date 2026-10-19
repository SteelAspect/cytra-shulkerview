package dev.steelaspect.shulkerview.mixin;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (ShulkerSummary.testVanillaServer) return tag;
        ShulkerSummary summary = ShulkerSummary.of(getItems());
        cytra$summary = summary;
        tag.put(ShulkerSummary.TAG_KEY, summary.toTag(registries));
        // The name too (vanilla's own key, so the client's loadAdditional reads it back), for named-box textures.
        tag.storeNullable("CustomName", ComponentSerialization.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), getCustomName());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        if (ShulkerSummary.testVanillaServer) return super.getUpdatePacket();
        return ClientboundBlockEntityDataPacket.create((BlockEntity) (Object) this);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void cytra$readSummary(ValueInput input, CallbackInfo ci) {
        // Only client update tags carry this key; saved worlds never do, so on the server this just clears the cache.
        cytra$summary = input.child(ShulkerSummary.TAG_KEY).map(ShulkerSummary::fromInput).orElse(null);
        if (cytra$summary != null) ShulkerSummary.serverSyncSeen = true;
    }
}
