package com.example.addon.utils;

public class Notification {
    private final String message;
    private final long creation;
    public double slideOffset = 0;
    public double displayY = 0;

    public Notification(String message) {
        this.message = message;
        creation = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public long getDelta() {
        return System.currentTimeMillis() - creation;
    }
}
