package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves out vanilla's "Item x 3" / "and 2 more..." lines while the preview grid shows the contents. */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockTooltipMixin {
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void cytra$hideList(ItemStack stack, @Nullable BlockGetter level, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        if (ShulkerPreview.replacesList(stack)) ci.cancel();
    }
}
