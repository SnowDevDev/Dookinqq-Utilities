package com.example.addon.themes.snow;

import com.example.addon.themes.DookinqqGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class WSnowSlider extends WSlider implements MeteorWidget {

    public WSnowSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!(theme instanceof DookinqqGuiTheme snowTheme)) return;

        double valueWidth = valueWidth();
        double s = snowTheme.scale(3);
        double handle = handleSize();
        double barX = this.x + handle / 2;
        double barY = this.y + height / 2 - s / 2;

        renderLeftFluid(renderer, snowTheme, barX, barY, valueWidth, s);
        renderer.quad(barX + valueWidth, barY, width - valueWidth - handle, s, snowTheme.sliderRight.get());
        renderHandleFluid(renderer, snowTheme, this.x + valueWidth, this.y, handle, handle);
    }

    private void renderLeftFluid(GuiRenderer renderer, DookinqqGuiTheme snowTheme, double x, double y, double w, double h) {
        if (w <= 0 || h <= 0) return;
        drawFluidGradient(renderer, snowTheme, x, y, w, h, 0.95, 16, 3, 0.14);
    }

    private void renderHandleFluid(GuiRenderer renderer, DookinqqGuiTheme snowTheme, double x, double y, double w, double h) {
        if (w <= 0 || h <= 0) return;
        drawFluidGradient(renderer, snowTheme, x, y, w, h, 1.0, 6, 6, 0.25);
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
            double yNorm = stepsY == 1 ? 0.0 : yi / (double) (stepsY - 1);
            for (int xi = 0; xi < stepsX; xi++) {
                double xNorm = stepsX == 1 ? 0.0 : xi / (double) (stepsX - 1);
                SettingColor c = snowTheme.fluidGradientColor(xNorm, yNorm, phaseOffset, alphaScale);
                renderer.quad(x + (xi * stepW), y + (yi * stepH), stepW, stepH, c);
            }
        }
    }
}