package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.RenderUtil;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;

public class SnowFriends extends HudElement {
    public static final HudElementInfo<SnowFriends> INFO =
        new HudElementInfo<>(AddonTemplate.HUD_GROUP,
            "Dookinqq Friends",
            "Render friends list",
            SnowFriends::new);

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<SettingColor> background = sg.add(
        new ColorSetting.Builder()
            .name("background")
            .defaultValue(new SettingColor(18, 18, 18))
            .build()
    );

    private final Setting<SettingColor> border = sg.add(
        new ColorSetting.Builder()
            .name("border")
            .defaultValue(new SettingColor(192, 32, 32))
            .build()
    );

    private final Setting<SettingColor> text = sg.add(
        new ColorSetting.Builder()
            .name("text")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public SnowFriends() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        List<String> friends = new ArrayList<>();
        Friends.get().forEach(friend -> friends.add(friend.getName()));

        if (friends.isEmpty()) return;

        int padding = 6;
        int spacing = 16;

        double width = friends.stream()
            .mapToDouble(renderer::textWidth)
            .max()
            .orElse(40) + padding * 2;

        double height = friends.size() * spacing + padding;

        // Background FIRST
        renderer.quad(x, y, width, height, background.get());

        RenderUtil.renderRoundedOutline(renderer, x, y, width, height, 4, 1.0, border.get(), background.get());

        int sy = y + padding;

        for (String name : friends) {
            renderer.text(name, x + padding, sy, text.get(), true);
            sy += spacing;
        }

        setSize((int) width, (int) height);
    }
}


