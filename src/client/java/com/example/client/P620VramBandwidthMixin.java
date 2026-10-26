package com.example.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.IrisRenderSystem", remap = false)
public class P620VramBandwidthMixin {

    @Unique
    private static int downpackFormatForP620(int internalFormat) {
        return switch (internalFormat) {
            // GL_RGBA32F (0x8814) -> GL_RGBA16F (0x881A): 50% less VRAM/bus bandwidth, keeps Alpha channel
            case 0x8814 -> 0x881A;

            // GL_RGB32F (0x8815) & GL_RGB16F (0x881B) -> GL_R11F_G11F_B10F (0x8C3A): 50-66% less bandwidth
            case 0x8815, 0x881B -> 0x8C3A;

            // GL_RG32F (0x8230) -> GL_RG16F (0x822F): 50% less bandwidth on 2-channel buffers
            case 0x8230 -> 0x822F;

            // GL_R32F (0x822E) -> GL_R16F (0x822D): 50% less bandwidth on single-channel AO buffers
            case 0x822E -> 0x822D;

            default -> internalFormat;
        };
    }

    @ModifyVariable(
            method = "texImage2D(IIIIIIIIILjava/nio/ByteBuffer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 3,
            remap = false,
            require = 0
    )
    private static int compressDsaRenderTarget(int internalFormat) {
        return downpackFormatForP620(internalFormat);
    }

    @ModifyVariable(
            method = "texImage2D(IIIIIIIILjava/nio/ByteBuffer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2,
            remap = false,
            require = 0
    )
    private static int compressLegacyRenderTarget(int internalFormat) {
        return downpackFormatForP620(internalFormat);
    }
}
