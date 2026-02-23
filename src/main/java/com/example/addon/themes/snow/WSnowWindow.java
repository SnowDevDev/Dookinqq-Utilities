package com.example.addon.themes.snow;

import com.example.addon.themes.DookinqqGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class WSnowWindow extends WWindow implements MeteorWidget {
    public WSnowWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WSnowHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!(theme instanceof DookinqqGuiTheme snowTheme)) return;
        if (!(expanded || animProgress > 0)) return;

        double bx = x + 5;
        double by = y + header.height;
        double bw = width - 10;
        double bh = height - header.height;

        drawFluidGradient(renderer, snowTheme, bx, by, bw, bh, 0.36, 26, 16, 0.08);
        drawScanGrid(renderer, snowTheme, bx, by, bw, bh);
        drawParticles(renderer, snowTheme, bx, by, bw, bh, 0.26);

        SettingColor line = snowTheme.panelLineColor.get();
        renderer.quad(bx, by, bw, 1, withAlpha(line, 0.62));
        renderer.quad(bx, by + bh - 1, bw, 1, withAlpha(line, 0.24));
    }

    private static void drawScanGrid(
        GuiRenderer renderer,
        DookinqqGuiTheme snowTheme,
        double x,
        double y,
        double w,
        double h
    ) {
        if (w <= 0 || h <= 0) return;

        double t = System.currentTimeMillis() / 1000.0;
        double spacing = Math.max(6.0, snowTheme.scale(11));
        SettingColor line = snowTheme.panelLineColor.get();

        double xShift = ((t * 20.0) % spacing) - spacing;
        for (double gx = x + xShift; gx < x + w; gx += spacing) {
            renderer.quad(gx, y, 1, h, withAlpha(line, 0.08));
        }

        double yShift = ((t * 8.0) % spacing) - spacing;
        for (double gy = y + yShift; gy < y + h; gy += spacing) {
            renderer.quad(x, gy, w, 1, withAlpha(line, 0.06));
        }

        double scanY = y + (((Math.sin(t * 1.2) * 0.5) + 0.5) * Math.max(0.0, h - 2));
        renderer.quad(x, scanY, w, 2, withAlpha(line, 0.14));
    }

    private static void drawParticles(
        GuiRenderer renderer,
        DookinqqGuiTheme snowTheme,
        double x,
        double y,
        double w,
        double h,
        double alphaScale
    ) {
        int count = Math.max(0, snowTheme.particleCount.get());
        if (count == 0 || w <= 0 || h <= 0) return;

        double t = (System.currentTimeMillis() / 1000.0) * snowTheme.gradientSpeed.get();

        for (int i = 0; i < count; i++) {
            double seed = (i + 1) * 12.9898;
            double px = x + fract(Math.sin(seed + t * (0.23 + (i % 5) * 0.01)) * 43758.5453) * w;
            double py = y + fract(Math.cos(seed * 1.73 - t * (0.18 + (i % 7) * 0.01)) * 24634.6345) * h;
            double flicker = 0.45 + (0.55 * Math.sin((t * 3.0) + seed));
            SettingColor c = snowTheme.futuristicParticleColor(seed + t, alphaScale * flicker);
            double size = 1 + (i % 3 == 0 ? 1 : 0);
            renderer.quad(px, py, size, size, c);
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

    private static void drawOutline(
        GuiRenderer renderer,
        double x,
        double y,
        double w,
        double h,
        SettingColor c
    ) {
        if (w <= 0 || h <= 0) return;
        renderer.quad(x, y, w, 1, c);
        renderer.quad(x, y + h - 1, w, 1, c);
        renderer.quad(x, y, 1, h, c);
        renderer.quad(x + w - 1, y, 1, h, c);
    }

    private static void drawGlowOutline(
        GuiRenderer renderer,
        DookinqqGuiTheme snowTheme,
        double x,
        double y,
        double w,
        double h,
        SettingColor base
    ) {
        double alphaMul = Math.max(0.0, snowTheme.glowAlpha.get());
        drawOutline(renderer, x, y, w, h, withAlpha(base, 0.88 * alphaMul));

        int rings = Math.max(1, (int) Math.round(snowTheme.glowRadius.get() * 2.0));
        rings = Math.min(rings, 8);

        for (int i = 1; i <= rings; i++) {
            double ringAlpha = ((0.36 * alphaMul) / i) * ((rings + 1.0 - i) / (double) rings);
            drawOutline(renderer, x - i, y - i, w + (i * 2.0), h + (i * 2.0), withAlpha(base, ringAlpha));
        }
    }

    private static SettingColor withAlpha(SettingColor c, double alphaScale) {
        int a = (int) Math.max(0, Math.min(255, Math.round(c.a * Math.max(0.0, alphaScale))));
        return new SettingColor(c.r, c.g, c.b, a);
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }

    private class WSnowHeader extends WHeader {
        public WSnowHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (!(WSnowWindow.this.theme instanceof DookinqqGuiTheme snowTheme)) return;

            drawFluidGradient(renderer, snowTheme, x, y, width, height, 1.0, 24, 9, 0.22);
            drawParticles(renderer, snowTheme, x, y, width, height, 0.42);

            SettingColor line = snowTheme.panelLineColor.get();
            renderer.quad(x, y, width, 1, withAlpha(line, 0.62));
            renderer.quad(x, y + height - 1, width, 1, withAlpha(line, 0.5));
            drawGlowOutline(renderer, snowTheme, x, y, width, height, line);

            drawHeaderTextGlow(renderer, snowTheme);

            double t = System.currentTimeMillis() / 1000.0;
            double scanWidth = Math.max(theme.scale(20), width * 0.18);
            double scanX = x + (((Math.sin(t * 1.5) * 0.5) + 0.5) * Math.max(0.0, width - scanWidth));
            renderer.quad(scanX, y + height - 2, scanWidth, 1, withAlpha(line, 0.58));
        }

        private void drawHeaderTextGlow(GuiRenderer renderer, DookinqqGuiTheme snowTheme) {
            double glow = snowTheme.moduleGlowStrength.get() * snowTheme.glowAlpha.get();
            if (glow <= 0.0) return;

            double offset = Math.max(0.15, snowTheme.glowRadius.get() * 0.35);
            SettingColor glowColor = withAlpha(new SettingColor(255, 255, 255, 255), Math.min(1.0, glow * 0.9));

            for (Cell<?> cell : cells) {
                WWidget widget = cell.widget();
                if (!(widget instanceof WLabel label)) continue;

                String text = label.get();
                if (text == null || text.isEmpty()) continue;

                double tx = widget.x;
                double ty = widget.y;

                renderer.text(text, tx - offset, ty, glowColor, true);
                renderer.text(text, tx + offset, ty, glowColor, true);
                renderer.text(text, tx, ty - offset, glowColor, true);
                renderer.text(text, tx, ty + offset, glowColor, true);
            }
        }
    }
}