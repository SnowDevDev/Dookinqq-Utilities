package com.example.addon;

import com.example.addon.themes.SnowGuiTheme;
import com.example.addon.hud.*;
import com.example.addon.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
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

        MeteorClient.EVENT_BUS.subscribe(this);

        // Modules
        Modules.get().add(new SnowAscend());
        Modules.get().add(new SnowCrash());
        Modules.get().add(new SnowDupe());
        Modules.get().add(new SnowFetch());
        Modules.get().add(new SnowMIK());
        Modules.get().add(new SnowRIK());
        Modules.get().add(new SnowBaseESP());
        Modules.get().add(new SnowNJD());
        Modules.get().add(new SnowDM());
        Modules.get().add(new SnowFatty());
        Modules.get().add(new SnowSniper());
        Modules.get().add(new SnowEncryption());
		Modules.get().add(new SnowAntiOpium());
		Modules.get().add(new SnowAHB());
		Modules.get().add(new SnowCapes());
		Modules.get().add(new SnowProjectile());
		Modules.get().add(new SnowReach());
		Modules.get().add(new SnowABB());
		Modules.get().add(new SnowABBr());
		Modules.get().add(new SnowGrimFlight());
		Modules.get().add(new SnowGFF());
        Modules.get().add(SnowIRC.INSTANCE);

        // HUD
        Hud.get().register(SnowWatermark.INFO);
        Hud.get().register(SnowLogger.INFO);
		
		// Themes
		GuiThemes.add(new SnowGuiTheme());
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