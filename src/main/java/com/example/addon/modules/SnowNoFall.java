package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class SnowNoFall extends Module {

    private boolean waitingForGround = false;

    public SnowNoFall() {
        super(AddonTemplate.CATEGORY, "Snow No Fall", "Grim 2.3.7.1 NoFall bypass (1.9+ style).");
    }

    @Override
    public void onActivate() {
        waitingForGround = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // If falling enough
        if (!mc.player.isOnGround() && mc.player.fallDistance >= 2.5f) {

            // Force jump like original LiquidBounce mode
            mc.options.jumpKey.setPressed(true);

            waitingForGround = true;
        }

        // When we land, send spoofed ground packet
        if (waitingForGround && mc.player.isOnGround()) {
            mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.OnGroundOnly(
                    true,
                    mc.player.horizontalCollision
                )
            );

            waitingForGround = false;
        }
    }
}