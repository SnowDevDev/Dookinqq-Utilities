package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.RenderUtil;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class SnowTab extends HudElement {
    private static final SettingColor THEME_TOP = new SettingColor(0, 128, 255, 255);
    private static final SettingColor THEME_BOTTOM = new SettingColor(0, 64, 128, 255);
    private static final SettingColor THEME_BACKGROUND = new SettingColor(0, 20, 44, 116);
    private static final SettingColor THEME_NORMAL = new SettingColor(224, 224, 224, 255);

    public static final HudElementInfo<SnowTab> INFO =
        new HudElementInfo<>(
            AddonTemplate.HUD_GROUP,
            "Dookinqq Tab",
            "styled tab GUI",
            SnowTab::new
        );

    private final List<Category> categories = new ArrayList<>();
    private int selectedCategory = 0;
    private int selectedModule = 0;
    private boolean expanded = false;

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<SettingColor> selected = sg.add(
        new ColorSetting.Builder()
            .name("selected")
            .defaultValue(new SettingColor(0, 128, 255, 255))
            .build()
    );

    private final Setting<SettingColor> normal = sg.add(
        new ColorSetting.Builder()
            .name("normal")
            .defaultValue(new SettingColor(224, 224, 224, 255))
            .build()
    );

    private final Setting<SettingColor> background = sg.add(
        new ColorSetting.Builder()
            .name("background")
            .defaultValue(new SettingColor(0, 20, 44, 200))
            .build()
    );

    private final Setting<SettingColor> border = sg.add(
        new ColorSetting.Builder()
            .name("border")
            .defaultValue(new SettingColor(0, 64, 128, 170))
            .build()
    );

    public SnowTab() {
        super(INFO);
        Modules.loopCategories().forEach(categories::add);
        syncPalette();
    }

    @Override
    public void render(HudRenderer renderer) {
        if (categories.isEmpty()) return;

        syncPalette();

        int padding = 6;
        int catHeight = 18;
        int modHeight = 16;

        Category category = categories.get(selectedCategory);
        List<Module> modules = Modules.get().getGroup(category);

        double catWidth = categories.stream()
            .mapToDouble(c -> renderer.textWidth(c.name))
            .max()
            .orElse(40) + padding * 2;

        double totalWidth = catWidth;
        double totalHeight = categories.size() * catHeight + padding;

        if (expanded && !modules.isEmpty()) {
            double modWidth = modules.stream()
                .mapToDouble(m -> renderer.textWidth(m.title))
                .max()
                .orElse(40) + padding * 2;

            totalWidth += modWidth + 10;
            totalHeight = Math.max(totalHeight, modules.size() * modHeight + padding);
        }

        renderer.quad(x, y, totalWidth, totalHeight, background.get());
        RenderUtil.renderRoundedOutline(renderer, x, y, totalWidth, totalHeight, 4, 1.0, border.get(), background.get());

        int sy = y + padding;

        for (int i = 0; i < categories.size(); i++) {
            renderer.text(
                categories.get(i).name,
                x + padding,
                sy,
                i == selectedCategory ? selected.get() : normal.get(),
                false
            );
            sy += catHeight;
        }

        if (expanded && !modules.isEmpty()) {
            int rx = (int) (x + catWidth + 10);
            int ry = y + padding;

            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);

                renderer.text(
                    i == selectedModule ? m.title + " <" : m.title,
                    rx + padding,
                    ry,
                    m.isActive() ? selected.get() : normal.get(),
                    false
                );

                ry += modHeight;
            }
        }

        setSize((int) totalWidth, (int) totalHeight);
    }

    @EventHandler
    private void onKey(KeyEvent e) {
        if (e.action != KeyAction.Press || categories.isEmpty()) return;

        if (!expanded) {
            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 264) // Down
                selectedCategory = (selectedCategory + 1) % categories.size();

            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 265) // Up
                selectedCategory = (selectedCategory - 1 + categories.size()) % categories.size();

            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 262) // Right
                expanded = true;
        } else {
            List<Module> mods = Modules.get().getGroup(categories.get(selectedCategory));
            if (mods.isEmpty()) return;

            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 264)
                selectedModule = (selectedModule + 1) % mods.size();

            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 265)
                selectedModule = (selectedModule - 1 + mods.size()) % mods.size();

            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 262)
                mods.get(selectedModule).toggle();

            if (com.example.addon.utils.KeyEventCompat.getKey(e) == 263) { // Left
                expanded = false;
                selectedModule = 0;
            }
        }
    }

    private void syncPalette() {
        SettingColor selectedColor = selected.get();
        selectedColor.set(new SettingColor(THEME_TOP.r, THEME_TOP.g, THEME_TOP.b, selectedColor.a));

        SettingColor normalColor = normal.get();
        normalColor.set(new SettingColor(THEME_NORMAL.r, THEME_NORMAL.g, THEME_NORMAL.b, normalColor.a));

        SettingColor backgroundColor = background.get();
        backgroundColor.set(new SettingColor(THEME_BACKGROUND.r, THEME_BACKGROUND.g, THEME_BACKGROUND.b, backgroundColor.a));

        SettingColor borderColor = border.get();
        borderColor.set(new SettingColor(THEME_BOTTOM.r, THEME_BOTTOM.g, THEME_BOTTOM.b, borderColor.a));
    }
}
