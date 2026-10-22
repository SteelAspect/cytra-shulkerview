package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shulker items get the slot-grid preview as their tooltip image (vanilla has none for them). */
@Mixin(Item.class)
public abstract class ItemTooltipImageMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void cytra$shulkerPreview(ItemStack stack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        Optional<TooltipComponent> image = ShulkerPreview.imageFor(stack);
        if (image != null) cir.setReturnValue(image);
    }
}
