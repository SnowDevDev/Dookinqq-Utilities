package com.example.addon.themes;

import com.example.addon.themes.snow.WSnowCheckbox;
import com.example.addon.themes.snow.WSnowModule;
import com.example.addon.themes.snow.WSnowSlider;
import com.example.addon.themes.snow.WSnowWindow;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.awt.*;
import java.lang.reflect.Field;

public class DookinqqGuiTheme extends MeteorGuiTheme {

    public enum ParticleRenderMode { Pixel, Soft, Glow }
    public enum ParticlePhysicsMode { Linear, Accelerated, Turbulent }

    private final SettingGroup sgDookinqqGradient = settings.createGroup("Dookinqq Gradient");
    private final SettingGroup sgDookinqqModule = settings.createGroup("Module Text");
    private final SettingGroup sgFuturistic = settings.createGroup("Futuristic FX");

    public final Setting<SettingColor> gradientTop = sgDookinqqGradient.add(
        new ColorSetting.Builder()
            .name("gradient-top-color")
            .description("Top color of the Dookinqq GUI gradient.")
            .defaultValue(new SettingColor(0, 128, 255, 210))
            .build()
    );

    public final Setting<SettingColor> gradientBottom = sgDookinqqGradient.add(
        new ColorSetting.Builder()
            .name("gradient-bottom-color")
            .description("Bottom color of the Dookinqq GUI gradient.")
            .defaultValue(new SettingColor(0, 64, 128, 170))
            .build()
    );

    public final Setting<Double> gradientSpeed = sgDookinqqGradient.add(
        new DoubleSetting.Builder()
            .name("gradient-speed")
            .description("Animation speed of the Dookinqq GUI gradient.")
            .defaultValue(1.2)
            .min(0.0)
            .sliderRange(0.0, 4.0)
            .build()
    );

    public final Setting<Double> moduleGlowStrength = sgDookinqqModule.add(
        new DoubleSetting.Builder()
            .name("module-glow-strength")
            .description("Glow strength for module tab text.")
            .defaultValue(0.24)
            .min(0.0)
            .sliderRange(0.0, 1.5)
            .build()
    );

    public final Setting<SettingColor> bindColor = sgDookinqqModule.add(
        new ColorSetting.Builder()
            .name("bind-color")
            .description("Color used for module bind text in the GUI.")
            .defaultValue(new SettingColor(125, 215, 255, 245))
            .build()
    );

    public final Setting<Double> bindGlowStrength = sgDookinqqModule.add(
        new DoubleSetting.Builder()
            .name("bind-glow-strength")
            .description("Glow strength for module bind text.")
            .defaultValue(0.2)
            .min(0.0)
            .sliderRange(0.0, 1.5)
            .build()
    );

    public final Setting<SettingColor> panelLineColor = sgFuturistic.add(
        new ColorSetting.Builder()
            .name("panel-line-color")
            .description("Primary panel line color for futuristic outlines.")
            .defaultValue(new SettingColor(52, 255, 245, 135))
            .build()
    );

    public final Setting<SettingColor> particleColor = sgFuturistic.add(
        new ColorSetting.Builder()
            .name("particle-color")
            .description("Color used for GUI particles.")
            .defaultValue(new SettingColor(180, 245, 255, 140))
            .build()
    );

    public final Setting<Double> particleOpacity = sgFuturistic.add(
        new DoubleSetting.Builder()
            .name("particle-opacity")
            .description("Global opacity multiplier for GUI particles.")
            .defaultValue(0.5)
            .min(0.0)
            .sliderRange(0.0, 1.5)
            .build()
    );

    public final Setting<Integer> particleCount = sgFuturistic.add(
        new IntSetting.Builder()
            .name("particle-count")
            .description("Number of particles rendered in futuristic effects.")
            .defaultValue(34)
            .min(0)
            .sliderRange(0, 120)
            .build()
    );

    public final Setting<ParticleRenderMode> particleRenderMode = sgFuturistic.add(
        new EnumSetting.Builder<ParticleRenderMode>()
            .name("particle-render-mode")
            .description("How snow particles are drawn.")
            .defaultValue(ParticleRenderMode.Soft)
            .build()
    );

    public final Setting<ParticlePhysicsMode> particlePhysicsMode = sgFuturistic.add(
        new EnumSetting.Builder<ParticlePhysicsMode>()
            .name("particle-physics-mode")
            .description("How snow particles move.")
            .defaultValue(ParticlePhysicsMode.Accelerated)
            .build()
    );

