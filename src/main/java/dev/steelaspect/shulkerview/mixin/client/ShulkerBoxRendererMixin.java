package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.NamedShulkerTextures;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Placed shulkers named Trash / Tools / Food / Blocks use their Alloy Pack texture (see NamedShulkerTextures). */
@Mixin(ShulkerBoxRenderer.class)
public abstract class ShulkerBoxRendererMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("STORE"))
    private Material cytra$useNamedMaterial(Material material, ShulkerBoxBlockEntity shulker) {
        Material named = NamedShulkerTextures.materialFor(shulker);
        return named != null ? named : material;
    }
}
