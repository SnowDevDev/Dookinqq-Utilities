package com.example.addon.themes.snow;

import com.example.addon.themes.DookinqqGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.MathHelper;

public class WSnowCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;

    public WSnowCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1.0 : 0.0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!(theme instanceof DookinqqGuiTheme snowTheme)) {
            renderBackground(renderer, this, pressed, mouseOver);
            return;
        }

        animProgress += (checked ? 1.0 : -1.0) * delta * 14.0;
        animProgress = MathHelper.clamp(animProgress, 0.0, 1.0);

        renderBackground(renderer, this, pressed, mouseOver);

        double shellW = Math.max(0.0, width - 2.0);
        double shellH = Math.max(0.0, height - 2.0);

        if (shellW > 0.0 && shellH > 0.0) {
            drawGradientQuad(renderer, snowTheme, x + 1.0, y + 1.0, shellW, shellH, 0.18);
            renderer.quad(x + 1.0, y + height - 2.0, shellW, 1.0, new SettingColor(0, 0, 0, 32));
        }

        if (animProgress > 0.0) {
            double cs = (width - theme.scale(2.0)) / 1.75 * animProgress;
            double cx = x + (width - cs) / 2.0;
            double cy = y + (height - cs) / 2.0;

            drawGradientQuad(renderer, snowTheme, cx, cy, cs, cs, 0.95 * animProgress);

            double glowOffset = Math.max(0.2, snowTheme.glowRadius.get() * 0.18);
            SettingColor glow = snowTheme.fluidGradientColor(
                0.5, 0.2, 0.15, 0.42 * animProgress * snowTheme.glowAlpha.get()
            );
            renderer.quad(cx - glowOffset, cy - glowOffset, cs + (glowOffset * 2.0), cs + (glowOffset * 2.0), glow);
        }
    }

    private static void drawGradientQuad(
        GuiRenderer renderer,
        DookinqqGuiTheme snowTheme,
        double x,
        double y,
        double w,
        double h,
        double alphaScale
    ) {
        if (w <= 0.0 || h <= 0.0) return;

        int stepsX = 4;
        int stepsY = 4;
        double stepW = w / stepsX;
        double stepH = h / stepsY;

        for (int yi = 0; yi < stepsY; yi++) {
            double yNorm = stepsY == 1 ? 0.0 : (yi / (double) (stepsY - 1));
            for (int xi = 0; xi < stepsX; xi++) {
                double xNorm = stepsX == 1 ? 0.0 : (xi / (double) (stepsX - 1));
                SettingColor c = snowTheme.fluidGradientColor(xNorm, yNorm, 0.12, alphaScale);
                renderer.quad(x + (xi * stepW), y + (yi * stepH), stepW, stepH, c);
            }
        }
    }
}