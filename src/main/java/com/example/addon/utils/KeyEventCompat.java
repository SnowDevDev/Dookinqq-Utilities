package com.example.addon.utils;

public final class KeyEventCompat {
    private KeyEventCompat() {}

    public static int getKey(Object e) {
        if (e == null) return -1;
        try {
            java.lang.reflect.Field f = e.getClass().getDeclaredField("key");
            f.setAccessible(true);
            Object o = f.get(e);
            if (o instanceof Number) return ((Number)o).intValue();
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Method m = e.getClass().getMethod("getKey");
            Object o = m.invoke(e);
            if (o instanceof Number) return ((Number)o).intValue();
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Method m2 = e.getClass().getMethod("getKeyCode");
            Object o = m2.invoke(e);
            if (o instanceof Number) return ((Number)o).intValue();
        } catch (Exception ignored) {}
        return -1;
    }
}
