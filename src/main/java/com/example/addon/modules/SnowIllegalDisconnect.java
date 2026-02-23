package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

import java.time.Instant;
import java.util.BitSet;

public class SnowIllegalDisconnect extends Module {

    public SnowIllegalDisconnect() {
        super(AddonTemplate.CATEGORY, "Dookinqq Illegal Disconnect",
            "Attempts to interfere with disconnect handling.");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        try {
            BitSet invalidBits = new BitSet(10);
            invalidBits.set(0);
            invalidBits.set(9);

            ChatMessageC2SPacket packet = new ChatMessageC2SPacket(
                "§",
                Instant.now(),
                0L,
                null,
                null
            );

            mc.getNetworkHandler().sendPacket(packet);
        } catch (Exception ignored) {
        }
    }
}


