package com.example.addon.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class Compat {
    private Compat() {}

    public static Vec3d getPos(Entity e) {
        if (e == null) return Vec3d.ZERO;
        try {
            Method m = e.getClass().getMethod("getPos");
            Object res = m.invoke(e);
            if (res instanceof Vec3d) return (Vec3d) res;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        // Fallback to coordinates (these getters should exist across mappings)
        try {
            Method gx = e.getClass().getMethod("getX");
            Method gy = e.getClass().getMethod("getY");
            Method gz = e.getClass().getMethod("getZ");
            double x = ((Number) gx.invoke(e)).doubleValue();
            double y = ((Number) gy.invoke(e)).doubleValue();
            double z = ((Number) gz.invoke(e)).doubleValue();
            return new Vec3d(x, y, z);
        } catch (Exception ex) {
            return new Vec3d(0, 0, 0);
        }
    }

    public static int getSelectedSlot(PlayerInventory inv) {
        if (inv == null) return 0;
        try {
            Field f = PlayerInventory.class.getDeclaredField("selectedSlot");
            f.setAccessible(true);
            return f.getInt(inv);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            try {
                Method m = PlayerInventory.class.getMethod("getSelectedSlot");
                return (int) m.invoke(inv);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ItemStack> getMain(PlayerInventory inv) {
        if (inv == null) return new ArrayList<>();
        try {
            Field f = PlayerInventory.class.getDeclaredField("main");
            f.setAccessible(true);
            Object o = f.get(inv);
            if (o instanceof List) return (List<ItemStack>) o;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

        // Try method fallback
        try {
            Method m = PlayerInventory.class.getMethod("getMain");
            Object o = m.invoke(inv);
            if (o instanceof List) return (List<ItemStack>) o;
        } catch (Exception ignored) {}

        return new ArrayList<>();
    }

    public static String getProfileName(GameProfile profile) {
        if (profile == null) return "";
        try {
            Method m = profile.getClass().getMethod("getName");
            Object o = m.invoke(profile);
            if (o != null) return o.toString();
        } catch (Exception ignored) {}
        try {
            Field f = GameProfile.class.getDeclaredField("name");
            f.setAccessible(true);
            Object o = f.get(profile);
            if (o != null) return o.toString();
        } catch (Exception ignored) {}
        // Last resort: attempt other known methods/fields, otherwise empty
        try {
            Method m2 = profile.getClass().getMethod("getPlayerName");
            Object o = m2.invoke(profile);
            if (o != null) return o.toString();
        } catch (Exception ignored) {}
        return "";
    }

    public static String getProfileId(GameProfile profile) {
        if (profile == null) return "";
        try {
            Method m = profile.getClass().getMethod("getId");
            Object o = m.invoke(profile);
            if (o != null) return o.toString();
        } catch (Exception ignored) {}
        try {
            Field f = GameProfile.class.getDeclaredField("id");
            f.setAccessible(true);
            Object o = f.get(profile);
            if (o != null) return o.toString();
        } catch (Exception ignored) {}
        try {
            Method m2 = profile.getClass().getMethod("getUUID");
            Object o = m2.invoke(profile);
            if (o != null) return o.toString();
        } catch (Exception ignored) {}
        return "";
    }

    public static boolean isCollidable(Entity e) {
        if (e == null) return false;
        try {
            Method m = e.getClass().getMethod("isCollidable", Entity.class);
            Object o = m.invoke(e, e);
            if (o instanceof Boolean) return (Boolean) o;
        } catch (Exception ignored) {}
        try {
            Method m2 = e.getClass().getMethod("isCollidable");
            Object o = m2.invoke(e);
            if (o instanceof Boolean) return (Boolean) o;
        } catch (Exception ignored) {}
        return true;
    }

    public static boolean isPickaxe(net.minecraft.item.ItemStack stack) {
        if (stack == null) return false;
        try {
            Object item = stack.getItem();
            if (item == null) return false;
            Class<?> pickaxeClass = Class.forName("net.minecraft.item.PickaxeItem");
            return pickaxeClass.isInstance(item);
        } catch (ClassNotFoundException ignored) {
        }
        // Fallback: check class name
        try {
            String name = stack.getItem().getClass().getSimpleName().toLowerCase();
            return name.contains("pickaxe");
        } catch (Exception ignored) {}
        return false;
    }

    public static int getSyncId(Object packet) {
        if (packet == null) return -1;
        try {
            Method m = packet.getClass().getMethod("getSyncId");
            Object o = m.invoke(packet);
            if (o instanceof Number) return ((Number)o).intValue();
        } catch (Exception ignored) {}
        try {
            Field f = packet.getClass().getDeclaredField("syncId");
            f.setAccessible(true);
            Object o = f.get(packet);
            if (o instanceof Number) return ((Number)o).intValue();
        } catch (Exception ignored) {}
        return -1;
    }

    public static double getPrevX(Object player) {
        if (player == null) return 0;
        try {
            Field f = player.getClass().getDeclaredField("prevX");
            f.setAccessible(true);
            Object o = f.get(player);
            if (o instanceof Number) return ((Number)o).doubleValue();
        } catch (Exception ignored) {}
        try {
            Method m = player.getClass().getMethod("getPrevX");
            Object o = m.invoke(player);
            if (o instanceof Number) return ((Number)o).doubleValue();
        } catch (Exception ignored) {}
        return 0;
    }

    public static double getPrevZ(Object player) {
        if (player == null) return 0;
        try {
            Field f = player.getClass().getDeclaredField("prevZ");
            f.setAccessible(true);
            Object o = f.get(player);
            if (o instanceof Number) return ((Number)o).doubleValue();
        } catch (Exception ignored) {}
        try {
            Method m = player.getClass().getMethod("getPrevZ");
            Object o = m.invoke(player);
            if (o instanceof Number) return ((Number)o).doubleValue();
        } catch (Exception ignored) {}
        return 0;
    }

    public static void connectClient(Object address, boolean nativeTransport, Object connection) {
        try {
            Class<?> cc = Class.forName("net.minecraft.network.ClientConnection");
            for (Method m : cc.getMethods()) {
                if (!m.getName().equals("connect")) continue;
                try {
                    Object[] args = new Object[m.getParameterCount()];
                    if (m.getParameterCount() == 3) {
                        args[0] = address;
                        args[1] = nativeTransport;
                        args[2] = connection;
                    } else if (m.getParameterCount() == 2) {
                        args[0] = address;
                        args[1] = connection;
                    } else {
                        continue;
                    }
                    m.invoke(null, args);
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public static int getVillagerLevel(Object entity) {
        if (entity == null) return 0;
        try {
            Method gv = entity.getClass().getMethod("getVillagerData");
            Object data = gv.invoke(entity);
            if (data != null) {
                try {
                    Method gl = data.getClass().getMethod("getLevel");
                    Object o = gl.invoke(data);
                    if (o instanceof Number) return ((Number)o).intValue();
                } catch (Exception ignored) {}
                try {
                    Method gl2 = data.getClass().getMethod("level");
                    Object o = gl2.invoke(data);
                    if (o instanceof Number) return ((Number)o).intValue();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static Object getBlockConstant(String name) {
        try {
            Class<?> blocks = Class.forName("net.minecraft.block.Blocks");
            java.lang.reflect.Field f = blocks.getField(name);
            return f.get(null);
        } catch (Exception ignored) {}
        return null;
    }

    public static String getProfilePropertiesJson(com.mojang.authlib.GameProfile profile) {
        // Best-effort: try to obtain properties and serialize; fall back to empty array
        try {
            Method m = profile.getClass().getMethod("getProperties");
            Object props = m.invoke(profile);
            if (props != null) {
                try {
                    Method values = props.getClass().getMethod("values");
                    Object vals = values.invoke(props);
                    if (vals instanceof java.util.Collection) {
                        java.util.Collection<?> coll = (java.util.Collection<?>) vals;
                        java.util.List<String> out = new java.util.ArrayList<>();
                        for (Object o : coll) out.add(o.toString());
                        return new com.google.gson.Gson().toJson(out);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return "[]";
    }

    public static void sendLoginHello(Object connection, Object profile) {
        try {
            String name = getProfileName((com.mojang.authlib.GameProfile) profile);
            String idStr = getProfileId((com.mojang.authlib.GameProfile) profile);
            java.util.UUID id = null;
            try {
                id = java.util.UUID.fromString(idStr);
            } catch (Exception ignored) {}

            Class<?> pktClass = Class.forName("net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket");
            Object pkt = null;
            for (java.lang.reflect.Constructor<?> c : pktClass.getConstructors()) {
                try {
                    Class<?>[] pts = c.getParameterTypes();
                    if (pts.length == 2 && pts[0] == String.class && pts[1] == java.util.UUID.class && id != null) {
                        pkt = c.newInstance(name, id);
                        break;
                    } else if (pts.length == 2 && pts[0] == String.class && pts[1] == String.class) {
                        pkt = c.newInstance(name, idStr);
                        break;
                    } else if (pts.length == 1 && pts[0].isAssignableFrom(profile.getClass())) {
                        pkt = c.newInstance(profile);
                        break;
                    }
                } catch (Throwable ignored) {}
            }

            if (pkt != null) {
                try {
                    java.lang.reflect.Method m = connection.getClass().getMethod("send", Class.forName("net.minecraft.network.packet.Packet"));
                    m.invoke(connection, pkt);
                    return;
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method m2 = connection.getClass().getMethod("sendPacket", Object.class);
                    m2.invoke(connection, pkt);
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public static net.minecraft.util.math.Vec3d getCameraPos(Object gameRenderer) {
        try {
            Method getCamera = gameRenderer.getClass().getMethod("getCamera");
            Object camera = getCamera.invoke(gameRenderer);
            if (camera != null) {
                try {
                    Method gp = camera.getClass().getMethod("getPos");
                    Object p = gp.invoke(camera);
                    if (p instanceof net.minecraft.util.math.Vec3d) return (net.minecraft.util.math.Vec3d) p;
                } catch (Exception ignored) {}
                try {
                    Method gp2 = camera.getClass().getMethod("getPosition");
                    Object p = gp2.invoke(camera);
                    if (p instanceof net.minecraft.util.math.Vec3d) return (net.minecraft.util.math.Vec3d) p;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return net.minecraft.util.math.Vec3d.ZERO;
    }
}
