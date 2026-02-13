package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.x150.renderer.render.Renderer2d;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.EVENT_BUS;

public class SnowTab extends HudElement {

    public static final HudElementInfo<SnowTab> INFO =
            new HudElementInfo<>(AddonTemplate.HUD_GROUP,
                    "snow-tab",
                    "Renders SnowTab",
                    SnowTab::new);

    private final List<Category> categories;
    private int selectedCategoryIndex = 0;
    private int selectedModuleIndex = 0;
    private boolean isExpanded = false;

    private final String longestCategoryName;
    private final Object2ObjectOpenHashMap<Category, String> longestModuleNameInCategoryMap = new Object2ObjectOpenHashMap<>();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public SnowTab() {
        super(INFO);
        EVENT_BUS.subscribe(this);

        List<Category> temp = new ArrayList<>();
        Modules.loopCategories().forEach(temp::add);
        this.categories = temp;

        this.longestCategoryName = getLongestCategoryName();
        this.categories.forEach(category ->
                longestModuleNameInCategoryMap.put(category, getLongestModuleNameInCategory(category))
        );
    }

    /* ---------------- SETTINGS ---------------- */

    private final Setting<SettingColor> selectedCategoryColor = sgGeneral.add(
            new ColorSetting.Builder()
                    .name("selected-category")
                    .defaultValue(new SettingColor(255, 105, 180))
                    .build()
    );

    private final Setting<SettingColor> unSelectedCategoryColor = sgGeneral.add(
            new ColorSetting.Builder()
                    .name("unselected-category")
                    .defaultValue(new SettingColor(255, 255, 255))
                    .build()
    );

    private final Setting<SettingColor> enabledModuleColor = sgGeneral.add(
            new ColorSetting.Builder()
                    .name("enabled-module")
                    .defaultValue(new SettingColor(255, 105, 180))
                    .build()
    );

    private final Setting<SettingColor> disabledModuleColor = sgGeneral.add(
            new ColorSetting.Builder()
                    .name("disabled-module")
                    .defaultValue(new SettingColor(192, 192, 192))
                    .build()
    );

    private final Setting<SettingColor> snowTabBackground = sgGeneral.add(
            new ColorSetting.Builder()
                    .name("background")
                    .defaultValue(new SettingColor(18, 18, 18, 200))
                    .build()
    );

    private final Setting<SettingColor> snowTabBorder = sgGeneral.add(
            new ColorSetting.Builder()
                    .name("border")
                    .defaultValue(new SettingColor(160, 160, 160, 140)) // softer = looks thinner
                    .build()
    );

    /* ---------------- HELPERS ---------------- */

    private String getLongestCategoryName() {
        return categories.stream()
                .map(category -> category.name)
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private String getLongestModuleNameInCategory(Category category) {
        return Modules.get().getGroup(category).stream()
                .map(module -> module.title)
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }

    /* ---------------- RENDER ---------------- */

    @Override
    public void render(HudRenderer renderer) {

        int sx = x;
        int sy = y;

        final int categoryHeight = 20;
        final int moduleHeight = 16;

        double categoryWidth = renderer.textWidth(longestCategoryName) * 1.2;

        /* ---- CATEGORY BACKGROUND ---- */

        renderer.quad(
                x - 5,
                y - 5,
                categoryWidth,
                (categories.size() * categoryHeight) + 8,
                snowTabBackground.get()
        );

        Renderer2d.renderRoundedOutline(
                renderer.drawContext.getMatrices(),
                new java.awt.Color(
                        snowTabBorder.get().r,
                        snowTabBorder.get().g,
                        snowTabBorder.get().b,
                        snowTabBorder.get().a
                ),
                x - 5,
                y - 5,
                (x - 5) + categoryWidth,
                (y - 5) + (categories.size() * categoryHeight) + 8,
                3f,
                3f,
                3f
        );

        /* ---- CATEGORY TEXT ---- */

        for (Category category : categories) {

            boolean isSelected = categories.get(selectedCategoryIndex) == category;

            Color textColor = isSelected
                    ? selectedCategoryColor.get()
                    : unSelectedCategoryColor.get();

            renderer.text(category.name, sx, sy, textColor, false);

            if (isExpanded && isSelected) {

                List<Module> modules = Modules.get().getGroup(category);
                if (modules.isEmpty()) break;

                String longestModule = longestModuleNameInCategoryMap.get(category);

                int rx = (int) (categoryWidth + x);
                int ry = sy + 3;

                double moduleWidth = renderer.textWidth(longestModule) * 1.2;

                /* ---- MODULE BACKGROUND ---- */

                renderer.quad(
                        rx - 5,
                        sy,
                        moduleWidth,
                        (modules.size() * moduleHeight) + 6,
                        snowTabBackground.get()
                );

                Renderer2d.renderRoundedOutline(
                        renderer.drawContext.getMatrices(),
                        new java.awt.Color(
                                snowTabBorder.get().r,
                                snowTabBorder.get().g,
                                snowTabBorder.get().b,
                                snowTabBorder.get().a
                        ),
                        rx - 5,
                        sy,
                        (rx - 5) + moduleWidth,
                        sy + (modules.size() * moduleHeight) + 6,
                        3f,
                        3f,
                        3f
                );

                /* ---- MODULE TEXT ---- */

                for (int i = 0; i < modules.size(); i++) {

                    Module m = modules.get(i);
                    boolean isSelectedModule = (i == selectedModuleIndex);

                    Color moduleColor = m.isActive()
                            ? enabledModuleColor.get()
                            : disabledModuleColor.get();

                    String title = isSelectedModule
                            ? m.title + " <"
                            : m.title;

                    renderer.text(title, rx, ry, moduleColor, false);

                    ry += moduleHeight;
                }
            }

            sy += categoryHeight;
        }

        setSize((int) categoryWidth + 10, sy - y);
    }

    /* ---------------- INPUT ---------------- */

    @EventHandler
    public void onKeyEvent(KeyEvent event) {

        if (!event.action.equals(KeyAction.Press)) return;

        if (event.key == 264 && !isExpanded)
            selectedCategoryIndex = (selectedCategoryIndex + 1) % categories.size();

        if (event.key == 265 && !isExpanded)
            selectedCategoryIndex = (selectedCategoryIndex - 1 + categories.size()) % categories.size();

        if (event.key == 262 && isExpanded)
            Modules.get().getGroup(categories.get(selectedCategoryIndex))
                    .get(selectedModuleIndex)
                    .toggle();

        if (event.key == 262 && !isExpanded)
            isExpanded = true;

        if (event.key == 263 && isExpanded) {
            isExpanded = false;
            selectedModuleIndex = 0;
        }

        if (event.key == 265 && isExpanded) {
            List<Module> mods = Modules.get().getGroup(categories.get(selectedCategoryIndex));
            selectedModuleIndex = (selectedModuleIndex - 1 + mods.size()) % mods.size();
        }

        if (event.key == 264 && isExpanded) {
            List<Module> mods = Modules.get().getGroup(categories.get(selectedCategoryIndex));
            selectedModuleIndex = (selectedModuleIndex + 1) % mods.size();
        }
    }
}