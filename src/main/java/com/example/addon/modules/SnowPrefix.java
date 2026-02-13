package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.ChatUtilsAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SnowPrefix extends Module {

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
        .defaultValue("Snow-Utils")
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
        .defaultValue(new SettingColor(192, 32, 32))
        .onChanged(value -> applyPrefix())
        .build()
    );

    private final Setting<SettingColor> seperatorColor = sgColor.add(new ColorSetting.Builder()
        .name("separator-color")
        .description("Color of the separators.")
        .defaultValue(new SettingColor(Formatting.GRAY))
        .onChanged(value -> applyPrefix())
        .build()
    );

    public SnowPrefix() {
        super(AddonTemplate.CATEGORY, "Snow Prefix", "Changes the Meteor chat prefix.");
    }

    @Override
    public void onActivate() {
        applyPrefix();
    }

    private void applyPrefix() {
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