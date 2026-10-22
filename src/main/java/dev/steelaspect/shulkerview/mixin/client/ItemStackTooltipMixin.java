package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves out vanilla's "Item x 3" lines when the preview grid shows them; adds a Shift hint in Shift-only mode. */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void cytra$hideList(DataComponentType<?> type, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        if (type != DataComponents.CONTAINER) return;
        ItemStack self = (ItemStack) (Object) this;
        if (ShulkerPreview.replacesList(self)) {
            ci.cancel();
        } else if (ShulkerPreview.wantsShiftHint(self)) {
            // Vanilla's list still goes in (this method carries on); the hint comes right before it.
            lines.accept(Component.translatable("cytra-shulkerview.tooltip.shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
