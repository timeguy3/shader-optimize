package com.example.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.minecraft.class_634", // 1.19.3 - 1.21.11 ClientPlayNetworkHandler
        "net.minecraft.client.multiplayer.ClientPacketListener" // 26.1+ Unobfuscated
}, remap = false)
public class P620CommandMixin {

    @Inject(
            method = {
                    "method_45730(Ljava/lang/String;)V",
                    "sendChatCommand(Ljava/lang/String;)V",
                    "sendCommand(Ljava/lang/String;)V"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void onSendClientCommand(String command, CallbackInfo ci) {
        if (P620State.handleChatCommand(command)) {
            ci.cancel();
        }
    }
}
