package com.example.addon.themes.snow;

import com.example.addon.themes.DookinqqGuiTheme;
import net.minecraft.util.math.MathHelper;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WSnowModule extends WPressable implements MeteorWidget {
    private final Module module;
    private double titleWidth;
    private double animationProgress1;
    private double animationProgress2;
    private static Field keybindField;
    private static boolean keybindFieldInitialized;

    public WSnowModule(Module module) {
        this.module = module;
        this.tooltip = module.description;
        if (module.isActive()) {
            animationProgress1 = 1;
            animationProgress2 = 1;
        }
    }

    @Override
    public double pad() {
        return theme.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        String title = normalizeTitle(module.title);
        String bindText = getBindText();

        titleWidth = theme.textWidth(title);
        double bindWidth = bindText.isEmpty() ? 0 : theme.textWidth(bindText);
        double bindReserve = bindWidth > 0 ? bindWidth + theme.scale(12) : theme.scale(18);

        width = (pad + titleWidth + bindReserve + pad) * 1.28;
        height = (pad + theme.textHeight() + pad) * 1.32;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!(this.theme instanceof DookinqqGuiTheme snowTheme)) return;

        double pad = pad();
        String title = normalizeTitle(module.title);
        String bindText = getBindText();

        animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = MathHelper.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress2 = MathHelper.clamp(animationProgress2, 0, 1);

        if (animationProgress1 > 0) {
            drawFluidGradient(renderer, snowTheme, x, y, width * animationProgress1, height, 0.44, 16, 6, 0.12);
        }

        if (animationProgress2 > 0) {
            drawFluidGradient(renderer, snowTheme, x, y, theme.scale(2), height, 0.82, 4, 8, 0.22);
        }

        if (module.isActive() || mouseOver) {
            drawRowParticles(renderer, snowTheme, x + theme.scale(2), y + theme.scale(2), width - theme.scale(4), height - theme.scale(4));
        }

        double bindWidth = bindText.isEmpty() ? 0 : theme.textWidth(bindText);
        double rightReserve = bindWidth > 0 ? bindWidth + theme.scale(12) : theme.scale(8);

        double textX = this.x + pad + theme.scale(6);
        double availableTextW = Math.max(0.0, width - (pad * 2) - rightReserve);

        if (snowTheme.moduleAlignment.get() == AlignmentX.Center) textX += (availableTextW / 2.0) - (titleWidth / 2.0);
        else if (snowTheme.moduleAlignment.get() == AlignmentX.Right) textX += availableTextW - titleWidth;

        double textY = y + (height - theme.textHeight()) / 2;
        SettingColor enabledText = new SettingColor(255, 255, 255, 255);
        SettingColor disabledText = new SettingColor(224, 224, 224, 255);

        double glowBase = snowTheme.moduleGlowStrength.get() * snowTheme.glowAlpha.get();
        double glowRadius = Math.max(0.0, snowTheme.glowRadius.get());

        if (module.isActive()) {
            drawTextGlow(renderer, title, textX, textY, enabledText, glowBase * 4.0, glowRadius);
            renderer.text(title, textX + 0.75, textY + 0.75, new SettingColor(0, 0, 0, 145), false);
            renderer.text(title, textX, textY, enabledText, false);
        } else {
            drawTextGlow(renderer, title, textX, textY, disabledText, glowBase, glowRadius);
            renderer.text(title, textX + 0.75, textY + 0.75, new SettingColor(0, 0, 0, 130), false);
            renderer.text(title, textX, textY, disabledText, false);
        }

        if (!bindText.isEmpty()) {
            double bindX = x + width - pad - bindWidth - theme.scale(4);
            SettingColor bindColor = snowTheme.bindColor.get();
            double bindGlow = snowTheme.bindGlowStrength.get() * snowTheme.glowAlpha.get();

            if (bindGlow > 0.0) {
                int ga = clamp255((int) Math.round(bindColor.a * Math.min(1.8, bindGlow)));
                SettingColor gc = new SettingColor(bindColor.r, bindColor.g, bindColor.b, ga);
                double bOffset = Math.max(0.12, snowTheme.glowRadius.get() * 0.3);

                renderer.text(bindText, bindX - bOffset, textY, gc, false);
                renderer.text(bindText, bindX + bOffset, textY, gc, false);
                renderer.text(bindText, bindX, textY - bOffset, gc, false);
                renderer.text(bindText, bindX, textY + bOffset, gc, false);
            }

            renderer.text(bindText, bindX + 0.65, textY + 0.65, new SettingColor(0, 0, 0, 125), false);
            renderer.text(bindText, bindX, textY, bindColor, false);
        }
    }

    private void drawTextGlow(
        GuiRenderer renderer,
        String text,
        double x,
        double y,
        SettingColor textColor,
        double glowStrength,
        double radius
    ) {
        if (text == null || text.isEmpty() || glowStrength <= 0.0) return;

        int glowAlpha = clamp255((int) Math.round(textColor.a * Math.min(2.0, glowStrength)));
        if (glowAlpha <= 0) return;

        SettingColor glowColor = new SettingColor(textColor.r, textColor.g, textColor.b, glowAlpha);
        double offset = Math.max(0.15, radius * 0.35);

        renderer.text(text, x - offset, y, glowColor, false);
        renderer.text(text, x + offset, y, glowColor, false);
        renderer.text(text, x, y - offset, glowColor, false);
        renderer.text(text, x, y + offset, glowColor, false);

        if (glowStrength > 1.0) {
            double secondOffset = offset * 1.65;
            SettingColor outer = new SettingColor(textColor.r, textColor.g, textColor.b, clamp255((int) Math.round(glowAlpha * 0.55)));

            renderer.text(text, x - secondOffset, y, outer, false);
            renderer.text(text, x + secondOffset, y, outer, false);
            renderer.text(text, x, y - secondOffset, outer, false);
            renderer.text(text, x, y + secondOffset, outer, false);
        }
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private String getBindText() {
        Object keybind = getKeybindValue();
        if (keybind == null) return "";

        try {
            Method isSet = keybind.getClass().getMethod("isSet");
            Object result = isSet.invoke(keybind);
            if (result instanceof Boolean b && !b) return "";
        } catch (Exception ignored) {
        }

        String raw = keybind.toString();
        if (raw == null) return "";
        raw = raw.trim();
        if (raw.isEmpty() || raw.equalsIgnoreCase("none")) return "";

        return "[" + raw.toUpperCase(Locale.ROOT) + "]";
    }

    private Object getKeybindValue() {
        try {
            if (!keybindFieldInitialized) {
                keybindField = findKeybindField(module.getClass());
                keybindFieldInitialized = true;
            }
            if (keybindField == null) return null;

            Object keybindSetting = keybindField.get(module);
            if (keybindSetting == null) return null;

            try {
                Method get = keybindSetting.getClass().getMethod("get");
                return get.invoke(keybindSetting);
            } catch (Exception ignored) {
                return keybindSetting;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Field findKeybindField(Class<?> start) {
        Class<?> type = start;
        while (type != null) {
            try {
                Field f = type.getDeclaredField("keybind");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private String normalizeTitle(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s*-\\s*", " - ").replaceAll("\\s{2,}", " ").trim();
    }

    private static void drawRowParticles(
        GuiRenderer renderer,
        DookinqqGuiTheme snowTheme,
        double x,
        double y,
        double w,
        double h
    ) {
        if (w <= 0 || h <= 0) return;

        int count = Math.max(4, snowTheme.particleCount.get() / 3);
        double t = (System.currentTimeMillis() / 1000.0) * snowTheme.gradientSpeed.get();

        for (int i = 0; i < count; i++) {
            double seed = (i + 1) * 17.991;
            double px = x + fract(Math.sin(seed + t * 0.7) * 67453.11) * w;
            double py = y + fract(Math.cos(seed * 0.82 - t * 0.5) * 21563.77) * h;
            double pulse = 0.4 + (0.6 * Math.sin(seed + t * 3.2));
            SettingColor c = snowTheme.futuristicParticleColor(seed + t, 0.34 * pulse);
            renderer.quad(px, py, 1, 1, c);
        }
    }

    private static void drawFluidGradient(
        GuiRenderer renderer,
        DookinqqGuiTheme snowTheme,
        double x,
        double y,
        double w,
        double h,
        double alphaScale,
        int stepsX,
        int stepsY,
        double phaseOffset
    ) {
        if (w <= 0 || h <= 0 || stepsX <= 0 || stepsY <= 0) return;

        double stepW = w / stepsX;
        double stepH = h / stepsY;

        for (int yi = 0; yi < stepsY; yi++) {
            double yNorm = stepsY == 1 ? 0.0 : (yi / (double) (stepsY - 1));
            for (int xi = 0; xi < stepsX; xi++) {
                double xNorm = stepsX == 1 ? 0.0 : (xi / (double) (stepsX - 1));
                SettingColor c = snowTheme.fluidGradientColor(xNorm, yNorm, phaseOffset, alphaScale);
                renderer.quad(x + (xi * stepW), y + (yi * stepH), stepW, stepH, c);
            }
        }
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }
}