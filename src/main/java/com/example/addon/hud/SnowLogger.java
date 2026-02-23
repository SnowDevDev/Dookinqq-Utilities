package com.example.addon.hud;

import com.example.addon.utils.RenderUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnowLogger extends HudElement {
    public static final HudElementInfo<SnowLogger> INFO =
        new HudElementInfo<>(com.example.addon.AddonTemplate.HUD_GROUP, "Dookinqq Logger", "Logs coordinates from chat.", SnowLogger::new);

    private enum AlignMode {
        Left,
        Center,
        Right
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStyle = settings.createGroup("Tab Style");
    private final SettingGroup sgAnim = settings.createGroup("Animations");

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("Maximum stored coordinates.")
        .defaultValue(10)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private final Setting<AlignMode> align = sgStyle.add(new EnumSetting.Builder<AlignMode>()
        .name("align")
        .description("Text alignment inside the logger panel.")
        .defaultValue(AlignMode.Left)
        .build()
    );

    private final Setting<SettingColor> selected = sgStyle.add(new ColorSetting.Builder()
        .name("selected")
        .description("Color for the newest coordinate line.")
        .defaultValue(new SettingColor(0, 128, 255, 255))
        .build()
    );

    private final Setting<SettingColor> normal = sgStyle.add(new ColorSetting.Builder()
        .name("normal")
        .description("Color for older coordinate lines.")
        .defaultValue(new SettingColor(224, 224, 224, 255))
        .build()
    );

    private final Setting<SettingColor> background = sgStyle.add(new ColorSetting.Builder()
        .name("background")
        .defaultValue(new SettingColor(8, 16, 30, 190))
        .build()
    );

    private final Setting<SettingColor> border = sgStyle.add(new ColorSetting.Builder()
        .name("border")
        .defaultValue(new SettingColor(0, 64, 128, 165))
        .build()
    );

    private final Setting<Boolean> animate = sgAnim.add(new BoolSetting.Builder()
        .name("animate")
        .description("Animates logger entries like Dookinqq tab transitions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> animationSpeed = sgAnim.add(new DoubleSetting.Builder()
        .name("animation-speed")
        .description("Entry animation speed for coordinate lines.")
        .defaultValue(10.0)
        .min(1.0)
        .sliderRange(1.0, 30.0)
        .visible(animate::get)
        .build()
    );

    private final Setting<Double> slideDistance = sgAnim.add(new DoubleSetting.Builder()
        .name("slide-distance")
        .description("Slide offset distance used by entry animations.")
        .defaultValue(12.0)
        .min(0.0)
        .sliderRange(0.0, 40.0)
        .visible(animate::get)
        .build()
    );

    private final Setting<Double> stagger = sgAnim.add(new DoubleSetting.Builder()
        .name("stagger")
        .description("Per-line animation delay.")
        .defaultValue(0.06)
        .min(0.0)
        .sliderRange(0.0, 0.2)
        .visible(animate::get)
        .build()
    );

    private final List<String> coords = new ArrayList<>();
    private final Map<String, Double> lineAnim = new HashMap<>();

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
            String coord = matcher.group(1) + ", " + matcher.group(2) + ", " + matcher.group(3);

            if (!coords.contains(coord)) {
                coords.add(0, coord);
                lineAnim.put(coord, 0.0);
            }

            while (coords.size() > limit.get()) {
                String removed = coords.remove(coords.size() - 1);
                lineAnim.remove(removed);
            }
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (coords.isEmpty()) {
            setSize(46, 16);
            return;
        }

        double step = 0.01 * animationSpeed.get();
        for (String coord : coords) {
            double p = lineAnim.getOrDefault(coord, 1.0);
            lineAnim.put(coord, animate.get() ? Math.min(1.0, p + step) : 1.0);
        }

        double textHeight = renderer.textHeight();
        double lineGap = 1.0;
        double padding = 6.0;

        double maxTextWidth = 0;
        for (String coord : coords) maxTextWidth = Math.max(maxTextWidth, renderer.textWidth(coord));

        double width = maxTextWidth + padding * 2.0;
        double height = padding * 2.0 + (coords.size() * textHeight) + (Math.max(0, coords.size() - 1) * lineGap);

        setSize(width, height);

        RenderUtil.renderRoundedOutline(renderer, x, y, width, height, 4, 1.0, border.get(), background.get());

        double yOffset = y + padding;
        for (int i = 0; i < coords.size(); i++) {
            String coord = coords.get(i);
            double progress = lineAnim.getOrDefault(coord, 1.0);
            double delayed = Math.max(0.0, Math.min(1.0, progress - (i * stagger.get())));
            double eased = animate.get() ? ease(delayed) : 1.0;

            double textWidth = renderer.textWidth(coord);
            double baseX = switch (align.get()) {
                case Left -> x + padding;
                case Center -> x + (width - textWidth) / 2.0;
                case Right -> x + width - padding - textWidth;
            };

            double dir = switch (align.get()) {
                case Left -> -1.0;
                case Right -> 1.0;
                case Center -> (i % 2 == 0 ? -1.0 : 1.0);
            };

            double drawX = baseX + (1.0 - eased) * slideDistance.get() * dir;
            SettingColor color = i == 0 ? selected.get() : normal.get();
            color = withAlpha(color, eased);

            renderer.text(coord, drawX, yOffset, color, true);
            yOffset += textHeight + lineGap;
        }
    }

    private double ease(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    private SettingColor withAlpha(SettingColor c, double a) {
        int alpha = (int) Math.max(0, Math.min(255, Math.round(c.a * Math.max(0.0, Math.min(1.0, a)))));
        return new SettingColor(c.r, c.g, c.b, alpha);
    }
}
