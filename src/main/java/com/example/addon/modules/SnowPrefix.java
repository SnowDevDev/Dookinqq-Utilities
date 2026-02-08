package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class SnowPrefix extends Module {
    public SnowPrefix() {
        super(AddonTemplate.CATEGORY, "Snow Prefix", "if you turn ts off ur gay");
    }

    private final SettingGroup sgMain = settings.createGroup("Main Prefix");
    private final SettingGroup sgModule = settings.createGroup("Module Prefix");

    private final Setting<String> prefix = sgMain.add(new StringSetting.Builder()
        .name("prefix-text")
        .description("no matter what you type in except the default thing ill kill u")
        .defaultValue("Snow-Client")
        .onChanged(this::updateMainPrefix)
        .build()
    );

    private final Setting<SettingColor> mainColor = sgMain.add(new ColorSetting.Builder()
        .name("prefix-color")
        .description("Color of the main prefix")
        .defaultValue(new SettingColor(224, 255, 224))
        .onChanged(this::updateMainPrefix)
        .build()
    );

    private final Setting<SettingColor> moduleColor = sgModule.add(new ColorSetting.Builder()
        .name("module-color")
        .description("Color of module/class name prefixes (e.g., [AutoTotem], [Flight])")
        .defaultValue(new SettingColor(224, 256, 224))
        .onChanged(this::updateModuleColor)
        .build()
    );

@Override
public void onActivate() {
    super.onActivate();
    applyPrefix();
}

@Override
public void onDeactivate() {
    if (!isActive()) toggle();
}

private void updateMainPrefix(String newValue) {
    applyPrefix();
}

private void updateMainPrefix(SettingColor newValue) {
    applyPrefix();
}

private void updateModuleColor(SettingColor newValue) {
    applyPrefix();
}

private void applyPrefix() {
    String text = prefix.get();
    SettingColor main = mainColor.get();
    SettingColor module = moduleColor.get();
}
}
