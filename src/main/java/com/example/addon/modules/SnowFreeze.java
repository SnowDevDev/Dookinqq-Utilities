package com.example.addon.modules;

import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import com.example.addon.AddonTemplate;

public class SnowFreeze extends Module {
    public SnowFreeze() {
        super(AddonTemplate.CATEGORY, "Dookinqq Freeze", "Freezes your position for server.");
    }

    private final SettingGroup FSettings = settings.getDefaultGroup();

    private final Setting<Boolean> SnowFreezeLook = FSettings.add(new BoolSetting.Builder()
        .name("SnowFreeze look")
        .description("Freezes your pitch and yaw.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> Packet = FSettings.add(new BoolSetting.Builder()
        .name("Packet mode")
        .description("Enable packet mode, better.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> SnowFreezeLookSilent = FSettings.add(new BoolSetting.Builder()
        .name("SnowFreeze look silent")
        .description("Freezes your pitch and yaw silently.")
        .defaultValue(true)
        .visible(() -> Packet.get() && SnowFreezeLook.get())
        .build()
    );

    private final Setting<Boolean> SnowFreezeLookPlace = FSettings.add(new BoolSetting.Builder()
        .name("SnowFreeze look place support")
        .description("Unfreezes yaw and pitch on place.")
        .defaultValue(true)
        .visible(SnowFreezeLookSilent::get)
        .build()
    );

    private float yaw = 0;
    private float pitch = 0;
    private Vec3d position = Vec3d.ZERO;

    private boolean rotate = false;

    @Override
    public void onActivate() {
        if (mc.player != null) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            position = com.example.addon.utils.Compat.getPos(mc.player); // FIXED
        }
    }

    private void setSnowFreezeLook(PacketEvent.Send event, PlayerMoveC2SPacket playerMove) {
        if (playerMove.changesLook() && SnowFreezeLook.get() && SnowFreezeLookSilent.get() && !rotate) {
            event.setCancelled(true);
        }
        else if (mc.player != null && playerMove.changesLook() && SnowFreezeLook.get() && !SnowFreezeLookSilent.get()) {
            event.setCancelled(true);
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }

        if (mc.player != null && playerMove.changesPosition()) {
            mc.player.setVelocity(0, 0, 0);
            mc.player.setPos(position.x, position.y, position.z);
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (mc.player != null && mc.getNetworkHandler() != null && SnowFreezeLookPlace.get()) {
PlayerMoveC2SPacket.LookAndOnGround packet =
    new PlayerMoveC2SPacket.LookAndOnGround(
        mc.player.getYaw(),
        mc.player.getPitch(),
        mc.player.isOnGround(),
        mc.player.horizontalCollision
    );
            rotate = true;
            mc.getNetworkHandler().sendPacket(packet);
            rotate = false;
        }
    }

    @EventHandler
    private void onMovePacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket playerMove) {
            if (Packet.get()) {
                setSnowFreezeLook(event, playerMove);
            }
        }
    }

    @EventHandler
    private void connectToServerEvent(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player != null) {
            mc.player.setVelocity(0, 0, 0);
            mc.player.setPos(position.x, position.y, position.z);
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        // FIXED: instead of using undefined variable "even"
        event.movement = Vec3d.ZERO;
    }

    @EventHandler
    private void remove(EntityRemovedEvent event) {
        if (event.entity == mc.player && isActive()) {
            toggle();
        }
    }
}


