package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SnowLogo extends HudElement {

    public static final HudElementInfo<SnowLogo> INFO =
        new HudElementInfo<>(AddonTemplate.HUD_GROUP, "Snow Logo", "Badge or something.", SnowLogo::new);

    public SnowLogo() {
        super(INFO);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .defaultValue(1)
        .min(0.1)
        .sliderRange(0.1, 5)
        .build()
    );

    private final Identifier logo = Identifier.of(AddonTemplate.MOD_ID, "logo.png");

    @Override
    public void render(HudRenderer renderer) {
        double size = 256 * scale.get();
        setSize(size, size);

        MatrixStack matrices = new MatrixStack();

        GL.bindTexture(logo);
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, size, size, new Color(255, 255, 255, 255));
        Renderer2D.TEXTURE.render(matrices);
    }
}