package com.example.addon;

import com.example.addon.hud.*;
import com.example.addon.modules.*;
import com.example.addon.themes.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Locale;

public class AddonTemplate extends MeteorAddon {

    public static final String MOD_ID = "dookinqq-utils";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Dookinqq Utils");
    public static final HudGroup HUD_GROUP = new HudGroup("Dookinqq Utils");

    private static final int THEME_TOP_R = 0;
    private static final int THEME_TOP_G = 128;
    private static final int THEME_TOP_B = 255;
    private static final int THEME_BOTTOM_R = 0;
    private static final int THEME_BOTTOM_G = 64;
    private static final int THEME_BOTTOM_B = 128;

    @Override
    public void onInitialize() {
        LOG.info("successfully winged the ding");

        Modules.get().add(new SnowAscend());
        Modules.get().add(new SnowCrash());
        Modules.get().add(new SnowDupe());
        Modules.get().add(new SnowMIK());
        Modules.get().add(new SnowRIK());
        Modules.get().add(new SnowDM());
        Modules.get().add(new SnowFatty());
        Modules.get().add(new SnowSniper());
        Modules.get().add(new SnowAntiOpium());
        Modules.get().add(new SnowAHB());
        Modules.get().add(new SnowABB());
        Modules.get().add(new SnowABBr());
        Modules.get().add(new SnowGrimFlight());
        Modules.get().add(new SnowGFF());
        Modules.get().add(new SnowTotem());
        Modules.get().add(new SnowFreeze());
        Modules.get().add(new SnowNameTags());
        Modules.get().add(new SnowLagFinder());
        Modules.get().add(new SnowSwap());
        Modules.get().add(new SnowSpunge());
        Modules.get().add(new SnowAntiCrash());
        Modules.get().add(new SnowSoundFinder());
        Modules.get().add(new SnowMiner());
        Modules.get().add(new SnowNoPop());
        Modules.get().add(new SnowKicker());
        Modules.get().add(new SnowCaster());
        Modules.get().add(new SnowHWB());
        Modules.get().add(new SnowAutoWither());
        Modules.get().add(new SnowSpeed());
        Modules.get().add(new SnowPrefix());
        Modules.get().add(new SnowASTP());
        Modules.get().add(new SnowAutoMine());
        Modules.get().add(new SnowSeedDupe());
        Modules.get().add(new SnowBookDupe());
        Modules.get().add(new SnowMiniGun());
        Modules.get().add(new SnowSafe());
        Modules.get().add(new SnowElytraDurability());
        Modules.get().add(new SnowElytraFlyPlusPlusPlus());
        Modules.get().add(new SnowMsgLag());
        Modules.get().add(new SnowCracker());
        Modules.get().add(new SnowSuperCrash());
        Modules.get().add(new SnowAlarms());
        Modules.get().add(new SnowAAC());
        Modules.get().add(new SnowPatch());
        Modules.get().add(new SnowLive());
        Modules.get().add(new SnowSpearKill());
        Modules.get().add(new SnowCDD());
        Modules.get().add(new SnowGlow());
        Modules.get().add(new SnowInvMove());
        Modules.get().add(new SnowFetch());

        increaseModuleSearchCount();
        applyAnarchyDefaults();

        Hud.get().register(SnowTab.INFO);
        Hud.get().register(SnowWatermark.INFO);
        Hud.get().register(SnowLogger.INFO);
        Hud.get().register(SnowFriendly.INFO);
        Hud.get().register(SnowFriends.INFO);
        Hud.get().register(SnowNotifs.INFO);
        Hud.get().register(SnowArrayList.INFO);
        Hud.get().register(SnowLogo.INFO);

        GuiThemes.add(new DookinqqGuiTheme());
        // GuiThemes.add(new GumballGardassGuiTheme());
        GuiThemes.select("Dookinqq");

        applyThemeSyncToModulesAndHud();
        ensurePrefixAlwaysEnabled();
    }

    private void increaseModuleSearchCount() {
        try {
            Setting<Integer> search = Config.get().moduleSearchCount;

            // Increase UI range for module search count.
            patchIntField(search, "sliderMax", 24);

            // Raise default and current value.
            patchObjectField(search, Setting.class, "defaultValue", 14);
            search.set(14);
        } catch (Throwable t) {
            LOG.warn("Failed to patch module search count defaults.", t);
        }
    }

    private void applyThemeSyncToModulesAndHud() {
        for (Module module : Modules.get().getGroup(CATEGORY)) {
            syncThemeForSettings(module.settings, false);
        }

        for (HudElement element : Hud.get()) {
            syncThemeForSettings(element.settings, true);
        }
    }

