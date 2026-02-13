package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import me.x150.renderer.render.Renderer2d;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SnowFriends extends HudElement {
    public static final HudElementInfo<SnowFriends> INFO = new HudElementInfo<>(AddonTemplate.HUD_GROUP, "Snow Friends", "Render friends list", SnowFriends::new);

    public SnowFriends(){
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
        .defaultValue(new SettingColor(255, 0, 76))
        .build()
    );
    private final Setting<SettingColor> text = sgGeneral.add(new ColorSetting.Builder()
        .name("Text")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private List<String> getFriends(){
        List<String> friends = new ArrayList<>();
        Friends.get().iterator().forEachRemaining(friend -> friends.add(friend.getName()));
        return friends;
    }

    private String longestFriendName(){
        String longestFriend = getFriends().getFirst();

        for (String friend : getFriends()) {
            if (friend.length() > longestFriend.length()) {
                longestFriend = friend;
            }
        }

        return longestFriend;
    }


    @Override
    public void render(HudRenderer renderer) {
        box.setSize(20,20);
        if (getFriends().isEmpty()) return;
        int sx = x + 5;
        int sy = y + 5;
        final int space = 25;
        double width = renderer.textWidth(longestFriendName());
        for (String fren : getFriends()) {
            renderer.text(fren, sx, sy, text.get(), false);
            sy += space;
        }

        renderer.quad(x - 5,y - 5, width * 1.2, sy - y + 10,background.get());
        Renderer2d.renderRoundedOutline(renderer.drawContext.getMatrices(), new Color(border.get().r,border.get().g,border.get().b,border.get().a),x - 5, y - 5, (x-5) +width * 1.2, (y-5) +(sy - y) + 10, 0,5, 5);

        setSize(40, 40);
    }
}
