package com.maskedfish.infinitexray;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws see-through highlight boxes (translucent faces + bright edges) around
 * every matching block. Depth testing is disabled so the boxes are visible
 * through terrain. Draws in immediate mode (Tesselator + BufferUploader),
 * which avoids the shared-buffer pitfalls of MultiBufferSource.
 */
public final class HighlightRenderer {

    private static final int FACE_ALPHA = 0x4C; // 30%
    private static final int EDGE_ALPHA = 0xF2; // 95%

    /** Hard cap so an absurd number of matches can never tank the framerate. */
    private static final int MAX_DRAWN = 4096;

    private static boolean loggedError = false;
    private static long lastDiagLog = 0;

    private HighlightRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        if (!InfiniteXRayState.isEnabled() || InfiniteXRayState.getSelected() == null) {
            return;
        }
        try {
            doRender(event);
        } catch (Exception e) {
            if (!loggedError) {
                loggedError = true;
                InfiniteXRayMod.LOGGER.error("InfiniteXRay render error", e);
            }
        }
    }

    private static void doRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        List<BlockPos> positions = InfiniteXRayState.getPositions();
        if (positions.isEmpty()) {
            return;
        }

        int rgb = InfiniteXRayState.getHighlightColor();
        int faceColor = (FACE_ALPHA << 24) | rgb;
        int edgeColor = (EDGE_ALPHA << 24) | lighten(rgb);

        Camera camera = event.getCamera();
        Frustum frustum = event.getFrustum();
        Vec3 camPos = camera.getPosition();
        double maxDistSq = (double) InfiniteXRayState.getScanRadius() * InfiniteXRayState.getScanRadius();

        // World -> camera transform: the event's model-view matrix is the
        // camera rotation only; the camera position offset must be added.
        Matrix4f modelView = new Matrix4f(event.getModelViewMatrix());
        modelView.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        int drawn = 0;

        // Pass 1: translucent faces (only in filled mode).
        if (InfiniteXRayState.getRenderMode() == InfiniteXRayState.RenderMode.FILLED) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder faces = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (BlockPos pos : positions) {
                if (drawn >= MAX_DRAWN) {
                    break;
                }
                if (!isVisible(pos, camPos, maxDistSq, frustum)) {
                    continue;
                }
                drawFaces(faces, modelView, pos.getX(), pos.getY(), pos.getZ(), faceColor);
                drawn++;
            }
            MeshData faceMesh = faces.build();
            if (faceMesh != null) {
                BufferUploader.drawWithShader(faceMesh);
            }
        }

        // Pass 2: bright edges.
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(2.0F);
        BufferBuilder edges = Tesselator.getInstance().begin(
                VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        int edgeDrawn = 0;
        for (BlockPos pos : positions) {
            if (edgeDrawn >= MAX_DRAWN) {
                break;
            }
            if (!isVisible(pos, camPos, maxDistSq, frustum)) {
                continue;
            }
            drawEdges(edges, modelView, pos.getX(), pos.getY(), pos.getZ(), edgeColor);
            edgeDrawn++;
        }
        MeshData edgeMesh = edges.build();
        if (edgeMesh != null) {
            BufferUploader.drawWithShader(edgeMesh);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // Throttled diagnostics so issues are easy to find in the log.
        long now = System.currentTimeMillis();
        if (now - lastDiagLog > 10000) {
            lastDiagLog = now;
            InfiniteXRayMod.LOGGER.info("InfiniteXRay: drawing {} highlight boxes ({} known positions)",
                    drawn, positions.size());
        }
    }

    private static boolean isVisible(BlockPos pos, Vec3 camPos, double maxDistSq, Frustum frustum) {
        double dx = pos.getX() + 0.5 - camPos.x;
        double dy = pos.getY() + 0.5 - camPos.y;
        double dz = pos.getZ() + 0.5 - camPos.z;
        if (dx * dx + dy * dy + dz * dz > maxDistSq) {
            return false;
        }
        return frustum == null || frustum.isVisible(new AABB(pos));
    }

    private static void drawFaces(BufferBuilder builder, Matrix4f mat, int x, int y, int z, int color) {
        float x0 = x;
        float y0 = y;
        float z0 = z;
        float x1 = x + 1.0f;
        float y1 = y + 1.0f;
        float z1 = z + 1.0f;

        // Six faces (culling is off, so winding order does not matter).
        quad(builder, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color);
        quad(builder, mat, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, color);
        quad(builder, mat, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, color);
        quad(builder, mat, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color);
        quad(builder, mat, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color);
        quad(builder, mat, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, color);
    }

    private static void drawEdges(BufferBuilder builder, Matrix4f mat, int x, int y, int z, int color) {
        float x0 = x;
        float y0 = y;
        float z0 = z;
        float x1 = x + 1.0f;
        float y1 = y + 1.0f;
        float z1 = z + 1.0f;

        line(builder, mat, x0, y0, z0, x1, y0, z0, 1, 0, 0, color);
        line(builder, mat, x0, y0, z1, x1, y0, z1, 1, 0, 0, color);
        line(builder, mat, x0, y1, z0, x1, y1, z0, 1, 0, 0, color);
        line(builder, mat, x0, y1, z1, x1, y1, z1, 1, 0, 0, color);
        line(builder, mat, x0, y0, z0, x0, y1, z0, 0, 1, 0, color);
        line(builder, mat, x1, y0, z0, x1, y1, z0, 0, 1, 0, color);
        line(builder, mat, x0, y0, z1, x0, y1, z1, 0, 1, 0, color);
        line(builder, mat, x1, y0, z1, x1, y1, z1, 0, 1, 0, color);
        line(builder, mat, x0, y0, z0, x0, y0, z1, 0, 0, 1, color);
        line(builder, mat, x1, y0, z0, x1, y0, z1, 0, 0, 1, color);
        line(builder, mat, x0, y1, z0, x0, y1, z1, 0, 0, 1, color);
        line(builder, mat, x1, y1, z0, x1, y1, z1, 0, 0, 1, color);
    }

    private static void quad(BufferBuilder builder, Matrix4f mat,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz, int color) {
        builder.addVertex(mat, ax, ay, az).setColor(color);
        builder.addVertex(mat, bx, by, bz).setColor(color);
        builder.addVertex(mat, cx, cy, cz).setColor(color);
        builder.addVertex(mat, dx, dy, dz).setColor(color);
    }

    private static void line(BufferBuilder builder, Matrix4f mat,
            float ax, float ay, float az, float bx, float by, float bz,
            float nx, float ny, float nz, int color) {
        builder.addVertex(mat, ax, ay, az).setColor(color).setNormal(nx, ny, nz);
        builder.addVertex(mat, bx, by, bz).setColor(color).setNormal(nx, ny, nz);
    }

    /** Blends the highlight color toward white so the edges stand out. */
    private static int lighten(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = r + (255 - r) * 60 / 100;
        g = g + (255 - g) * 60 / 100;
        b = b + (255 - b) * 60 / 100;
        return (r << 16) | (g << 8) | b;
    }
}