    private void syncThemeForSettings(Settings settings, boolean hudElement) {
        for (SettingGroup group : settings) {
            String groupName = group.name.toLowerCase(Locale.ROOT);
            for (Setting<?> setting : group) {
                Object raw = setting.get();
                if (!(raw instanceof SettingColor color)) continue;

                String settingName = setting.name.toLowerCase(Locale.ROOT);
                boolean colorish = settingName.contains("color");
                boolean renderish = groupName.contains("render") || groupName.contains("color") || groupName.contains("colors");

                if (!(colorish || renderish || hudElement)) continue;

                SettingColor synced = getSyncedThemeColor(settingName, color.a);
                setColor(setting, synced);
            }
        }
    }

    private SettingColor getSyncedThemeColor(String settingName, int alpha) {
        boolean secondary = settingName.contains("gradient color 2")
            || settingName.contains("color2")
            || settingName.contains("-end")
            || settingName.contains("end-color")
            || settingName.contains("side-color")
            || settingName.contains("separator")
            || settingName.contains("background");

        if (secondary) {
            return new SettingColor(THEME_BOTTOM_R, THEME_BOTTOM_G, THEME_BOTTOM_B, alpha);
        }

        return new SettingColor(THEME_TOP_R, THEME_TOP_G, THEME_TOP_B, alpha);
    }

    private void ensurePrefixAlwaysEnabled() {
        SnowPrefix prefix = Modules.get().get(SnowPrefix.class);
        if (prefix != null) {
            if (!prefix.isActive()) prefix.toggle();
            prefix.applyPrefix();
        }
    }

    private void applyAnarchyDefaults() {
        for (Module module : Modules.get().getGroup(CATEGORY)) {
            for (SettingGroup group : module.settings) {
                for (Setting<?> setting : group) {
                    if (setting.wasChanged()) continue;
                    applyAnarchyDefault(module, setting);
                }
            }
        }
    }

    private void applyAnarchyDefault(Module module, Setting<?> setting) {
        String name = setting.name.toLowerCase(Locale.ROOT);
        Object value = setting.get();

        if (value instanceof Boolean b) {
            if (name.contains("attack-friends")) setBool(setting, false);
            else if (name.contains("require-line-of-sight")) setBool(setting, true);
            else if (name.equals("blink-lunge")) setBool(setting, false);
            else if (name.equals("attack-spam")) setBool(setting, false);
            else if (name.equals("auto-switch-spear")) setBool(setting, true);
            else if (name.equals("auto-hold-right-click")) setBool(setting, true);
            else if (name.equals("auto-hover")) setBool(setting, true);
            else if (name.contains("spam") && b) setBool(setting, false);
            return;
        }

        if (value instanceof Integer i) {
            int target = i;

            if (name.equals("attack-delay")) target = 3;
            else if (name.equals("lunge-delay")) target = 10;
            else if (name.contains("max-buffered-packets")) target = Math.min(i, 160);
            else if (name.contains("packets") && i > 400) target = 220;
            else if (name.contains("render-distance")) target = (int) clamp(i, 24, 48);
            else if ((name.contains("target") || name.contains("attack")) && (name.contains("range") || name.contains("distance"))) {
                target = (int) clamp(i, 6, 18);
            } else if ((name.contains("delay") || name.contains("cooldown")) && i > 40) {
                target = 12;
            }

            if (target != i) setInt(setting, target);
            return;
        }

        if (value instanceof Double d) {
            double target = d;

            if (name.equals("max-targeting-range")) target = Math.min(d, 24.0);
            else if ((name.contains("target") || name.contains("attack")) && (name.contains("range") || name.contains("distance"))) {
                target = clamp(d, 4.5, 24.0);
            } else if (name.equals("flush-range")) {
                target = clamp(d, 2.2, 4.0);
            } else if (name.equals("force-flush-distance")) {
                target = clamp(d, 6.0, 10.0);
            } else if (name.equals("distance-boost")) {
                target = Math.min(d, 1.75);
            } else if (name.equals("spear-velocity")) {
                target = Math.min(d, 3.6);
            } else if (name.equals("lunge-strength")) {
                target = Math.min(d, 1.4);
            } else if (name.contains("packets") && d > 400.0) {
                target = 220.0;
            }

            if (Math.abs(target - d) > 1e-6) setDouble(setting, target);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setBool(Setting<?> setting, boolean value) {
        ((Setting<Boolean>) setting).set(value);
    }

    @SuppressWarnings("unchecked")
    private static void setInt(Setting<?> setting, int value) {
        ((Setting<Integer>) setting).set(value);
    }

    @SuppressWarnings("unchecked")
    private static void setDouble(Setting<?> setting, double value) {
        ((Setting<Double>) setting).set(value);
    }

    @SuppressWarnings("unchecked")
    private static void setColor(Setting<?> setting, SettingColor value) {
        ((Setting<SettingColor>) setting).set(value);
    }

    private static void patchIntField(Object target, String fieldName, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    private static void patchObjectField(Object target, Class<?> owner, String fieldName, Object value) throws Exception {
        Field f = owner.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}



