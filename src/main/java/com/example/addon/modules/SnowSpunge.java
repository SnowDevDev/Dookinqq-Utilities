// SnowSpunge.java
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.HandshakeC2SPacketAccessor;
import com.google.gson.Gson;
import com.mojang.authlib.properties.PropertyMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;

import java.util.List;

public class SnowSpunge extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private static final Gson GSON = new Gson();

    private final Setting<Boolean> whitelist = sgGeneral.add(new BoolSetting.Builder()
            .name("whitelist")
            .description("Use whitelist.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> whitelistedServers = sgGeneral.add(new StringListSetting.Builder()
            .name("whitelisted-servers")
            .description("Will only work if you joined the servers above.")
            .visible(whitelist::get)
            .build()
    );

    private final Setting<Boolean> spoofProfile = sgGeneral.add(new BoolSetting.Builder()
            .name("spoof-profile")
            .description("Spoof account profile.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> forwardedIP = sgGeneral.add(new StringSetting.Builder()
            .name("forwarded-IP")
            .description("The forwarded IP address.")
            .defaultValue("127.0.0.1")
            .build()
    );

    public SnowSpunge() {
        super(AddonTemplate.CATEGORY, "Dookinqq Spunge", "Join BungeeCord servers with optional spoofing.");
        runInMainMenu = true;
    }

@EventHandler
private void onPacketSend(PacketEvent.Send event) {
    if (!(event.packet instanceof HandshakeC2SPacket packet)) return;

    // Whitelist logic
    if (whitelist.get() && !whitelistedServers.get().contains(Utils.getWorldName())) return;

    HandshakeC2SPacketAccessor acc = (HandshakeC2SPacketAccessor) (Object) packet;

    String original = acc.getAddress();
    String address = original
            + "\0" + forwardedIP.get()
            + "\0" + com.example.addon.utils.Compat.getProfileId(mc.getNetworkHandler().getProfile()).replace("-", "")
            + (spoofProfile.get() ? getProperty() : "");

    acc.setAddress(address);
}

    private String getProperty() {
        return "\0" + com.example.addon.utils.Compat.getProfilePropertiesJson(mc.getNetworkHandler().getProfile());
    }
}


