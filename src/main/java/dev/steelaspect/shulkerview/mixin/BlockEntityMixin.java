package dev.steelaspect.shulkerview.mixin;

import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/**
 * When a placed shulker's contents change (players, hoppers, droppers all call setChanged), resend its update packet
 * to players that can see it, but only if the summary they see actually changed.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Shadow protected @Nullable Level level;
    @Shadow @Final protected BlockPos worldPosition;

    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void cytra$syncShulker(CallbackInfo ci) {
        if (!((Object) this instanceof ShulkerBoxBlockEntity shulker) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ShulkerSummaryHolder holder = (ShulkerSummaryHolder) shulker;
        ShulkerSummary now = ShulkerSummary.of(((ContainerItemsAccessor) shulker).cytra$getItems());
        if (now.looksSame(holder.cytra$getSummary())) return;
        holder.cytra$setSummary(now);
        serverLevel.getChunkSource().blockChanged(worldPosition);
    }
}
