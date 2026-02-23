package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class SnowGrimFlight extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // ================= MODE =================

    public enum Mode {
        Grim2859V,
        Grim2373Jan15
    }

    private final Setting<Mode> mode = sgGeneral.add(
        new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Grim2859V)
            .build()
    );

    // ================= SETTINGS =================

    private final Setting<Integer> toggleTicks = sgGeneral.add(
        new IntSetting.Builder()
            .name("auto-disable-ticks")
            .defaultValue(0)
            .range(0, 200)
            .visible(() -> mode.get() == Mode.Grim2859V)
            .build()
    );

    private final Setting<Double> timerSpeed = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("timer")
            .defaultValue(0.446)
            .range(0.1, 1.0)
            .visible(() -> mode.get() == Mode.Grim2859V)
            .build()
    );

    private final Setting<Boolean> autoLag = sgGeneral.add(
        new BoolSetting.Builder()
            .name("auto-lag-in-air")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.Grim2373Jan15)
            .build()
    );

    private final Setting<Integer> airTick = sgGeneral.add(
        new IntSetting.Builder()
            .name("air-tick")
            .defaultValue(3)
            .range(0, 20)
            .visible(() -> mode.get() == Mode.Grim2373Jan15)
            .build()
    );

    // ================= STATE =================

    private int ticks;
    private Vec3d storedPos;

    private boolean started;
    private boolean shouldDelay;

    public SnowGrimFlight() {
        super(AddonTemplate.CATEGORY, "Dookinqq Grim Flight", "Grim fly modes.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        storedPos = null;
        started = false;
        shouldDelay = false;
    }

    @Override
    public void onDeactivate() {
        if (storedPos != null && mc.player != null) {
            mc.player.setPosition(storedPos);
        }

        Timer timerModule = Modules.get().get(Timer.class);
timerModule.setOverride(1);

        ticks = 0;
        storedPos = null;
        started = false;
        shouldDelay = false;
    }

    // ================= TICK =================

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (mode.get() == Mode.Grim2859V) {
            handle2859V();
        }
    }

    private void handle2859V() {
        if (ticks == 0) {
            mc.player.jump();
        }

        if (ticks <= 5) {
            Timer timerModule = Modules.get().get(Timer.class);
timerModule.setOverride(timerSpeed.get());
        } else {
            Timer timerModule = Modules.get().get(Timer.class);
timerModule.setOverride(1);
        }

        if (toggleTicks.get() != 0 && ticks >= toggleTicks.get()) {
            toggle();
        }

        ticks++;
    }

    // ================= WORLD TICK =================

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (mode.get() == Mode.Grim2859V) {
            if (ticks >= 2) {
                storedPos = com.example.addon.utils.Compat.getPos(mc.player);
                mc.player.setPosition(
                    storedPos.x + 1152,
                    storedPos.y,
                    storedPos.z + 1152
                );
            }
        }

        if (mode.get() == Mode.Grim2373Jan15) {
            handle2373Pre();
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (mc.player == null) return;

        if (mode.get() == Mode.Grim2859V && storedPos != null) {
            mc.player.setPosition(storedPos);
        }

        if (mode.get() == Mode.Grim2373Jan15 && started) {
            sendFallFlying();
        }
    }

    private void handle2373Pre() {
        if (started) return;

        if ((autoLag.get() && mc.player.fallDistance > airTick.get()) || shouldDelay) {
            started = true;
            sendFallFlying();
        }
    }

    // ================= PACKETS =================

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (mode.get() != Mode.Grim2373Jan15) return;

        Packet<?> packet = event.packet;

        if (packet instanceof EntityVelocityUpdateS2CPacket vel) {
            if (vel.getEntityId() == mc.player.getId()) {
                shouldDelay = true;
            }
        }

        if (shouldDelay) {

            if (packet instanceof PlayerPositionLookS2CPacket) {
                shouldDelay = false;
            }
        }
    }

    // ================= FALL FLY =================

    private void sendFallFlying() {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(
            new ClientCommandC2SPacket(
                mc.player,
                ClientCommandC2SPacket.Mode.START_FALL_FLYING
            )
        );
    }
}


