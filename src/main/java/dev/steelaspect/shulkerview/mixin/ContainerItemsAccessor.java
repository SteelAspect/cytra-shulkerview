package dev.steelaspect.shulkerview.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShulkerBoxBlockEntity.class)
public interface ContainerItemsAccessor {
    @Invoker("getItems")
    NonNullList<ItemStack> cytra$getItems();
}
