package com.example.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.IrisRenderSystem", remap = false)
public class P620VramBandwidthMixin {

    @Unique
    private static void compressBufferArgs(Args args, int formatIdx, int widthIdx, int heightIdx) {
        if (!P620State.enabled) {
            return;
        }

        int internalFormat = args.get(formatIdx);
        int width = args.get(widthIdx);
        int height = args.get(heightIdx);
        long pixels = (long) width * (long) height;

        int newFormat = internalFormat;
        int bytesSavedPerPixel = 0;

        switch (internalFormat) {
            // GL_RGBA32F (16B/px) -> GL_RGBA16F (8B/px)
            case 0x8814 -> {
                newFormat = 0x881A;
                bytesSavedPerPixel = 8;
            }
            // GL_RGB32F (12B/px) -> GL_R11F_G11F_B10F (4B/px)
            case 0x8815 -> {
                newFormat = 0x8C3A;
                bytesSavedPerPixel = 8;
            }
            // GL_RGB16F (8B/px padded on GPU) -> GL_R11F_G11F_B10F (4B/px)
            case 0x881B -> {
                newFormat = 0x8C3A;
                bytesSavedPerPixel = 4;
            }
            // GL_RG32F (8B/px) -> GL_RG16F (4B/px)
            case 0x8230 -> {
                newFormat = 0x822F;
                bytesSavedPerPixel = 4;
            }
            // GL_R32F (4B/px) -> GL_R16F (2B/px)
            case 0x822E -> {
                newFormat = 0x822D;
                bytesSavedPerPixel = 2;
            }
        }

        if (newFormat != internalFormat) {
            args.set(formatIdx, newFormat);
            P620State.buffersCompressed++;
            P620State.vramBytesSaved += (pixels * bytesSavedPerPixel);
        }
    }

    // DSA allocation: texImage2D(int texture, int target, int level, int internalformat, int width, int height, ...)
    @ModifyArgs(
            method = "texImage2D(IIIIIIIIILjava/nio/ByteBuffer;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL45C;glTextureImage2DEXT(IIIIIIIIILjava/nio/ByteBuffer;)V"),
            remap = false,
            require = 0
    )
    private static void onDsaTexImage2D(Args args) {
        compressBufferArgs(args, 3, 4, 5);
    }

    // Standard allocation: texImage2D(int target, int level, int internalformat, int width, int height, ...)
    @ModifyArgs(
            method = "texImage2D(IIIIIIIILjava/nio/ByteBuffer;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"),
            remap = false,
            require = 0
    )
    private static void onLegacyTexImage2D(Args args) {
        compressBufferArgs(args, 2, 3, 4);
    }
}
