package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.chatUtils.ChatUtilsHelper;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class SnowPrefix extends Module {

    public SnowPrefix() {
        super(AddonTemplate.CATEGORY, "Snow Prefix", "Customize the [Meteor] chat prefix and module colors.");
    }

    private final SettingGroup sgMain = settings.createGroup("Main Prefix");
    private final SettingGroup sgModule = settings.createGroup("Module Prefix");

    private final Setting<String> prefix = sgMain.add(new StringSetting.Builder()
        .name("prefix-text")
        .description("The main prefix text (e.g., 'Meteor', 'MyAddon')")
        .defaultValue("Snow")
        .onChanged(value -> updateMainPrefix()) // value not needed, just update
        .build()
    );

    private final Setting<SettingColor> mainColor = sgMain.add(new ColorSetting.Builder()
        .name("prefix-color")
        .description("Color of the main prefix")
        .defaultValue(new SettingColor(192, 32, 32))
        .onChanged(value -> updateMainPrefix())
        .build()
    );

    private final Setting<SettingColor> moduleColor = sgModule.add(new ColorSetting.Builder()
        .name("module-color")
        .description("Color of module/class name prefixes (e.g., [AutoTotem], [Flight])")
        .defaultValue(new SettingColor(255, 255, 255))
        .onChanged(value -> updateModuleColor())
        .build()
    );

    @Override
    public void onActivate() {
        super.onActivate();
        updateMainPrefix();
        updateModuleColor();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        ChatUtilsHelper.resetToDefault();
    }

    private void updateMainPrefix() {
        if (isActive()) {
            ChatUtilsHelper.setCustomPrefix(prefix.get(), mainColor.get().getPacked());
        }
    }

    private void updateModuleColor() {
        if (isActive()) {
            ChatUtilsHelper.setCustomModulePrefixColor(moduleColor.get().getPacked());
        }
    }
}