package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;

public class SnowWatermark extends HudElement {

    public static final HudElementInfo<SnowWatermark> INFO =
            new HudElementInfo<>(AddonTemplate.HUD_GROUP,
                    "snow-watermark",
                    "Snow Utils Watermark",
                    SnowWatermark::new);

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public SnowWatermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {

        String clientName = "Snow Utils";
        String version = "v1.0";
        String username = mc.getSession().getUsername();

        String text = clientName + " §7| §f" + username + " §7| §b" + version;

        double width = renderer.textWidth(text, true);
        double height = renderer.textHeight(true);

        setSize(width + 10, height + 6);

        // Background
        renderer.quad(x, y, width + 10, height + 6,
                new Color(15, 15, 20, 160));

        // Accent bar (top)
        renderer.quad(x, y, width + 10, 2,
                new Color(255, 105, 180)); // soft pink accent

        // Main text (gradient-ish effect)
        renderer.text(clientName, x + 5, y + 3,
                new Color(255, 105, 180), true);

        double offset = renderer.textWidth(clientName, true);

        renderer.text(" §7| §f" + username + " §7| §b" + version,
                x + 5 + offset, y + 3,
                Color.WHITE, true);
    }
}