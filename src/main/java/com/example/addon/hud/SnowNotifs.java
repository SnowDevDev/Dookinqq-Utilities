package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.Notification;
import com.example.addon.utils.NotificationManager;
import me.x150.renderer.render.Renderer2d;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.awt.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SnowNotifs extends HudElement {
    public static final HudElementInfo<SnowNotifs> INFO = new HudElementInfo<>(AddonTemplate.HUD_GROUP, "Snow Notifications", "Renders notification. This only lets you adjust the Y coord", SnowNotifs::new);

    public SnowNotifs(){
        super(INFO);
        setSize(40,40);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> notificationBackground = sgGeneral.add(new ColorSetting.Builder()
        .name("Notification Background")
        .defaultValue(new SettingColor(18, 18, 18, 195))
        .build()
    );

    private final Setting<SettingColor> notificationTextColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Notification Text Color")
        .defaultValue(new SettingColor(192, 32, 32))
        .build()
    );

    @Override
    public void render(HudRenderer renderer) {
        if (isInEditor()) {
            renderer.text("NOT", x, y, meteordevelopment.meteorclient.utils.render.color.Color.YELLOW, false);
            return;
        }

        double screenWidth = mc.getWindow().getWidth();
        double padding = 8;
        double offset = 10;

        int notifCount = NotificationManager.INSTANCE.getSnowNotifs().size();

        double totalHeight = 0;
        for (int i = 0; i < notifCount; i++) {
            Notification n = NotificationManager.INSTANCE.getSnowNotifs().get(i);
            double height = 10 + renderer.textHeight() + 10;
            totalHeight += height + 10;
        }

        double yTarget = y - totalHeight + 10;

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

            if (elapsed < 300) {
                double t = easeOutCubic(elapsed / 300.0);
                slideOffset = (1.0 - t) * (width + 40);
            }
            else if (remaining < 400) {
                double t = (400 - remaining) / 400.0;
                if (t < 0.25) {
                    double p = easeOutCubic(t / 0.25);
                    slideOffset = -p * 20;
                } else {
                    double p = easeInCubic((t - 0.25) / 0.75);
                    slideOffset = p * (width + 40);
                }
            }

            double targetX = baseX + slideOffset;

            n.slideOffset = lerp(n.slideOffset, targetX, 0.15);
            n.displayY = lerp(n.displayY, targetY, 0.15);

            Renderer2d.renderRoundedQuad(
                renderer.drawContext.getMatrices(),
                new Color(notificationBackground.get().r, notificationBackground.get().g, notificationBackground.get().b, notificationBackground.get().a),
                n.slideOffset,
                n.displayY,
                n.slideOffset + width,
                n.displayY + height,
                5, 5
            );

            renderer.text(msg, n.slideOffset + padding, n.displayY + 10, notificationTextColor.get(), false);

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

}
