package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SnowAAC extends Module {
    private static final int REQUIRED_SAMPLES = 5;

    private final List<Integer> transactions = new ArrayList<>();
    private boolean capturing;

    public SnowAAC() {
        super(AddonTemplate.CATEGORY, "Dookinqq AAC", "Attempts to guess the server anti-cheat using transaction IDs.");
    }

    @Override
    public void onActivate() {
        reset();
        capturing = true;
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        Packet<?> packet = event.packet;

        if (packet instanceof GameJoinS2CPacket) {
            reset();
            capturing = true;
            return;
        }

        if (!capturing) return;
        if (!(packet instanceof CommonPingS2CPacket ping)) return;

        transactions.add(ping.getParameter());
        if (transactions.size() > 10) {
            transactions.remove(0);
        }

        if (transactions.size() >= REQUIRED_SAMPLES) {
            capturing = false;
            // Schedule work on the client thread so chat/render helpers are called safely.
            mc.execute(() -> announceGuess(new ArrayList<>(transactions)));
        }
    }

    private void announceGuess(List<Integer> samples) {
        String guess = guessAntiCheat(samples);
        if (guess == null) return;

        info("Detected anti-cheat: %s.", guess);
    }

    private String guessAntiCheat(List<Integer> samples) {
        if (samples.size() < REQUIRED_SAMPLES) return null;

        String address = null;
        ServerInfo serverEntry = mc.getCurrentServerEntry();
        if (serverEntry != null) {
            address = serverEntry.address;
        }

        if (address != null && address.toLowerCase(Locale.ROOT).endsWith("hypixel.net")) {
            return "Watchdog";
        }

        List<Integer> diffs = new ArrayList<>(Math.max(0, samples.size() - 1));
        for (int i = 1; i < samples.size(); i++) {
            diffs.add(samples.get(i) - samples.get(i - 1));
        }

        int first = samples.get(0);

        if (!diffs.isEmpty() && diffs.stream().allMatch(d -> d.equals(diffs.get(0)))) {
            int diff = diffs.get(0);
            if (diff == 1) {
                if (inRange(first, -23772, -23762)) return "Vulcan";
                if (inRange(first, 95, 105) || inRange(first, -20005, -19995)) return "Matrix";
                if (inRange(first, -32773, -32762)) return "Grizzly";
                return "Verus";
            } else if (diff == -1) {
                if (inRange(first, -8287, -8280)) return "Errata";
                if (first < -3000) return "Intave";
                if (inRange(first, -5, 0)) return "Grim";
                if (inRange(first, -3000, -2995)) return "Karhu";
                return "Polar";
            }
        }

        if (samples.size() >= 2 && samples.get(0).equals(samples.get(1))) {
            boolean ascendingByOne = true;
            for (int i = 2; i < samples.size(); i++) {
                if (samples.get(i) - samples.get(i - 1) != 1) {
                    ascendingByOne = false;
                    break;
                }
            }
            if (ascendingByOne) return "Verus";
        }

        if (diffs.size() >= 2 && diffs.get(0) >= 100 && diffs.get(1) == -1) {
            boolean restNegOne = true;
            for (int i = 2; i < diffs.size(); i++) {
                if (diffs.get(i) != -1) {
                    restNegOne = false;
                    break;
                }
            }
            if (restNegOne) return "Polar";
        }

        if (first < -3000 && samples.stream().anyMatch(t -> t == 0)) {
            return "Intave";
        }

        if (samples.size() >= 3
            && samples.get(0) == -30767
            && samples.get(1) == -30766
            && samples.get(2) == -25767) {
            boolean ascendingByOne = true;
            for (int i = 3; i < samples.size(); i++) {
                if (samples.get(i) - samples.get(i - 1) != 1) {
                    ascendingByOne = false;
                    break;
                }
            }
            if (ascendingByOne) return "Old Vulcan";
        }

        return "Unknown";
    }

    private boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private void reset() {
        transactions.clear();
        capturing = false;
    }
}



