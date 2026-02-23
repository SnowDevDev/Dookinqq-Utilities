package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.PacketCompat;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;

import static org.lwjgl.glfw.GLFW.*;

public class SnowInvMove extends Module {

    public SnowInvMove() {
        super(AddonTemplate.CATEGORY, "dookinqq-inv-move", "Move inside GUIs.");
    }

    // --------------------------------------------------
    // Settings
    // --------------------------------------------------

    public enum Bypass {
        No_Open_Packet,
        None
    }

    public enum NoSprint {
        Real,
        Packet_Spoof,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> twoB2T = sgGeneral.add(new BoolSetting.Builder()
        .name("2b2t")
        .defaultValue(true)
        .build()
    );

    private final Setting<Bypass> bypass = sgGeneral.add(new EnumSetting.Builder<Bypass>()
        .name("bypass")
        .defaultValue(Bypass.No_Open_Packet)
        .build()
    );

    private final Setting<NoSprint> noSprint = sgGeneral.add(new EnumSetting.Builder<NoSprint>()
        .name("no-sprint")
        .defaultValue(NoSprint.Packet_Spoof)
        .build()
    );

    private final Setting<Boolean> noMoveClicks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-move-clicks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("arrows-rotate")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate-speed")
        .defaultValue(4)
        .min(0)
        .build()
    );

    // --------------------------------------------------
    // Held Key System
    // --------------------------------------------------

    private boolean forwardHeld, backHeld, leftHeld, rightHeld, jumpHeld;

    @Override
    public void onDeactivate() {
        resetKeys();
    }

    // --------------------------------------------------
    // Packet Handling
    // --------------------------------------------------

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof ClientCommandC2SPacket packet) {
            if (packet.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY
                && bypass.get() == Bypass.No_Open_Packet) {

                if (noSprint.get() == NoSprint.Packet_Spoof) spoofStop();
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!twoB2T.get()) return;

        if (event.packet instanceof CloseScreenS2CPacket packet) {
            if (mc.player != null && packet.getSyncId() == mc.player.playerScreenHandler.syncId) {
                event.cancel();
            }
        }
    }

    private void spoofStop() {
        if (mc.player.isSprinting())
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

        if (mc.player.isSneaking())
            mc.player.networkHandler.sendPacket(
                PacketCompat.createReleaseShiftPacket(mc.player));
    }

    // --------------------------------------------------
    // Movement Logic
    // --------------------------------------------------

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (!canMove()) {
            resetKeys();
            return;
        }

        // Track current key presses
        forwardHeld = mc.options.forwardKey.isPressed();
        backHeld = mc.options.backKey.isPressed();
        leftHeld = mc.options.leftKey.isPressed();
        rightHeld = mc.options.rightKey.isPressed();
        jumpHeld = mc.options.jumpKey.isPressed();

        updateHeldKeys();

        if (arrowsRotate.get()) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            for (int i = 0; i < rotateSpeed.get() * 2; i++) {
                if (isKeyPressed(GLFW_KEY_LEFT)) yaw -= 0.5f;
                if (isKeyPressed(GLFW_KEY_RIGHT)) yaw += 0.5f;
                if (isKeyPressed(GLFW_KEY_UP)) pitch -= 0.5f;
                if (isKeyPressed(GLFW_KEY_DOWN)) pitch += 0.5f;
            }

            pitch = Math.max(-90, Math.min(90, pitch));
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private void updateHeldKeys() {
        set(mc.options.forwardKey, forwardHeld);
        set(mc.options.backKey, backHeld);
        set(mc.options.leftKey, leftHeld);
        set(mc.options.rightKey, rightHeld);
        set(mc.options.jumpKey, jumpHeld);
    }

    private void set(KeyBinding bind, boolean pressed) {
        bind.setPressed(pressed);
    }

    private void resetKeys() {
        forwardHeld = backHeld = leftHeld = rightHeld = jumpHeld = false;

        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    private boolean isKeyPressed(int key) {
        return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW_PRESS;
    }

    // --------------------------------------------------
    // GUI Filter
    // --------------------------------------------------

    private boolean canMove() {
        if (mc.currentScreen == null) return true;

        return !(mc.currentScreen instanceof ChatScreen
            || mc.currentScreen instanceof SignEditScreen
            || mc.currentScreen instanceof AnvilScreen
            || mc.currentScreen instanceof AbstractCommandBlockScreen
            || mc.currentScreen instanceof StructureBlockScreen
            || mc.currentScreen instanceof CreativeInventoryScreen);
    }
}