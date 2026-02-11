package com.example.addon.themes.snow;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WSnowVerticalList extends WVerticalList {

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        super.onRender(renderer, mouseX, mouseY, delta);

        if (cells.isEmpty()) return;

        double top = Double.MAX_VALUE;
        double bottom = Double.MIN_VALUE;

        for (Cell<?> cell : cells) {
            top = Math.min(top, cell.y);
            bottom = Math.max(bottom, cell.y + cell.height);
        }

        double left = x;
        double right = x + width;

        MeteorGuiTheme theme = (MeteorGuiTheme) this.theme;
Color accent = theme.accentColor.get();
        Color glow = new Color(accent.r, accent.g, accent.b, 40);

        // Glow layer
        renderer.quad(left - 1, top - 1, right - left + 2, bottom - top + 2, glow);

        double line = 1;

        // Top
        renderer.quad(left, top, right - left, line, accent);

        // Bottom
        renderer.quad(left, bottom - line, right - left, line, accent);

        // Left
        renderer.quad(left, top, line, bottom - top, accent);

        // Right
        renderer.quad(right - line, top, line, bottom - top, accent);
    }
}