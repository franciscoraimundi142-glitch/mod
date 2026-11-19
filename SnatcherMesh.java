package com.horrormod.client;

import com.horrormod.HorrorMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Pre-skinned, pre-animated mesh baked from the .glb by tools/bake_glb.py.
 *
 * File layout (little endian):
 *   "SNCH", int vertexCount, int triCount, int frameCount, float fps,
 *   float min[3], float range[3],
 *   float uv[V*2], ushort indices[T*3],
 *   frameCount x { ushort pos[V*3] (quantised into min..min+range), byte normal[V*3] (/127) }
 */
public final class SnatcherMesh {
    private static final ResourceLocation FILE = new ResourceLocation(HorrorMod.MODID, "mesh/snatcher.bin");
    private static SnatcherMesh instance;

    public final int vertexCount;
    public final int triCount;
    public final int frameCount;
    public final float fps;

    private final float minX, minY, minZ, rngX, rngY, rngZ;
    private final float[] uv;
    private final int[] indices;
    private final short[][] framePos;
    private final byte[][] frameNormal;

    // scratch buffers (render thread only)
    private final float[] sPos;
    private final float[] sNor;

    public static synchronized SnatcherMesh get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static SnatcherMesh load() {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(FILE)) {
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            if (bb.get() != 'S' || bb.get() != 'N' || bb.get() != 'C' || bb.get() != 'H') {
                throw new IOException("Bad magic in " + FILE);
            }
            return new SnatcherMesh(bb);
        } catch (IOException e) {
            throw new RuntimeException("Could not load mesh " + FILE, e);
        }
    }

    private SnatcherMesh(ByteBuffer bb) {
        this.vertexCount = bb.getInt();
        this.triCount = bb.getInt();
        this.frameCount = bb.getInt();
        this.fps = bb.getFloat();
        this.minX = bb.getFloat();
        this.minY = bb.getFloat();
        this.minZ = bb.getFloat();
        this.rngX = bb.getFloat();
        this.rngY = bb.getFloat();
        this.rngZ = bb.getFloat();

        this.uv = new float[vertexCount * 2];
        for (int i = 0; i < uv.length; i++) {
            uv[i] = bb.getFloat();
        }
        this.indices = new int[triCount * 3];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = bb.getShort() & 0xFFFF;
        }
        this.framePos = new short[frameCount][vertexCount * 3];
        this.frameNormal = new byte[frameCount][vertexCount * 3];
        for (int f = 0; f < frameCount; f++) {
            short[] p = framePos[f];
            for (int i = 0; i < p.length; i++) {
                p[i] = bb.getShort();
            }
            bb.get(frameNormal[f]);
        }
        this.sPos = new float[vertexCount * 3];
        this.sNor = new float[vertexCount * 3];
    }

    /**
     * @param frame fractional frame index, wraps around (animation loops)
     */
    public void render(PoseStack.Pose pose, VertexConsumer vc, float frame, int light, int overlay) {
        float ff = frame % frameCount;
        if (ff < 0) ff += frameCount;
        int f0 = (int) ff;
        if (f0 >= frameCount) f0 = frameCount - 1;
        int f1 = (f0 + 1) % frameCount;
        float t = ff - f0;

        short[] a = framePos[f0];
        short[] b = framePos[f1];
        byte[] na = frameNormal[f0];
        byte[] nb = frameNormal[f1];
        float kx = rngX / 65535.0F, ky = rngY / 65535.0F, kz = rngZ / 65535.0F;

        // interpolate every vertex once between the two baked frames
        for (int v = 0; v < vertexCount; v++) {
            int o = v * 3;
            int ax = a[o] & 0xFFFF, ay = a[o + 1] & 0xFFFF, az = a[o + 2] & 0xFFFF;
            int bx = b[o] & 0xFFFF, by = b[o + 1] & 0xFFFF, bz = b[o + 2] & 0xFFFF;
            sPos[o] = minX + (ax + (bx - ax) * t) * kx;
            sPos[o + 1] = minY + (ay + (by - ay) * t) * ky;
            sPos[o + 2] = minZ + (az + (bz - az) * t) * kz;
            sNor[o] = (na[o] + (nb[o] - na[o]) * t) / 127.0F;
            sNor[o + 1] = (na[o + 1] + (nb[o + 1] - na[o + 1]) * t) / 127.0F;
            sNor[o + 2] = (na[o + 2] + (nb[o + 2] - na[o + 2]) * t) / 127.0F;
        }

        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        // Entity render types draw QUADS, so every triangle is emitted as a degenerate quad (v0, v1, v2, v2).
        for (int i = 0; i < indices.length; i += 3) {
            put(vc, m, n, indices[i], light, overlay);
            put(vc, m, n, indices[i + 1], light, overlay);
            put(vc, m, n, indices[i + 2], light, overlay);
            put(vc, m, n, indices[i + 2], light, overlay);
        }
    }

    private void put(VertexConsumer vc, Matrix4f m, Matrix3f n, int v, int light, int overlay) {
        int o = v * 3;
        vc.vertex(m, sPos[o], sPos[o + 1], sPos[o + 2])
                .color(255, 255, 255, 255)
                .uv(uv[v * 2], uv[v * 2 + 1])
                .overlayCoords(overlay)
                .uv2(light)
                .normal(n, sNor[o], sNor[o + 1], sNor[o + 2])
                .endVertex();
    }
}
