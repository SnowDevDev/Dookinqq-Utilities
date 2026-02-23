package com.example.addon.utils;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class RenderUtil {
    // Draw a filled rounded rectangle using HudRenderer.quad primitives.
    // radius in pixels; quality is 1px per unit (fast enough for HUD)
    public static void renderRoundedQuad(HudRenderer renderer, double x, double y, double width, double height, int radius, SettingColor color) {
        if (radius <= 0) {
            renderer.quad(x, y, width, height, color);
            return;
        }

        // center horizontal strip
        renderer.quad(x + radius, y, width - radius * 2, height, color);
        // left and right vertical strips
        renderer.quad(x, y + radius, radius, height - radius * 2, color);
        renderer.quad(x + width - radius, y + radius, radius, height - radius * 2, color);

        int r = radius;
        // corner pixel fill (quarter-circles)
        for (int cx = 0; cx < r; cx++) {
            for (int cy = 0; cy < r; cy++) {
                double dx = r - cx - 0.5;
                double dy = r - cy - 0.5;
                if (dx * dx + dy * dy <= r * r) {
                    // top-left
                    renderer.quad(x + cx, y + cy, 1, 1, color);
                    // top-right
                    renderer.quad(x + width - cx - 1, y + cy, 1, 1, color);
                    // bottom-left
                    renderer.quad(x + cx, y + height - cy - 1, 1, 1, color);
                    // bottom-right
                    renderer.quad(x + width - cx - 1, y + height - cy - 1, 1, 1, color);
                }
            }
        }
    }

    // Draw an outline by drawing an outer rounded rect in borderColor and an inner rounded rect inset by thickness in innerColor
    public static void renderRoundedOutline(HudRenderer renderer, double x, double y, double width, double height, int radius, double thickness, SettingColor borderColor, SettingColor innerColor) {
        if (thickness <= 0) {
            return;
        }
        renderRoundedQuad(renderer, x, y, width, height, radius, borderColor);
        double inset = thickness;
        int innerRadius = Math.max(0, radius - (int)Math.round(inset));
        renderRoundedQuad(renderer, x + inset, y + inset, Math.max(0, width - inset * 2), Math.max(0, height - inset * 2), innerRadius, innerColor);
    }
}
