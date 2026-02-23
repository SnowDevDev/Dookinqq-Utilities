package com.example.addon.mixin;

import com.example.addon.hud.SnowArrayList;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudAnimationMixin {
    @Unique
    private int dookinqq$currentLine;

    @Unique
    private int dookinqq$currentTick;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
    private void dookinqq$onRenderHead(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean hidden, CallbackInfo ci) {
        this.dookinqq$currentLine = 0;
        this.dookinqq$currentTick = currentTick;
    }

    @ModifyArg(
        method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
        ),
        index = 2,
        require = 0
    )
    private int dookinqq$animateChatX(int x) {
        SnowArrayList.AnimationProfile profile = SnowArrayList.getAnimationProfile();
        if (!profile.syncHudChatAnimations) return x;

        double speed = clamp(profile.globalAnimationSpeed, 0.1, 4.0);
        double phase = (dookinqq$currentTick * 0.11 * speed) + (dookinqq$currentLine * 0.7);
        double amplitude = profile.chatSlideDistance / (1.0 + (dookinqq$currentLine * 0.18));
        int offset = (int) Math.round(Math.sin(phase) * amplitude);

        return x + offset;
    }

    @ModifyArg(
        method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
        ),
        index = 4,
        require = 0
    )
    private int dookinqq$animateChatColor(int color) {
        SnowArrayList.AnimationProfile profile = SnowArrayList.getAnimationProfile();
        if (!profile.syncHudChatAnimations) {
            dookinqq$currentLine++;
            return color;
        }

        double speed = clamp(profile.globalAnimationSpeed, 0.1, 4.0);
        double pulse = 1.0 - clamp(profile.chatPulseAlpha, 0.0, 1.0)
            + clamp(profile.chatPulseAlpha, 0.0, 1.0) * (0.5 + 0.5 * Math.sin((dookinqq$currentTick * 0.22 * speed) + dookinqq$currentLine));

        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) alpha = 255;
        alpha = clamp255((int) Math.round(alpha * pulse));

        SettingColor synced = profile.sampleColor((dookinqq$currentLine * 0.2) + (dookinqq$currentTick * 0.012 * speed), 1.0);
        int rgb = (synced.r << 16) | (synced.g << 8) | synced.b;

        dookinqq$currentLine++;
        return (alpha << 24) | rgb;
    }

    @Unique
    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Unique
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
