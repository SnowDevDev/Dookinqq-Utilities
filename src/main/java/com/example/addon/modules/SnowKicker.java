package com.example.addon.modules;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;
import net.minecraft.network.packet.s2c.login.*;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;

public class SnowKicker extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ban = sgGeneral.add(new BoolSetting.Builder()
        .name("ban")
        .description("Whether to kick joining players.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> kickFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("kick-friends")
        .description("Whether to kick friends.")
        .defaultValue(false)
        .build()
    );

    private static final HashSet<GameProfile> processingPlayers = new HashSet<>();

    public SnowKicker() {
        super(Categories.Misc, "snow-kicker", "Kicks everyone on a cracked server.");
    }

    @Override
    public void onActivate() {
        AntiPacketKick apk = Modules.get().get(AntiPacketKick.class);
        if (apk != null && apk.isActive() && apk.logExceptions.get())
            info("Disable \"Log Exceptions\" in Anti Packet Kick if you don't want to get spammed!");
    }

    @EventHandler
    private void postTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;

        if (mc.isInSingleplayer()) {
            warning("Not available in singleplayer!");
            return;
        }

        Collection<PlayerListEntry> entries = mc.getNetworkHandler().getPlayerList();

        for (PlayerListEntry entry : entries) {
            kick(this, entry, kickFriends.get());
        }

        if (!ban.get()) toggle();
    }

    public static void kick(SnowKicker module, PlayerListEntry entry, boolean kickFriends) {
        MinecraftClient mc = module.mc;

        GameProfile profile = entry.getProfile();

        if (profile.equals(mc.player.getGameProfile())) return;
        if (Friends.get().isFriend(entry) && !kickFriends) return;
        if (processingPlayers.contains(profile)) return;

        processingPlayers.add(profile);

        InetSocketAddress address = (InetSocketAddress) mc.getNetworkHandler().getConnection().getAddress();
        ClientConnection connection = new ClientConnection(NetworkSide.CLIENTBOUND);

        CompletableFuture.runAsync(() -> {
            try {
                ClientConnection.connect(address, mc.options.shouldUseNativeTransport(), connection)
                    .get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                processingPlayers.remove(profile);
                connection.disconnect(Text.literal("Failed"));
                return;
            }

            connection.send(new LoginHelloC2SPacket(profile.getName(), profile.getId()));

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}

            connection.disconnect(Text.literal("Done"));
            processingPlayers.remove(profile);
        });
    }
}