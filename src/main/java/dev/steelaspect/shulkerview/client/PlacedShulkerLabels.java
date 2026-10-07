package dev.steelaspect.shulkerview.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Item-frame-style labels on the four side faces of a placed shulker (the faces around its lid axis): the item icon
 * when the shulker holds one item type, and a fill bar under it.
 */
public final class PlacedShulkerLabels {
    private static final RenderStateDataKey<Label> LABEL = RenderStateDataKey.create(() -> "cytra-shulkerview label");

    private static final float FACE_OFFSET = 0.5f + 0.02f;
    private static final float ICON_SCALE = 0.4f;
    private static final float ICON_Y = 0.06f;
    private static final float BAR_HALF_WIDTH = 0.3f;
    private static final float BAR_TOP = -0.24f;
    private static final float BAR_HEIGHT = 0.06f;
    private static final float BAR_BORDER = 0.015f;

    private PlacedShulkerLabels() {}

    static final class Label {
        final ItemStackRenderState item = new ItemStackRenderState();
        boolean hasIcon;
        float fill;
        int barColor;
    }

    public static void extract(ShulkerBoxBlockEntity shulker, ShulkerBoxRenderState state, Vec3 camera) {
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        BlockPos pos = shulker.getBlockPos();
        if (!config.placedLabels || camera.distanceToSqr(pos.getCenter()) > (double) config.placedDistance * config.placedDistance) {
            state.setData(LABEL, null);
            return;
        }
        ShulkerSummary summary = ((ShulkerSummaryHolder) shulker).cytra$getSummary();
        if (summary == null) summary = OpenedShulkerCache.get(pos);
        if (summary == null || summary.empty()) {
            state.setData(LABEL, null);
            return;
        }

        Label label = state.getData(LABEL);
        if (label == null) label = new Label();
        label.hasIcon = summary.hasIcon();
        label.fill = summary.fill();
        label.barColor = ShulkerItems.barColor(summary);
        if (label.hasIcon) {
            Minecraft.getInstance().getItemModelResolver()
                    .updateForTopItem(label.item, summary.icon(), ItemDisplayContext.FIXED, shulker.getLevel(), null, (int) pos.asLong());
        } else {
            label.item.clear();
        }
        state.setData(LABEL, label);
    }

    public static void submit(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Label label = state.getData(LABEL);
        if (label == null) return;
        Direction facing = state.direction;
        Vec3 center = state.blockPos.getCenter();
        for (Direction face : Direction.values()) {
            if (face.getAxis() == facing.getAxis()) continue;
            // Skip faces turned away from the camera.
            Vec3 toCamera = camera.pos.subtract(center);
            if (toCamera.x * face.getStepX() + toCamera.y * face.getStepY() + toCamera.z * face.getStepZ() <= 0.5) continue;
            submitFace(label, face, facing, state.lightCoords, poseStack, collector);
        }
    }

    private static void submitFace(Label label, Direction face, Direction facing, int light, PoseStack poseStack,
                                   SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0.5f + face.getStepX() * FACE_OFFSET, 0.5f + face.getStepY() * FACE_OFFSET, 0.5f + face.getStepZ() * FACE_OFFSET);

        // Same orientation an item frame on this face would use (local -Z points out of the face).
        Quaternionf rotation = new Quaternionf();
        if (face.getAxis().isHorizontal()) {
            rotation.mul(Axis.YP.rotationDegrees(180.0f - face.toYRot()));
        } else {
            rotation.mul(Axis.XP.rotationDegrees(-90.0f * face.getAxisDirection().getStep())).mul(Axis.YP.rotationDegrees(180.0f));
        }
        // Turn the label so its top points up (or, on a sideways shulker's top/bottom face, toward the lid).
        Vector3f target = face.getAxis().isVertical() ? facing.step() : new Vector3f(0, 1, 0);
        Vector3f up = rotation.transform(new Vector3f(0, 1, 0));
        Vector3f normal = rotation.transform(new Vector3f(0, 0, 1));
        float angle = (float) Math.atan2(new Vector3f(up).cross(target).dot(normal), up.dot(target));
        rotation.mul(Axis.ZP.rotation(angle));
        poseStack.mulPose(rotation);

        if (label.hasIcon) {
            poseStack.pushPose();
            poseStack.translate(0.0f, ICON_Y, 0.0f);
            poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
            label.item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        float fill = label.fill;
        int color = label.barColor;
        collector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            float left = -BAR_HALF_WIDTH;
            float right = BAR_HALF_WIDTH;
            float top = BAR_TOP;
            float bottom = BAR_TOP - BAR_HEIGHT;
            quad(consumer, matrix, left - BAR_BORDER, top + BAR_BORDER, right + BAR_BORDER, bottom - BAR_BORDER, 0.0f, 0xFF000000, light);
            // Local +X is the viewer's left here, so the bar grows from +X toward -X to read left-to-right.
            float fillEnd = right - (right - left) * Math.max(0.03f, fill);
            quad(consumer, matrix, fillEnd, top, right, bottom, -0.002f, color, light);
        });
        poseStack.popPose();
    }

    /** Both windings so the bar shows whichever way the face's local axes ended up. */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float x2, float y2, float z, int argb, int light) {
        consumer.addVertex(matrix, x1, y1, z).setColor(argb).setLight(light);
        consumer.addVertex(matrix, x1, y2, z).setColor(argb).setLight(light);
        consumer.addVertex(matrix, x2, y2, z).setColor(argb).setLight(light);
        consumer.addVertex(matrix, x2, y1, z).setColor(argb).setLight(light);

        consumer.addVertex(matrix, x1, y1, z).setColor(argb).setLight(light);
        consumer.addVertex(matrix, x2, y1, z).setColor(argb).setLight(light);
        consumer.addVertex(matrix, x2, y2, z).setColor(argb).setLight(light);
        consumer.addVertex(matrix, x1, y2, z).setColor(argb).setLight(light);
    }

}
