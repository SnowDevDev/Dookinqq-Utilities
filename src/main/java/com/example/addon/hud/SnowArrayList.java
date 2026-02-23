package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public class SnowArrayList extends HudElement {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public static final HudElementInfo<SnowArrayList> INFO = new HudElementInfo<>(
        AddonTemplate.HUD_GROUP,
        "Dookinqq ArrayList",
        "Advanced animated arraylist with global HUD/chat animation sync.",
        SnowArrayList::new
    );

    private enum AnimationStyle { ClassicSlide, SmoothFade, Elastic, Wave }

    public enum ElColorPickerMode {
        ElGradient,
        ThemeColorSync,
        ThemeGradientSync
    }

    public static final class AnimationProfile {
        public final boolean syncHudChatAnimations;
        public final double globalAnimationSpeed;
        public final double chatSlideDistance;
        public final double chatPulseAlpha;
        public final ElColorPickerMode colorMode;
        public final double gradientSpeed;
        public final double gradientWidth;

        private final SettingColor gradientStart;
        private final SettingColor gradientEnd;
        private final SettingColor themeTop;
        private final SettingColor themeBottom;

        private AnimationProfile(
            boolean syncHudChatAnimations,
            double globalAnimationSpeed,
            double chatSlideDistance,
            double chatPulseAlpha,
            ElColorPickerMode colorMode,
            double gradientSpeed,
            double gradientWidth,
            SettingColor gradientStart,
            SettingColor gradientEnd,
            SettingColor themeTop,
            SettingColor themeBottom
        ) {
            this.syncHudChatAnimations = syncHudChatAnimations;
            this.globalAnimationSpeed = globalAnimationSpeed;
            this.chatSlideDistance = chatSlideDistance;
            this.chatPulseAlpha = chatPulseAlpha;
            this.colorMode = colorMode;
            this.gradientSpeed = gradientSpeed;
            this.gradientWidth = gradientWidth;
            this.gradientStart = gradientStart;
            this.gradientEnd = gradientEnd;
            this.themeTop = themeTop;
            this.themeBottom = themeBottom;
        }

        public SettingColor sampleColor(double phaseInput, double alphaScale) {
            double blend = computeBlend(phaseInput);

            SettingColor first;
            SettingColor second;

            if (colorMode == ElColorPickerMode.ThemeColorSync) {
                first = themeTop;
                second = themeTop;
            } else if (colorMode == ElColorPickerMode.ThemeGradientSync) {
                first = themeTop;
                second = themeBottom;
            } else {
                first = gradientStart;
                second = gradientEnd;
            }

            int r = lerp(first.r, second.r, blend);
            int g = lerp(first.g, second.g, blend);
            int b = lerp(first.b, second.b, blend);
            int a = clamp255((int) Math.round(255.0 * clamp(alphaScale, 0.0, 1.0)));
            return new SettingColor(r, g, b, a);
        }

        private double computeBlend(double phaseInput) {
            if (colorMode == ElColorPickerMode.ThemeColorSync) return 0.0;
            double wave = Math.sin(phaseInput * Math.PI * 2.0);
            return (wave + 1.0) * 0.5;
        }

        private static int lerp(int a, int b, double t) {
            return clamp255((int) Math.round(a + (b - a) * clamp(t, 0.0, 1.0)));
        }

        private static int clamp255(int value) {
            return Math.max(0, Math.min(255, value));
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final SettingColor THEME_TOP = new SettingColor(0, 128, 255, 255);
    private static final SettingColor THEME_BOTTOM = new SettingColor(0, 64, 128, 255);

    private static volatile AnimationProfile animationProfile = defaultProfile();

    private static AnimationProfile defaultProfile() {
        return new AnimationProfile(
            true,
            1.0,
            4.0,
            0.32,
            ElColorPickerMode.ThemeGradientSync,
            1.5,
            120.0,
            new SettingColor(0, 128, 255, 255),
            new SettingColor(0, 64, 128, 255),
            new SettingColor(THEME_TOP.r, THEME_TOP.g, THEME_TOP.b, THEME_TOP.a),
            new SettingColor(THEME_BOTTOM.r, THEME_BOTTOM.g, THEME_BOTTOM.b, THEME_BOTTOM.a)
        );
    }

    public static AnimationProfile getAnimationProfile() {
        return animationProfile;
    }

    private final SettingGroup sg = settings.getDefaultGroup();
    private final SettingGroup sgConfig = settings.createGroup("Config");
    private final SettingGroup sgColors = settings.createGroup("El Color Picker");

    private final Setting<AnimationStyle> animationStyle = sgConfig.add(new EnumSetting.Builder<AnimationStyle>()
        .name("animation-style")
        .description("Animation style for module entries.")
        .defaultValue(AnimationStyle.SmoothFade)
        .build()
    );

    private final Setting<Double> enterSpeed = sgConfig.add(new DoubleSetting.Builder()
        .name("enter-speed")
        .description("Speed when a module appears.")
        .defaultValue(3)
        .min(1)
        .sliderMax(30)
        .build()
    );

    private final Setting<Double> exitSpeed = sgConfig.add(new DoubleSetting.Builder()
        .name("exit-speed")
        .description("Speed when a module disappears.")
        .defaultValue(2.5)
        .min(1)
        .sliderMax(30)
        .build()
    );

    private final Setting<Double> stagger = sgConfig.add(new DoubleSetting.Builder()
        .name("stagger")
        .description("Delay between lines for smoother cascading motion.")
        .defaultValue(0.04)
        .min(0)
        .sliderRange(0, 0.2)
        .build()
    );

    private final Setting<Double> waveAmplitude = sgConfig.add(new DoubleSetting.Builder()
        .name("wave-amplitude")
        .description("Extra Y sway in Wave animation.")
        .defaultValue(2.5)
        .min(0)
        .sliderRange(0, 8)
        .visible(() -> animationStyle.get() == AnimationStyle.Wave)
        .build()
    );

    private final Setting<Double> waveSpeed = sgConfig.add(new DoubleSetting.Builder()
        .name("wave-speed")
        .description("Wave animation speed.")
        .defaultValue(2.2)
        .min(0.1)
        .sliderRange(0.1, 8)
        .visible(() -> animationStyle.get() == AnimationStyle.Wave)
        .build()
    );

    private final Setting<Double> glowStrength = sgConfig.add(new DoubleSetting.Builder()
        .name("glow-strength")
        .description("Glow strength for arraylist text.")
        .defaultValue(0.35)
        .min(0.0)
        .sliderRange(0.0, 1.5)
        .build()
    );

    private final Setting<AutoAlignMode> alignment = sgConfig.add(new EnumSetting.Builder<AutoAlignMode>()
        .name("alignment")
        .description("Alignment of arraylist module text.")
        .defaultValue(AutoAlignMode.Right)
        .build()
    );

    private final Setting<Boolean> onlyBound = sg.add(new BoolSetting.Builder()
        .name("only-bound")
        .description("Only show modules with a keybind set.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyDookinqqModules = sg.add(new BoolSetting.Builder()
        .name("only-dookinqq-modules")
        .description("Only show Dookinqq addon modules in the arraylist.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> syncHudChatAnimations = sgConfig.add(new BoolSetting.Builder()
        .name("sync-hud-chat-animations")
        .description("Use this element as the global animation controller for HUD and chat.")
        .defaultValue(true)
        .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<Double> globalAnimationSpeed = sgConfig.add(new DoubleSetting.Builder()
        .name("global-animation-speed")
        .description("Master speed multiplier for HUD and chat animations.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 4.0)
        .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<Double> chatSlideDistance = sgConfig.add(new DoubleSetting.Builder()
        .name("chat-slide-distance")
        .description("Horizontal chat text motion intensity.")
        .defaultValue(4.0)
        .min(0.0)
        .sliderRange(0.0, 18.0)
        .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<Double> chatPulseAlpha = sgConfig.add(new DoubleSetting.Builder()
        .name("chat-pulse-alpha")
        .description("Extra alpha pulse used by chat animation.")
        .defaultValue(0.32)
        .min(0.0)
        .sliderRange(0.0, 1.0)
        .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<ElColorPickerMode> elColorPickerMode = sgColors.add(new EnumSetting.Builder<ElColorPickerMode>()
        .name("mode")
        .description("Color source in this color picker section: el gradient / theme color sync / theme gradient sync.")
        .defaultValue(ElColorPickerMode.ElGradient)
        .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<Double> gradientSpeed = sgColors.add(new DoubleSetting.Builder()
        .name("gradient-speed")
        .description("Animation speed for gradient-based color modes.")
        .defaultValue(1.5)
        .min(0.1)
        .sliderMax(10)
        .onChanged(v -> publishAnimationProfile())
        .visible(() -> elColorPickerMode.get() != ElColorPickerMode.ThemeColorSync)
        .build()
    );

    private final Setting<Double> gradientWidth = sgColors.add(new DoubleSetting.Builder()
        .name("gradient-width")
        .description("Vertical span of one full gradient wave.")
        .defaultValue(120)
        .min(20)
        .sliderRange(20, 400)
        .onChanged(v -> publishAnimationProfile())
        .visible(() -> elColorPickerMode.get() != ElColorPickerMode.ThemeColorSync)
        .build()
    );

    private final Setting<SettingColor> gradientStart = sgColors.add(new ColorSetting.Builder()
        .name("gradient-start")
        .description("Gradient start color (RGB/rainbow picker).")
        .defaultValue(new SettingColor(0, 128, 255, 255))
                .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<SettingColor> gradientEnd = sgColors.add(new ColorSetting.Builder()
        .name("gradient-end")
        .description("Gradient end color (RGB/rainbow picker).")
        .defaultValue(new SettingColor(0, 64, 128, 255))
        .onChanged(v -> publishAnimationProfile())
        .build()
    );

    private final Setting<Double> bloomStrength = sgColors.add(new DoubleSetting.Builder()
        .name("bloom-strength")
        .description("Extra soft bloom from the current color picker output.")
        .defaultValue(0.25)
        .min(0.0)
        .sliderRange(0.0, 1.5)
        .build()
    );

    private final Map<Module, Double> moduleProgress = new HashMap<>();

    public SnowArrayList() {
        super(INFO);
        publishAnimationProfile();
    }

    @Override
    public void render(HudRenderer renderer) {
        publishAnimationProfile();

        AnimationProfile profile = animationProfile;
        double globalSpeed = Math.max(0.1, profile.globalAnimationSpeed);

        double frameTime = 0.01 * globalSpeed;
        double enterStep = frameTime * enterSpeed.get();
        double exitStep = frameTime * exitSpeed.get();

        Collection<Module> allModules = Modules.get().getAll();
        for (Module m : allModules) {
            boolean active = m.isActive();
            double current = moduleProgress.getOrDefault(m, 0.0);
            if (active && current < 1.0) moduleProgress.put(m, Math.min(1.0, current + enterStep));
            else if (!active && current > 0.0) moduleProgress.put(m, Math.max(0.0, current - exitStep));
        }

        List<Module> toRender = new ArrayList<>();
        moduleProgress.forEach((m, progress) -> {
            if (progress <= 0.0) return;
            if (onlyBound.get() && !hasBoundKeybind(m)) return;
            if (onlyDookinqqModules.get() && !isDookinqqModule(m)) return;
            toRender.add(m);
        });

        toRender.sort(Comparator.comparingDouble(m -> -renderer.textWidth(normalizeModuleTitle(m.title))));

        double maxWidth = 0;
        for (Module m : toRender) maxWidth = Math.max(maxWidth, renderer.textWidth(normalizeModuleTitle(m.title)));

        AutoAlignMode alignMode = resolveAlignMode(maxWidth);

        double yOffset = 0;
        double timeSeconds = System.currentTimeMillis() / 1000.0;

        for (int i = 0; i < toRender.size(); i++) {
            Module m = toRender.get(i);
            double rawProgress = moduleProgress.getOrDefault(m, 0.0);
            double lineDelay = i * (stagger.get() / globalSpeed);
            double delayed = Math.max(0.0, Math.min(1.0, rawProgress - lineDelay));
            double eased = ease(delayed, animationStyle.get());

            String text = normalizeModuleTitle(m.title);
            double textWidth = renderer.textWidth(text);

            double baseX = switch (alignMode) {
                case Left -> x;
                case Center -> x + (maxWidth - textWidth) / 2.0;
                case Right, Auto -> x + maxWidth - textWidth;
            };

            double slideDistance = switch (alignMode) {
                case Left -> -textWidth;
                case Right, Auto -> textWidth;
                case Center -> (i % 2 == 0 ? -textWidth * 0.45 : textWidth * 0.45);
            };

            double drawX = baseX + (1.0 - eased) * slideDistance;
            double drawY = y + yOffset;

            if (animationStyle.get() == AnimationStyle.Wave) {
                drawY += Math.sin((timeSeconds * waveSpeed.get() * globalSpeed) + i * 0.7) * waveAmplitude.get() * eased;
            }

            int alpha = (int) Math.max(0, Math.min(255, Math.round(255 * eased)));
            Color textColor = getProfileColor(drawY, alpha, timeSeconds, profile);

            double glow = glowStrength.get();
            if (glow > 0) {
                int ga = (int) Math.max(0, Math.min(180, alpha * glow));
                Color gc = new Color(textColor.r, textColor.g, textColor.b, ga);
                renderer.text(text, drawX - 0.6, drawY, gc, false);
                renderer.text(text, drawX + 0.6, drawY, gc, false);
                renderer.text(text, drawX, drawY - 0.6, gc, false);
                renderer.text(text, drawX, drawY + 0.6, gc, false);
            }

            double bloom = bloomStrength.get();
            if (bloom > 0) {
                int ba = (int) Math.max(0, Math.min(140, alpha * bloom));
                Color bc = new Color(textColor.r, textColor.g, textColor.b, ba);
                renderer.text(text, drawX - 1.3, drawY, bc, false);
                renderer.text(text, drawX + 1.3, drawY, bc, false);
                renderer.text(text, drawX, drawY - 1.3, bc, false);
                renderer.text(text, drawX, drawY + 1.3, bc, false);
                renderer.text(text, drawX - 0.9, drawY - 0.9, bc, false);
                renderer.text(text, drawX + 0.9, drawY - 0.9, bc, false);
                renderer.text(text, drawX - 0.9, drawY + 0.9, bc, false);
                renderer.text(text, drawX + 0.9, drawY + 0.9, bc, false);
            }

            renderer.text(text, drawX, drawY, textColor, false);
            yOffset += renderer.textHeight() * eased;
        }

        setSize(maxWidth, yOffset);
    }

    private Color getProfileColor(double drawY, int alpha, double timeSeconds, AnimationProfile profile) {
        double span = Math.max(20.0, profile.gradientWidth);
        double phase = ((drawY / span) + (timeSeconds * profile.gradientSpeed));
        SettingColor c = profile.sampleColor(phase, alpha / 255.0);
        return new Color(c.r, c.g, c.b, c.a);
    }

    private void publishAnimationProfile() {
        animationProfile = new AnimationProfile(
            syncHudChatAnimations.get(),
            globalAnimationSpeed.get(),
            chatSlideDistance.get(),
            chatPulseAlpha.get(),
            elColorPickerMode.get(),
            gradientSpeed.get(),
            gradientWidth.get(),
            copy(gradientStart.get()),
            copy(gradientEnd.get()),
            new SettingColor(THEME_TOP.r, THEME_TOP.g, THEME_TOP.b, THEME_TOP.a),
            new SettingColor(THEME_BOTTOM.r, THEME_BOTTOM.g, THEME_BOTTOM.b, THEME_BOTTOM.a)
        );
    }

    private static SettingColor copy(SettingColor color) {
        return new SettingColor(color.r, color.g, color.b, color.a);
    }

    private AutoAlignMode resolveAlignMode(double maxWidth) {
        AutoAlignMode selected = alignment.get();
        if (selected != AutoAlignMode.Auto) return selected;
        return resolveAutoAlignMode(maxWidth);
    }

    private AutoAlignMode resolveAutoAlignMode(double maxWidth) {
        double screenWidth = mc.getWindow().getScaledWidth();
        double center = x + (maxWidth / 2.0);
        if (center < screenWidth / 3.0) return AutoAlignMode.Left;
        if (center > (screenWidth * 2.0) / 3.0) return AutoAlignMode.Right;
        return AutoAlignMode.Center;
    }

    private double ease(double t, AnimationStyle style) {
        t = Math.max(0, Math.min(1, t));
        return switch (style) {
            case ClassicSlide -> t;
            case SmoothFade -> t * t * (3 - 2 * t);
            case Elastic -> {
                double c4 = (2 * Math.PI) / 3;
                if (t == 0 || t == 1) yield t;
                yield Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
            }
            case Wave -> 1 - Math.pow(1 - t, 3);
        };
    }

    private boolean hasBoundKeybind(Module module) {
        try {
            var field = module.getClass().getSuperclass().getDeclaredField("keybind");
            field.setAccessible(true);
            Object keybindSetting = field.get(module);
            if (keybindSetting == null) return false;
            var getMethod = keybindSetting.getClass().getMethod("get");
            Object keybind = getMethod.invoke(keybindSetting);
            if (keybind == null) return false;
            try {
                var isSet = keybind.getClass().getMethod("isSet");
                Object val = isSet.invoke(keybind);
                if (val instanceof Boolean b) return b;
            } catch (NoSuchMethodException ignored) {
            }
            String s = keybind.toString();
            return s != null && !s.isBlank() && !s.equalsIgnoreCase("none");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isDookinqqModule(Module module) {
        if (module == null) return false;

        if (module.category == AddonTemplate.CATEGORY) return true;

        if (module.category != null && module.category.name != null) {
            String categoryName = module.category.name.toLowerCase(Locale.ROOT);
            if (categoryName.contains("dookinqq")) return true;
        }

        String className = module.getClass().getName().toLowerCase(Locale.ROOT);
        return className.startsWith("com.example.addon.");
    }

    private String normalizeModuleTitle(String title) {
        if (title == null) return "";
        String s = title.replaceAll("\\s*-\\s*", " - ");
        return s.replaceAll("\\s{2,}", " ").trim();
    }

    private enum AutoAlignMode { Auto, Left, Center, Right }
}








