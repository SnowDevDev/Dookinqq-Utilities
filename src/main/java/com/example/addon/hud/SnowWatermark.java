package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

public class SnowWatermark extends HudElement {

    public static final HudElementInfo<SnowWatermark> INFO =
            new HudElementInfo<>(AddonTemplate.HUD_GROUP,
                    "Dookinqq Watermark",
                    "Dookinqq Utils Watermark",
                    SnowWatermark::new);

    private int charIndex = 0;
    private boolean deleting = false;
    private long lastUpdate = 0;

    private long pauseEnd = 0;
    private final long pauseDuration = 1050;

    private final Random random = new Random();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final String fullText = "Dookinqq Utils";

    // Smooth animation width
    private double displayWidth = 0;
    private final double expandSpeed = 6.5;   // slower expand
    private final double shrinkSpeed = 12.0;  // faster shrink

    public SnowWatermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.world == null) return;

        long time = System.currentTimeMillis();

        if (time >= pauseEnd) {
            int delay = randomInt(50, 80);

            if (time - lastUpdate > delay) {
                lastUpdate = time;

                if (!deleting) {
                    charIndex++;
                    if (charIndex >= fullText.length()) {
                        charIndex = fullText.length();
                        deleting = true;
                        pauseEnd = time + pauseDuration;
                    }
                } else {
                    charIndex--;
                    if (charIndex <= 0) {
                        charIndex = 0;
                        deleting = false;
                        pauseEnd = time + pauseDuration;
                    }
                }
            }
        }

        charIndex = Math.max(0, Math.min(charIndex, fullText.length()));
        String visibleText = fullText.substring(0, charIndex);

        double targetWidth = renderer.textWidth(visibleText, true) + 10;
        double height = renderer.textHeight(true) + 6;

        // Smooth width animation
        if (displayWidth < targetWidth) {
            displayWidth += expandSpeed;
            if (displayWidth > targetWidth) displayWidth = targetWidth;
        } else if (displayWidth > targetWidth) {
            displayWidth -= shrinkSpeed;
            if (displayWidth < targetWidth) displayWidth = targetWidth;
        }

        setSize(displayWidth, height);

        // Background
        renderer.quad(x, y, displayWidth, height,
                new Color(15, 15, 20, 160));

        // Accent bar
        renderer.quad(x, y, displayWidth, 2,
                new Color(0, 128, 255));

        // Draw text only when background has space
        if (displayWidth > 12) {
            renderer.text(visibleText,
                    x + 5, y + 3,
                    Color.WHITE, true);
        }
    }

    private int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}