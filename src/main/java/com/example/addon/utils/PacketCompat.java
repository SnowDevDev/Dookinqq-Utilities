package com.example.addon.utils;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PacketCompat {
    private PacketCompat() {}

    public static Packet<?> createClickSlotPacket(Object... args) {
        try {
            Class<?> cls = ClickSlotC2SPacket.class;
            // try common constructors by parameter count
            for (Constructor<?> c : cls.getConstructors()) {
                if (c.getParameterCount() == args.length) {
                    try {
                        return (Packet<?>) c.newInstance(args);
                    } catch (Exception ignored) {}
                }
            }
            // try specific fallback: use a constructor without modifiedSlots
            for (Constructor<?> c : cls.getConstructors()) {
                if (c.getParameterCount() == 6) {
                    Object[] a6 = new Object[6];
                    System.arraycopy(args, 0, a6, 0, Math.min(6, args.length));
                    return (Packet<?>) c.newInstance(a6);
                }
            }
        } catch (Throwable t) {
        }
        return null;
    }

    public static ClientCommandC2SPacket createReleaseShiftPacket(PlayerEntity player) {
        try {
            Class<?> modeCls = ClientCommandC2SPacket.Mode.class;
            Object target = null;
            for (Object o : modeCls.getEnumConstants()) {
                if (o.toString().toUpperCase().contains("RELEASE") && o.toString().toUpperCase().contains("SHIFT")) {
                    target = o;
                    break;
                }
            }
            if (target == null) target = modeCls.getEnumConstants()[0];
            Constructor<?> ctor = ClientCommandC2SPacket.class.getConstructor(PlayerEntity.class, modeCls);
            return (ClientCommandC2SPacket) ctor.newInstance(player, target);
        } catch (Exception e) {
            // Could not construct via reflection; return null to allow callers to handle gracefully.
            return null;
        }
    }
}
