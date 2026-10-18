package dev.steelaspect.shulkerview.mixin.client;

import dev.steelaspect.shulkerview.client.NamedMaterialHolder;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.resources.model.Material;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ShulkerBoxRenderState.class)
public abstract class ShulkerBoxRenderStateMixin implements NamedMaterialHolder {
    @Unique
    private @Nullable Material cytra$namedMaterial;

    @Override
    public @Nullable Material cytra$getNamedMaterial() {
        return cytra$namedMaterial;
    }

    @Override
    public void cytra$setNamedMaterial(@Nullable Material material) {
        cytra$namedMaterial = material;
    }
}
