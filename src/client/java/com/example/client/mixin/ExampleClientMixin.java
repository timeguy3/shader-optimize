package com.example.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.irisshaders.iris.gl.shader.GlShader",
        "net.irisshaders.iris.Iris"
}, remap = false)
public class ExampleClientMixin {

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
        if (!P620State.enabled || src == null || !src.contains("#version")) {
            return src;
        }

        int versionIdx = src.indexOf("#version");
        int lineEndIdx = src.indexOf('\n', versionIdx);
        if (lineEndIdx != -1) {
            P620State.shadersPatched++;
            return src.substring(0, lineEndIdx + 1)
                    + P620_NV_PRAGMAS
                    + src.substring(lineEndIdx + 1);
        }

        return src;
    }

    // Resets counters whenever Iris reloads a shaderpack
    @Inject(method = "reload", at = @At("HEAD"), remap = false, require = 0)
    private static void onIrisReload(CallbackInfo ci) {
        P620State.resetCountersForReload();
    }

    // Runs every tick via Iris to record frametimes and check F8/F9 hotkeys
    @Inject(method = "handleKeybinds", at = @At("HEAD"), remap = false, require = 0)
    private static void onIrisTick(CallbackInfo ci) {
        P620State.onFrameTick();
    }
}
