package com.example.addon;

import com.example.addon.hud.SnowWatermark;
import com.example.addon.hud.SnowLogger;
import com.example.addon.modules.SnowAscend;
import com.example.addon.modules.SnowCrash;
import com.example.addon.modules.SnowDupe;
import com.example.addon.modules.SnowFetch;
import com.example.addon.modules.SnowMIK;
import com.example.addon.modules.SnowRIK;
import com.example.addon.modules.SnowPrefix;
import com.example.addon.modules.SnowBaseESP;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Snow Utilities");
    public static final HudGroup HUD_GROUP = new HudGroup("Snow Utilities");

    @Override
    public void onInitialize() {
        LOG.info("succesfully winged the ding");

        // Modules
        Modules.get().add(new SnowAscend());
		Modules.get().add(new SnowCrash());
		Modules.get().add(new SnowDupe());
		Modules.get().add(new SnowFetch());
		Modules.get().add(new SnowMIK());
		Modules.get().add(new SnowRIK());
		Modules.get().add(new SnowBaseESP());
		Modules.get().add(new SnowPrefix());


        // HUD
        Hud.get().register(SnowWatermark.INFO);
		Hud.get().register(SnowLogger.INFO);
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
