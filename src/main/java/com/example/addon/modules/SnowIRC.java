package com.example.addon.modules;

import com.google.gson.JsonObject;
import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class SnowIRC extends Module {
    public static final SnowIRC INSTANCE = new SnowIRC();

    private SnowIRC(){
        super(AddonTemplate.CATEGORY, "Snow IRC", "snow addon SnowIRC");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> sendPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("Prefix")
        .defaultValue("!")
        .build());

    @EventHandler
    public void onSendMessageEvent(SendMessageEvent event) {
        if (sendPrefix.get().isEmpty()) sendPrefix.reset();
        if (event.message.startsWith(sendPrefix.get())) {
            JsonObject message = new JsonObject();
            message.addProperty("msg", event.message.substring(sendPrefix.get().length()));
            info("IRC fis broken rn");
            event.cancel();
        }
    }
}
