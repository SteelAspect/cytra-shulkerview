package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Container items get the slot-grid preview as their tooltip image. Hooked on the stack, not the item, so it also
 * replaces the bundle's own image (BundleItem overrides Item.getTooltipImage).
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipImageMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void cytra$containerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        Optional<TooltipComponent> image = ShulkerPreview.imageFor((ItemStack) (Object) this);
        if (image != null) cir.setReturnValue(image);
    }
}
