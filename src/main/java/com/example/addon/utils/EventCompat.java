package com.example.addon.utils;

import meteordevelopment.meteorclient.utils.misc.input.KeyAction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EventCompat {
    private EventCompat() {}

    public static int getButton(Object evt) {
        if (evt == null) return -1;
        try {
            Field f = evt.getClass().getDeclaredField("button");
            f.setAccessible(true);
            return f.getInt(evt);
        } catch (Exception ignored) {}
        try {
            Method m = evt.getClass().getMethod("getButton");
            Object o = m.invoke(evt);
            if (o instanceof Number) return ((Number)o).intValue();
        } catch (Exception ignored) {}
        return -1;
    }

    public static boolean isPress(Object evt) {
        if (evt == null) return false;
        try {
            Field f = evt.getClass().getDeclaredField("action");
            f.setAccessible(true);
            Object val = f.get(evt);
            if (val == null) return false;
            if (val instanceof KeyAction) return val == KeyAction.Press;
            String s = val.toString().toUpperCase();
            return s.contains("PRESS");
        } catch (Exception ignored) {}
        try {
            Method m = evt.getClass().getMethod("getAction");
            Object val = m.invoke(evt);
            if (val == null) return false;
            if (val instanceof KeyAction) return val == KeyAction.Press;
            return val.toString().toUpperCase().contains("PRESS");
        } catch (Exception ignored) {}
        return false;
    }

    public static void cancel(Object evt) {
        if (evt == null) return;
        try {
            Method m = evt.getClass().getMethod("cancel");
            m.invoke(evt);
            return;
        } catch (Exception ignored) {}
        try {
            Method m = evt.getClass().getMethod("setCancelled", boolean.class);
            m.invoke(evt, true);
        } catch (Exception ignored) {}
    }
}
