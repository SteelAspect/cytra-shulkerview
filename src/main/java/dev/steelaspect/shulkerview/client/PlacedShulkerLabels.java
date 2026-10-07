package dev.steelaspect.shulkerview.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Item-frame-style labels on the four side faces of a placed shulker (the faces around its lid axis): the item icon
 * when the shulker holds one item type, and a fill bar under it.
 *
 * <p>Drawn from Fabric's world render events for every shulker near the camera, not from the vanilla shulker
 * renderer, so they also show when another mod draws closed shulkers itself (e.g. Optimized Block Entities, which
 * only uses the vanilla renderer while the lid moves).
 */
public final class PlacedShulkerLabels {
    private static final float FACE_OFFSET = 0.5f + 0.02f;
    private static final float ICON_Y = 0.06f;
    private static final float BAR_HALF_WIDTH = 0.3f;
    private static final float BAR_TOP = -0.24f;
    private static final float BAR_HEIGHT = 0.06f;
    private static final float BAR_BORDER = 0.015f;
    /** How often (ticks) the list of nearby shulkers is rebuilt. */
    private static final int SCAN_INTERVAL = 10;

    /** Shulker block entities near the player, refreshed every few ticks. */
    private static final List<BlockPos> NEARBY = new ArrayList<>();
    /** Labels to draw this frame (objects reused between frames). */
    private static final List<Label> LABELS = new ArrayList<>();
    private static int labelCount;
    private static Vec3 cameraPos = Vec3.ZERO;
    private static int scanTimer;

    private PlacedShulkerLabels() {}

    static final class Label {
        final ItemStackRenderState item = new ItemStackRenderState();
        BlockPos pos = BlockPos.ZERO;
        Direction facing = Direction.UP;
        boolean hasIcon;
        float iconScale;
        float fill;
        int barColor;
        final int[] faceLight = new int[6];
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) {
                NEARBY.clear();
                return;
            }
            if (--scanTimer <= 0) {
                scanTimer = SCAN_INTERVAL;
                scan(client.level, client.player.position());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            NEARBY.clear();
            labelCount = 0;
        });
        WorldRenderEvents.END_EXTRACTION.register(PlacedShulkerLabels::extract);
        WorldRenderEvents.BEFORE_ENTITIES.register(PlacedShulkerLabels::submit);
    }

    private static void scan(ClientLevel level, Vec3 around) {
        NEARBY.clear();
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        if (!config.placedLabels) return;
        double range = config.placedDistance + 8;
        int minCx = BlockPos.containing(around.x - range, 0, 0).getX() >> 4;
        int maxCx = BlockPos.containing(around.x + range, 0, 0).getX() >> 4;
        int minCz = BlockPos.containing(0, 0, around.z - range).getZ() >> 4;
        int maxCz = BlockPos.containing(0, 0, around.z + range).getZ() >> 4;
        double rangeSq = range * range;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ShulkerBoxBlockEntity && be.getBlockPos().getCenter().distanceToSqr(around) <= rangeSq) {
                        NEARBY.add(be.getBlockPos());
                    }
                }
            }
        }
    }

    private static void extract(WorldExtractionContext context) {
        labelCount = 0;
        ShulkerViewConfig config = ShulkerViewConfig.INSTANCE;
        if (!config.placedLabels || NEARBY.isEmpty()) return;
        ClientLevel level = context.world();
        cameraPos = context.camera().position();
        double maxSq = (double) config.placedDistance * config.placedDistance;
        for (BlockPos pos : NEARBY) {
            if (pos.getCenter().distanceToSqr(cameraPos) > maxSq) continue;
            if (!(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulker)) continue;
            if (!context.frustum().isVisible(new AABB(pos).inflate(0.1))) continue;
            ShulkerSummary summary = ((ShulkerSummaryHolder) shulker).cytra$getSummary();
            if (summary == null) summary = OpenedShulkerCache.get(pos);
            if (summary == null || summary.empty()) continue;

            if (labelCount == LABELS.size()) LABELS.add(new Label());
            Label label = LABELS.get(labelCount++);
            label.pos = pos;
            label.facing = shulker.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
            label.hasIcon = summary.hasIcon();
            label.iconScale = (float) config.placedIconSize;
            label.fill = summary.fill();
            label.barColor = ShulkerItems.barColor(summary);
            for (Direction face : Direction.values()) {
                // Light where the label is: in front of the face (the shulker block itself is dark inside).
                label.faceLight[face.ordinal()] = LevelRenderer.getLightColor(level, pos.relative(face));
            }
            if (label.hasIcon) {
                Minecraft.getInstance().getItemModelResolver()
                        .updateForTopItem(label.item, summary.icon(), ItemDisplayContext.FIXED, level, null, (int) pos.asLong());
            } else {
                label.item.clear();
            }
        }
    }

    private static void submit(WorldRenderContext context) {
        if (labelCount == 0) return;
        PoseStack poseStack = context.matrices();
        SubmitNodeCollector collector = context.commandQueue();
        for (int i = 0; i < labelCount; i++) {
            Label label = LABELS.get(i);
            Vec3 center = label.pos.getCenter();
            Vec3 toCamera = cameraPos.subtract(center);
            poseStack.pushPose();
            poseStack.translate(label.pos.getX() - cameraPos.x, label.pos.getY() - cameraPos.y, label.pos.getZ() - cameraPos.z);
            for (Direction face : Direction.values()) {
                if (face.getAxis() == label.facing.getAxis()) continue;
                // Skip faces turned away from the camera.
                if (toCamera.x * face.getStepX() + toCamera.y * face.getStepY() + toCamera.z * face.getStepZ() <= 0.5) continue;
                submitFace(label, face, label.faceLight[face.ordinal()], poseStack, collector);
            }
            poseStack.popPose();
        }
    }

    private static void submitFace(Label label, Direction face, int light, PoseStack poseStack, SubmitNodeCollector collector) {
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
        Vector3f target = face.getAxis().isVertical() ? label.facing.step() : new Vector3f(0, 1, 0);
        Vector3f up = rotation.transform(new Vector3f(0, 1, 0));
        Vector3f normal = rotation.transform(new Vector3f(0, 0, 1));
        float angle = (float) Math.atan2(new Vector3f(up).cross(target).dot(normal), up.dot(target));
        rotation.mul(Axis.ZP.rotation(angle));
        poseStack.mulPose(rotation);

        if (label.hasIcon) {
            poseStack.pushPose();
            poseStack.translate(0.0f, ICON_Y, 0.0f);
            poseStack.scale(label.iconScale, label.iconScale, label.iconScale);
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
