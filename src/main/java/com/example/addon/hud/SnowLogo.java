package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Identifier;

public class SnowLogo extends HudElement {
    public static final HudElementInfo<SnowLogo> INFO = new HudElementInfo<>(
        AddonTemplate.HUD_GROUP, "Dookinqq Logo", "Displays the mod icon with text using a custom font.", SnowLogo::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the logo and banner.")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderMin(0.1)
        .sliderMax(5.0)
        .build()
    );

    private static final Identifier LOGO = Identifier.of("template", "icon.png");
private static final Identifier BANNER = Identifier.of("template", "banner.png");

    public SnowLogo() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        double baseLogoSize = 64;
        double baseBannerHeight = baseLogoSize * 0.75;
        double baseBannerWidth = baseBannerHeight * 4;
        double basePadding = 6;

        double scaledLogoSize = baseLogoSize * scale.get();
        double scaledBannerHeight = baseBannerHeight * scale.get();
        double scaledBannerWidth = baseBannerWidth * scale.get();
        double scaledPadding = basePadding * scale.get();

        setSize(scaledLogoSize + scaledPadding + scaledBannerWidth, Math.max(scaledLogoSize, scaledBannerHeight));
        renderer.texture(LOGO, x, y, scaledLogoSize, scaledLogoSize, Color.WHITE);
        renderer.texture(
            BANNER,
            x + scaledLogoSize + scaledPadding,
            y + (scaledLogoSize - scaledBannerHeight) / 2,
            scaledBannerWidth,
            scaledBannerHeight,
            Color.WHITE
        );
    }
}


