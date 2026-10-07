package dev.steelaspect.shulkerview.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.steelaspect.shulkerview.client.PlacedShulkerLabels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the icon + fill bar labels to placed shulker boxes. The shulker model itself is untouched. */
@Mixin(ShulkerBoxRenderer.class)
public abstract class ShulkerBoxRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void cytra$extractLabel(ShulkerBoxBlockEntity shulker, ShulkerBoxRenderState state, float partialTick, Vec3 camera,
                                    ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling, CallbackInfo ci) {
        PlacedShulkerLabels.extract(shulker, state, camera);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void cytra$submitLabel(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                   CameraRenderState camera, CallbackInfo ci) {
        PlacedShulkerLabels.submit(state, poseStack, collector, camera);
    }
}
