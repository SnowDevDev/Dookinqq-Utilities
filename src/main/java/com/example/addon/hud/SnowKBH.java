package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import me.x150.renderer.render.Renderer2d;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SnowKBH extends HudElement {
    public static final HudElementInfo<SnowKBH> INFO = new HudElementInfo<>(AddonTemplate.HUD_GROUP, "Snow KBH", "Render keybind list", SnowKBH::new);

    public SnowKBH(){
        super(INFO);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder()
        .name("Background")
        .defaultValue(new SettingColor(18, 18, 18))
        .build()
    );
    private final Setting<SettingColor> border = sgGeneral.add(new ColorSetting.Builder()
        .name("Border")
        .defaultValue(new SettingColor(255, 105, 180))
        .build()
    );
    private final Setting<SettingColor> text = sgGeneral.add(new ColorSetting.Builder()
        .name("Text")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private List<Module> getKeyBindedModules(){
        List<Module> part = new ArrayList<>();
        for (Module m : Modules.get().getAll()) {
            if (m.keybind.isSet()) part.add(m);
        }
        return part;
    }

    private Module longestModuleName(){
        Module module = getKeyBindedModules().getFirst();

        for (Module mod : getKeyBindedModules()) {
            if (mod.name.length() > module.name.length()) {
                module = mod;
            }
        }

        return module;
    }


    @Override
    public void render(HudRenderer renderer) {
        if (getKeyBindedModules().isEmpty()) return;
        int sx = x + 5;
        int sy = y + 5;
        final int space = 25;
        double width = renderer.textWidth(longestModuleName().keybind.toString() + "   ") + renderer.textWidth(longestModuleName().title);
        for (Module m : getKeyBindedModules()) {
            renderer.text("[" + m.keybind.toString() + "] — " + m.title, sx, sy, text.get(), false);
            sy += space;
        }

        renderer.quad(x - 5,y - 5, width * 1.2, sy - y + 10,background.get());
Renderer2d.renderRoundedOutline(
    renderer.drawContext.getMatrices(),
    new java.awt.Color(
        border.get().r,
        border.get().g,
        border.get().b,
        140
    ),
    x - 5,
    y - 5,
    (x - 5) + width * 1.2,
    (y - 5) + (sy - y) + 10,
    3f,
    3f,
    3f
);

        setSize(40, 40);
    }
}
