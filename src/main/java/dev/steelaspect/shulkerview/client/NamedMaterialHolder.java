package dev.steelaspect.shulkerview.client;

import net.minecraft.client.resources.model.Material;
import org.jspecify.annotations.Nullable;

/** Added to ShulkerBoxRenderState: the named-box texture to draw instead of the colour's, if any. */
public interface NamedMaterialHolder {
    @Nullable Material cytra$getNamedMaterial();

    void cytra$setNamedMaterial(@Nullable Material material);
}