    public final Setting<Double> particleFallSpeed = sgFuturistic.add(
        new DoubleSetting.Builder()
            .name("particle-fall-speed")
            .description("Global fall speed multiplier for snow particles.")
            .defaultValue(0.65)
            .min(0.05)
            .sliderRange(0.05, 2.0)
            .build()
    );

    public final Setting<Double> particleGravity = sgFuturistic.add(
        new DoubleSetting.Builder()
            .name("particle-gravity")
            .description("Gravity multiplier used by accelerated physics.")
            .defaultValue(0.9)
            .min(0.0)
            .sliderRange(0.0, 2.0)
            .build()
    );

    public final Setting<Double> glowRadius = sgFuturistic.add(
        new DoubleSetting.Builder()
            .name("glow-radius")
            .description("Radius of futuristic glow effects.")
            .defaultValue(1.2)
            .min(0.0)
            .sliderRange(0.0, 4.0)
            .build()
    );

    public final Setting<Double> glowAlpha = sgFuturistic.add(
        new DoubleSetting.Builder()
            .name("glow-alpha")
            .description("Global alpha multiplier for glow effects.")
            .defaultValue(0.75)
            .min(0.0)
            .sliderRange(0.0, 1.5)
            .build()
    );

    public DookinqqGuiTheme() {
        renameThemeToDookinqq();
        settingsFactory = new DefaultSettingsWidgetFactory(this);

        if (placeholderColor.get().r != 33) {
            moduleAlignment.set(AlignmentX.Left);
            patchScaleMinimum();
            scale.set(0.675);
            accentColor.set(new SettingColor(new Color(0, 128, 255, 210)));
            placeholderColor.set(new SettingColor(new Color(0, 128, 255, 255)));
            moduleBackground.set(new SettingColor(new Color(0, 40, 88, 132)));
            backgroundColor.get().set(new SettingColor(new Color(0, 20, 44, 116)));
            sliderRight.set(new SettingColor(new Color(0, 36, 72, 190)));
            textColor.set(new SettingColor(new Color(228, 246, 255, 255)));
            textSecondaryColor.set(new SettingColor(new Color(142, 198, 232, 255)));
        }
    }

    @Override
    public WWidget module(Module module) {
        return w(new WSnowModule(module));
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WSnowWindow(icon, title));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WSnowSlider(value, min, max));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WSnowCheckbox(checked));
    }

    public SettingColor fluidGradientColor(double xNorm, double yNorm, double phaseOffset, double alphaScale) {
        SettingColor top = gradientTop.get();
        SettingColor bottom = gradientBottom.get();

        double x = clamp01(xNorm);
        double y = clamp01(yNorm);
        double time = (System.currentTimeMillis() / 1000.0) * gradientSpeed.get();

        double waveA = Math.sin((x * 6.0) + (time * 1.4) + phaseOffset);
        double waveB = Math.cos((y * 7.0) - (time * 1.1) + (x * 2.2));
        double waveC = Math.sin(((x + y) * 5.0) + (time * 0.8) - phaseOffset);
        double mix = (waveA * 0.45) + (waveB * 0.35) + (waveC * 0.20);
        double t = clamp01((mix + 1.0) * 0.5);

        int r = (int) Math.round(lerp(top.r, bottom.r, t));
        int g = (int) Math.round(lerp(top.g, bottom.g, t));
        int b = (int) Math.round(lerp(top.b, bottom.b, t));
        int a = (int) Math.round(lerp(top.a, bottom.a, t) * clamp01(alphaScale));

        return new SettingColor(clamp255(r), clamp255(g), clamp255(b), clamp255(a));
    }

    public SettingColor futuristicParticleColor(double phase, double alphaScale) {
        SettingColor base = particleColor.get();
        double flicker = 0.55 + (0.45 * Math.sin(phase));
        int a = (int) Math.round(base.a * particleOpacity.get() * flicker * clamp01(alphaScale));
        return new SettingColor(base.r, base.g, base.b, clamp255(a));
    }

    private void patchScaleMinimum() {
        try {
            Class<?> klass = scale.getClass();
            Field minField = klass.getDeclaredField("min");
            minField.setAccessible(true);
            minField.setDouble(scale, 0.375);

            Field sliderMinField = klass.getDeclaredField("sliderMin");
            sliderMinField.setAccessible(true);
            sliderMinField.setDouble(scale, 0.375);
        } catch (Throwable ignored) {
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void renameThemeToDookinqq() {
        try {
            Field nameField = GuiTheme.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(this, "Dookinqq");
        } catch (Exception ignored) {
        }
    }
}