package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class SnowNoFall extends Module {

    public SnowNoFall() {
        super(AddonTemplate.CATEGORY, "Snow No Fall", "Grim / 2b2t NoFall bypass.");
    }

    // --------------------------------------------------
    // Settings
    // --------------------------------------------------

    public enum Mode {
        Grim2b2t,
        JumpSpoof
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .defaultValue(Mode.Grim2b2t)
        .build()
    );

    private boolean waitingForGround = false;

    @Override
    public void onActivate() {
        waitingForGround = false;
    }

    // --------------------------------------------------
    // JumpSpoof Mode (your Snow method)
    // --------------------------------------------------

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mode.get() != Mode.JumpSpoof) return;

        if (!mc.player.isOnGround() && mc.player.fallDistance >= 2.5f) {
            mc.options.jumpKey.setPressed(true);
            waitingForGround = true;
        }

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

    // --------------------------------------------------
    // Grim2b2t Mode (Lambda idea but fixed)
    // --------------------------------------------------

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (mc.player == null) return;
        if (mode.get() != Mode.Grim2b2t) return;

        if (mc.player.fallDistance < 3.0f) return;

        // Small Y offset spoof
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.Full(
                x,
                y + 1.0E-9,
                z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                true, // onGround spoof
                mc.player.horizontalCollision
            )
        );

        // Stop vertical motion to prevent real damage calc
        mc.player.setVelocity(
            mc.player.getVelocity().x,
            0,
            mc.player.getVelocity().z
        );

        mc.player.fallDistance = 0;
    }
}