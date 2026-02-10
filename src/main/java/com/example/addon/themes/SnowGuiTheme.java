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
            accentColor.set(new SettingColor(new Color(192, 32, 32, 192)));
            placeholderColor.set(new SettingColor(new Color(192, 32, 32, 255)));
            moduleBackground.set(new SettingColor(new Color(72, 12, 12, 108)));
            backgroundColor.get().set(new SettingColor(new Color(48, 8, 48, 96)));
        }
    }
}
