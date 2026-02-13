package com.example.addon.utils;

import com.mojang.text2speech.Narrator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.orbit.EventHandler;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    private static Clip beep;

    static {
        try {
            InputStream audioSrc = NotificationManager.class.getResourceAsStream("/assets/snow/beep.wav");

            if (audioSrc != null) {
                BufferedInputStream bufferedIn = new BufferedInputStream(audioSrc);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);

                beep = AudioSystem.getClip();
                beep.open(audioStream);
            } else {
                System.err.println("[Snow] Failed to load beep.wav");
            }

        } catch (Exception e) {
            System.err.println("[Snow] Error loading notification sound");
            e.printStackTrace();
        }
    }

    public static final NotificationManager INSTANCE = new NotificationManager();

    private NotificationManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private final List<Notification> notifications = new ArrayList<>();
    private final int maxAgeMs = 2100;

    public void registerNotification(Notification notification) {
        notifications.add(notification);

        // Narrator
        try {
            Narrator.getNarrator().say(notification.getMessage(), false);
        } catch (Exception ignored) {}

        // Sound
        if (beep != null) {
            try {
                if (beep.isRunning()) {
                    beep.stop();
                }
                beep.setFramePosition(0);
                beep.start();
            } catch (Exception ignored) {}
        }
    }

    private void update() {
        if (notifications.isEmpty()) return;

        notifications.removeIf(notification ->
                notification.getDelta() >= maxAgeMs
        );
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        update();
    }

    public List<Notification> getSnowNotifs() {
        return notifications;
    }

    public int getMaxAgeMs() {
        return maxAgeMs;
    }
}