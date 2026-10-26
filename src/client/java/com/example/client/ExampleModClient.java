package com.example.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.shader.GlShader", remap = false)
public class ExampleClientMixin {

    // Injected after Iris's glsl-transformer finishes so the directives are never stripped
    @Unique
    private static final String P620_NV_PRAGMAS =
            "\n#pragma optionNV(fastmath on)\n" +
            "#pragma optionNV(fastprecision on)\n" +
            "#pragma optionNV(ifcvt none)\n";

    @ModifyVariable(
            method = "createShader",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1,
            remap = false,
            require = 0
    )
    private static String injectQuadroPascalOptimizations(String src) {
        if (src == null || !src.contains("#version")) {
            return src;
        }

        int versionIdx = src.indexOf("#version");
        int lineEndIdx = src.indexOf('\n', versionIdx);
        if (lineEndIdx != -1) {
            return src.substring(0, lineEndIdx + 1)
                    + P620_NV_PRAGMAS
                    + src.substring(lineEndIdx + 1);
        }

        return src;
    }
}
