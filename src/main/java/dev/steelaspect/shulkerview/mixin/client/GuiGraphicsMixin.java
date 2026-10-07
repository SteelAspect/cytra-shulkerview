package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.ShulkerItemOverlay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the icon + fill bar on shulker box items wherever item decorations are drawn (inventories, hotbar...). */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"))
    private void cytra$shulkerOverlay(Font font, ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
        ShulkerItemOverlay.render((GuiGraphics) (Object) this, stack, x, y);
    }
}
