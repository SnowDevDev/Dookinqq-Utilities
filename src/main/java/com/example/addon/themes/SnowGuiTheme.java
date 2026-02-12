package com.example.addon.themes;

import com.example.addon.themes.snow.WSnowModule;
import com.example.addon.themes.snow.WSnowWindow;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.awt.*;

public class SnowGuiTheme extends MeteorGuiTheme {

    @Override
    public WWidget module(Module module) {
        return w(new WSnowModule(module));
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WSnowWindow(icon, title));
    }

    public SnowGuiTheme() {
        settingsFactory = new DefaultSettingsWidgetFactory(this);

        if (placeholderColor.get().r != 33) {
            moduleAlignment.set(AlignmentX.Left);

            // Accent & backgrounds
            accentColor.set(new SettingColor(new Color(224, 56, 112, 192)));
            placeholderColor.set(new SettingColor(new Color(224, 56, 112, 255)));
            moduleBackground.set(new SettingColor(new Color(112, 24, 56, 108)));
            backgroundColor.get().set(new SettingColor(new Color(168, 36, 84, 96)));

            // Text colors
            textColor.set(new SettingColor(new Color(255, 255, 255, 255)));          // Main text
            textSecondaryColor.set(new SettingColor(new Color(192, 192, 192, 255))); // Secondary text
        }
    }
}