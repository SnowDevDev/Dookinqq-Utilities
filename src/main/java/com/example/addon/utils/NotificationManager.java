package com.example.addon.utils;

import com.mojang.text2speech.Narrator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.orbit.EventHandler;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {

    public static final NotificationManager INSTANCE = new NotificationManager();

    private Clip beep;
    private final List<Notification> notifications = new ArrayList<>();
    private final int maxAgeMs = 2100;

    private NotificationManager() {
        loadSound();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void loadSound() {
        try {
            InputStream src = getClass().getResourceAsStream("/assets/snow/beep.wav");
            if (src == null) return;

            AudioInputStream stream =
                AudioSystem.getAudioInputStream(new BufferedInputStream(src));

            beep = AudioSystem.getClip();
            beep.open(stream);

        } catch (Exception ignored) {}
    }

    public void registerNotification(Notification notification) {
        notifications.add(notification);

        try {
            Narrator.getNarrator().say(notification.getMessage(), false, 1.0f);
        } catch (Exception ignored) {}

        if (beep != null) {
            try {
                if (beep.isRunning()) beep.stop();
                beep.setFramePosition(0);
                beep.start();
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    private void onRender(Render2DEvent e) {
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            if (it.next().getDelta() >= maxAgeMs) {
                it.remove();
            }
        }
    }

    public List<Notification> getSnowNotifs() {
        return notifications;
    }

    public int getMaxAgeMs() {
        return maxAgeMs;
    }
}