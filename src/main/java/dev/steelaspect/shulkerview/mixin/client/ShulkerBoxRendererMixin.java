package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.NamedMaterialHolder;
import dev.steelaspect.shulkerview.client.NamedShulkerTextures;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Placed shulkers named Trash / Tools / Food / Blocks use their Alloy Pack texture (see NamedShulkerTextures). */
@Mixin(ShulkerBoxRenderer.class)
public abstract class ShulkerBoxRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void cytra$extractNamedMaterial(ShulkerBoxBlockEntity shulker, ShulkerBoxRenderState state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling, CallbackInfo ci) {
        ((NamedMaterialHolder) state).cytra$setNamedMaterial(NamedShulkerTextures.materialFor(shulker));
    }

    @ModifyVariable(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("STORE"))
    private Material cytra$useNamedMaterial(Material material, ShulkerBoxRenderState state) {
        Material named = ((NamedMaterialHolder) state).cytra$getNamedMaterial();
        return named != null ? named : material;
    }
}
