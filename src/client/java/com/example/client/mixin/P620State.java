package com.example.client.mixin;

import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Method;

public final class P620State {
    public static volatile boolean enabled = true;
    public static volatile int shadersPatched = 0;
    public static volatile int buffersCompressed = 0;
    public static volatile long vramBytesSaved = 0;

    // Live Frametime & FPS Benchmark Tracking (ON vs OFF)
    private static long lastFrameNano = System.nanoTime();
    public static double currentFrameMs = 16.6;
    public static double onTotalMs = 0;
    public static long onFrameCount = 0;
    public static double offTotalMs = 0;
    public static long offFrameCount = 0;

    private static boolean f8WasDown = false;
    private static boolean f9WasDown = false;

    public static void resetCountersForReload() {
        shadersPatched = 0;
        buffersCompressed = 0;
        vramBytesSaved = 0;
    }

    public static void onFrameTick() {
        long now = System.nanoTime();
        double ms = (now - lastFrameNano) / 1_000_000.0;
        lastFrameNano = now;

        // Filter out pause/loading spikes over 200ms
        if (ms > 0.5 && ms < 200.0) {
            currentFrameMs = (currentFrameMs * 0.85) + (ms * 0.15);
            if (enabled) {
                onTotalMs += ms;
                onFrameCount++;
            } else {
                offTotalMs += ms;
                offFrameCount++;
            }
        }

        // Poll F8 (Toggle ON/OFF) and F9 (Print Status) via GLFW
        long window = GLFW.glfwGetCurrentContext();
        if (window != 0L) {
            boolean f8Down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F8) == GLFW.GLFW_PRESS;
            if (f8Down && !f8WasDown) {
                toggleMod();
            }
            f8WasDown = f8Down;

            boolean f9Down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F9) == GLFW.GLFW_PRESS;
            if (f9Down && !f9WasDown) {
                printStatusReport();
            }
            f9WasDown = f9Down;
        }
    }

    public static boolean handleChatCommand(String rawCommand) {
        if (rawCommand == null) return false;
        String cmd = rawCommand.startsWith("/") ? rawCommand.substring(1).trim() : rawCommand.trim();
        String lower = cmd.toLowerCase();

        if (!lower.equals("p620") && !lower.startsWith("p620 ")) {
            return false;
        }

        String arg = lower.length() > 4 ? lower.substring(4).trim() : "status";
        switch (arg) {
            case "toggle" -> toggleMod();
            case "on" -> {
                if (!enabled) toggleMod();
                else printStatusReport();
            }
            case "off" -> {
                if (enabled) toggleMod();
                else printStatusReport();
            }
            case "reset" -> {
                onTotalMs = 0;
                onFrameCount = 0;
                offTotalMs = 0;
                offFrameCount = 0;
                sendChatMessage("\u00A7b[P620]\u00A7r Benchmark averages reset.");
            }
            default -> printStatusReport();
        }
        return true;
    }

    public static void toggleMod() {
        enabled = !enabled;
        resetCountersForReload();
        reloadIrisShaders();
        String stateStr = enabled ? "\u00A7aENABLED" : "\u00A7cDISABLED (Stock Shader)";
        sendChatMessage("\u00A7b[P620 Optimizer]\u00A7r Switched to " + stateStr
                + "\u00A7r \u00A77(Patched " + shadersPatched + " shaders, "
                + buffersCompressed + " G-buffers)");
    }

    public static void printStatusReport() {
        double curFps = 1000.0 / Math.max(1.0, currentFrameMs);
        double vramMb = vramBytesSaved / (1024.0 * 1024.0);
        // At 60Hz, each compressed deferred buffer avoids a read+write pass per frame
        double bandwidthGbSec = (vramBytesSaved * 2.0 * 60.0) / (1024.0 * 1024.0 * 1024.0);

        double onAvgMs = onFrameCount > 0 ? (onTotalMs / onFrameCount) : currentFrameMs;
        double onAvgFps = 1000.0 / Math.max(1.0, onAvgMs);
        String comparison;
        if (offFrameCount > 30 && onFrameCount > 30) {
            double offAvgMs = offTotalMs / offFrameCount;
            double offAvgFps = 1000.0 / Math.max(1.0, offAvgMs);
            double pctGain = ((onAvgFps - offAvgFps) / offAvgFps) * 100.0;
            String sign = pctGain >= 0 ? "+" : "";
            comparison = String.format("ON Avg: \u00A7a%.1f FPS (%.1fms)\u00A7r vs OFF Avg: \u00A7c%.1f FPS (%.1fms)\u00A7r [\u00A7e%s%.1f%%\u00A7r]",
                    onAvgFps, onAvgMs, offAvgFps, offAvgMs, sign, pctGain);
        } else {
            comparison = String.format("ON Avg: \u00A7a%.1f FPS (%.1fms)\u00A7r \u00A77(Run '/p620 toggle' briefly to record OFF baseline)",
                    onAvgFps, onAvgMs);
        }

        String stateStr = enabled ? "\u00A7aACTIVE" : "\u00A7cOFF";
        sendChatMessage("\u00A7b=== Quadro P620 Hardware Optimizer [" + stateStr + "\u00A7b] ===");
        sendChatMessage(String.format("\u00A77Hooks:\u00A7r \u00A7f%d\u00A7r GLSL Shaders Patched | \u00A7f%d\u00A7r G-Buffers Compressed",
                shadersPatched, buffersCompressed));
        sendChatMessage(String.format("\u00A77Saved:\u00A7r \u00A7a%.1f MB\u00A7r G-Buffer VRAM | \u00A7a~%.2f GB/s\u00A7r Bus Bandwidth @ 60Hz",
                vramMb, bandwidthGbSec));
        sendChatMessage(String.format("\u00A77Live:\u00A7r \u00A7f%.1f FPS (%.1fms)\u00A7r | %s",
                curFps, currentFrameMs, comparison));
    }

    private static void reloadIrisShaders() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            Method reloadMethod = irisClass.getMethod("reload");
            reloadMethod.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    // Version-independent chat printer (Supports both 1.20/1.21 Intermediary and 26.1+ Unobfuscated)
    public static void sendChatMessage(String message) {
        System.out.println("[P620-Optimizer] " + message.replaceAll("\u00A7[0-9a-fk-or]", ""));
        try {
            // Try 26.1+ Unobfuscated Mojang Mappings first
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object gui = mcClass.getField("gui").get(mc);
            Object chat = gui.getClass().getMethod("getChat").invoke(gui);
            Class<?> compClass = Class.forName("net.minecraft.network.chat.Component");
            Object textObj = compClass.getMethod("literal", String.class).invoke(null, message);
            chat.getClass().getMethod("addMessage", compClass).invoke(chat, textObj);
            return;
        } catch (Throwable ignored) {
        }

        try {
            // Fallback to 1.20 - 1.21.x Fabric Intermediary Mappings
            Class<?> mcClass = Class.forName("net.minecraft.class_310");
            Object mc = mcClass.getMethod("method_1551").invoke(null);
            Object inGameHud = mcClass.getField("field_1705").get(mc);
            Object chatHud = inGameHud.getClass().getMethod("method_1743").invoke(inGameHud);
            Class<?> textClass = Class.forName("net.minecraft.class_2561");
            Object textObj = textClass.getMethod("method_43470", String.class).invoke(null, message);
            chatHud.getClass().getMethod("method_1812", textClass).invoke(chatHud, textObj);
        } catch (Throwable ignored) {
        }
    }
}
