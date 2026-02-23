package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.ChatUtilsAccessor;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.Text;

public class SnowPrefix extends Module {
    private static final SettingColor DOOKINQQ_NAME_COLOR = new SettingColor(0, 128, 255, 255);
    private static final SettingColor DOOKINQQ_SEPARATOR_COLOR = new SettingColor(0, 64, 128, 255);

    private final SettingGroup sgSnowPrefix = settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.createGroup("Colors");

    private final Setting<String> seperatorLeft = sgSnowPrefix.add(new StringSetting.Builder()
        .name("separator-left")
        .description("Left separator of the prefix.")
        .defaultValue("[")
        .onChanged(value -> applyPrefix())
        .build()
    );

    private final Setting<String> snowPrefix = sgSnowPrefix.add(new StringSetting.Builder()
        .name("name")
        .description("Main prefix text.")
        .defaultValue("Dookinqq Utils")
        .onChanged(value -> applyPrefix())
        .build()
    );

    private final Setting<String> seperatorRight = sgSnowPrefix.add(new StringSetting.Builder()
        .name("separator-right")
        .description("Right separator of the prefix.")
        .defaultValue("]")
        .onChanged(value -> applyPrefix())
        .build()
    );

    private final Setting<SettingColor> color = sgColor.add(new ColorSetting.Builder()
        .name("name-color")
        .description("Color of the prefix text.")
        .defaultValue(new SettingColor(0, 128, 255, 255))
        .onChanged(value -> applyPrefix())
        .build()
    );

    private final Setting<SettingColor> seperatorColor = sgColor.add(new ColorSetting.Builder()
        .name("separator-color")
        .description("Color of the separators.")
        .defaultValue(new SettingColor(0, 64, 128, 255))
        .onChanged(value -> applyPrefix())
        .build()
    );

    public SnowPrefix() {
        super(AddonTemplate.CATEGORY, "Dookinqq Prefix", "Changes the Meteor chat prefix.");
    }

    @Override
    public void toggle() {
        // Keep this module permanently enabled; disabling is ignored.
        if (isActive()) {
            applyPrefix();
            return;
        }

        super.toggle();
    }

    @Override
    public void onActivate() {
        syncPalette();
        applyPrefix();
    }

    private void syncPalette() {
        // Mutate SettingColor values directly to avoid recursively triggering onChanged callbacks.
        SettingColor nameColor = color.get();
        nameColor.set(new SettingColor(DOOKINQQ_NAME_COLOR.r, DOOKINQQ_NAME_COLOR.g, DOOKINQQ_NAME_COLOR.b, nameColor.a));

        SettingColor bracketColor = seperatorColor.get();
        bracketColor.set(new SettingColor(DOOKINQQ_SEPARATOR_COLOR.r, DOOKINQQ_SEPARATOR_COLOR.g, DOOKINQQ_SEPARATOR_COLOR.b, bracketColor.a));
    }

    public void applyPrefix() {
        syncPalette();

        ChatUtilsAccessor.setPrefix(
            Text.literal(seperatorLeft.get())
                .withColor(seperatorColor.get().getPacked())
                .append(
                    Text.literal(snowPrefix.get())
                        .withColor(color.get().getPacked())
                        .append(
                            Text.literal(seperatorRight.get() + " ")
                                .withColor(seperatorColor.get().getPacked())
                        )
                )
        );
    }
}
