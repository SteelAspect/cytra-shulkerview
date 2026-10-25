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

/** Swaps vanilla's "Item x 3" lines for the preview's hint line (Shift: preview / Alt+Shift: full) on previewed items. */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void cytra$hideList(DataComponentType<?> type, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        if (type != DataComponents.CONTAINER) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!ShulkerPreview.replacesList(self)) return;
        Component hint = ShulkerPreview.hint(self);
        if (hint != null) lines.accept(hint.copy().withStyle(ChatFormatting.GRAY));
        ci.cancel();
    }
}
