package com.example.addon.hud;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnowLogger extends HudElement {
    public static final HudElementInfo<SnowLogger> INFO =
     new HudElementInfo<>(com.example.addon.AddonTemplate.HUD_GROUP, "Snow Logger", "Logs coordinates from chat.", SnowLogger::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("Maximum stored coordinates.")
        .defaultValue(10)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private final List<String> coords = new ArrayList<>();

    private final Pattern coordPattern = Pattern.compile(
        "(-?\\d{1,7})[, ]+(-?\\d{1,4})[, ]+(-?\\d{1,7})"
    );

    public SnowLogger() {
        super(INFO);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();

        Matcher matcher = coordPattern.matcher(message);

        while (matcher.find()) {
            String x = matcher.group(1);
            String y = matcher.group(2);
            String z = matcher.group(3);

            String coord = x + ", " + y + ", " + z;

            if (!coords.contains(coord)) {
                coords.add(0, coord);
            }

            while (coords.size() > limit.get()) {
                coords.remove(coords.size() - 1);
            }
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        double width = 0;
        double height = 0;

        for (String coord : coords) {
            width = Math.max(width, renderer.textWidth(coord));
            height += renderer.textHeight();
        }

        setSize(width + 6, height + 6);

        double yOffset = 3;

        for (String coord : coords) {
            renderer.text(coord, x + 3, y + yOffset, Color.WHITE, false);
            yOffset += renderer.textHeight();
        }
    }
}