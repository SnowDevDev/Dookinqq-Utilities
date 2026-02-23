package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.Notification;
import com.example.addon.utils.NotificationManager;
import com.example.addon.utils.RenderUtil;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SnowNotifs extends HudElement {
    public static final HudElementInfo<SnowNotifs> INFO = new HudElementInfo<>(AddonTemplate.HUD_GROUP, "Dookinqq Notifications", "Renders notification. This only lets you adjust the Y coord", SnowNotifs::new);

    public SnowNotifs() {
        super(INFO);
        setSize(40, 40);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> notificationBackground = sgGeneral.add(new ColorSetting.Builder()
        .name("Notification Background")
        .defaultValue(new SettingColor(18, 18, 18, 195))
        .build()
    );

    private final Setting<SettingColor> notificationTextColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Notification Text Color")
        .defaultValue(new SettingColor(0, 128, 255, 255))
        .build()
    );

    @Override
    public void render(HudRenderer renderer) {
        if (isInEditor()) {
            renderer.text("NOT", x, y, meteordevelopment.meteorclient.utils.render.color.Color.YELLOW, true);
            return;
        }

        SnowArrayList.AnimationProfile profile = SnowArrayList.getAnimationProfile();
        double speedMul = profile.syncHudChatAnimations ? clamp(profile.globalAnimationSpeed, 0.1, 4.0) : 1.0;

        double screenWidth = mc.getWindow().getWidth();
        double padding = 8;
        double offset = 10;

        int notifCount = NotificationManager.INSTANCE.getSnowNotifs().size();

        double totalHeight = 0;
        for (int i = 0; i < notifCount; i++) {
            double height = 10 + renderer.textHeight() + 10;
            totalHeight += height + 10;
        }

        double yTarget = y - totalHeight + 10;
        double enterDuration = 300.0 / speedMul;
        double leaveDuration = 400.0 / speedMul;
        double lerpStrength = clamp(0.10 * speedMul, 0.08, 0.42);
        double phaseTime = (System.currentTimeMillis() / 1000.0) * Math.max(0.1, profile.gradientSpeed);

        for (int i = 0; i < notifCount; i++) {
            Notification n = NotificationManager.INSTANCE.getSnowNotifs().get(i);
            String msg = n.getMessage();

            double textWidth = renderer.textWidth(msg);
            double width = padding + textWidth + padding;
            double height = 10 + renderer.textHeight() + 10;

            double baseX = screenWidth - width - offset;
            double targetY = yTarget;

            if (n.slideOffset == 0) n.slideOffset = screenWidth + width + 40;
            if (n.displayY == 0) n.displayY = targetY;

            double elapsed = n.getDelta();
            double maxTime = NotificationManager.INSTANCE.getMaxAgeMs();
            double remaining = maxTime - elapsed;

            double slideOffset = 0;

            if (elapsed < enterDuration) {
                double t = easeOutCubic(elapsed / enterDuration);
                slideOffset = (1.0 - t) * (width + 40);
            } else if (remaining < leaveDuration) {
                double t = (leaveDuration - remaining) / leaveDuration;
                if (t < 0.25) {
                    double p = easeOutCubic(t / 0.25);
                    slideOffset = -p * 20;
                } else {
                    double p = easeInCubic((t - 0.25) / 0.75);
                    slideOffset = p * (width + 40);
                }
            }

            double targetX = baseX + slideOffset;

            n.slideOffset = lerp(n.slideOffset, targetX, lerpStrength);
            n.displayY = lerp(n.displayY, targetY, lerpStrength);

            SettingColor bgColor = notificationBackground.get();
            SettingColor textColor = notificationTextColor.get();

            if (profile.syncHudChatAnimations) {
                SettingColor synced = profile.sampleColor((i * 0.18) + phaseTime, 1.0);
                textColor = new SettingColor(synced.r, synced.g, synced.b, textColor.a);
                bgColor = new SettingColor(
                    Math.max(0, synced.r / 7),
                    Math.max(0, synced.g / 7),
                    Math.max(0, synced.b / 7),
                    bgColor.a
                );
            }

            RenderUtil.renderRoundedQuad(renderer, n.slideOffset, n.displayY, width, height, 4, bgColor);
            renderer.text(msg, n.slideOffset + padding, n.displayY + 10, textColor, true);

            yTarget += height + 10;
        }
    }

    private double easeOutCubic(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }

    private double easeInCubic(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * t;
    }

    private double lerp(double start, double end, double factor) {
        return start + factor * (end - start);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
