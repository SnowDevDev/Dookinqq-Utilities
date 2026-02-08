package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import com.mojang.brigadier.suggestion.Suggestion;

import java.util.List;
import java.util.stream.Collectors;

public class SnowFetch extends Module {
    public SnowFetch() {
        super(AddonTemplate.CATEGORY, "Snow Fetch", "liquidbounce's plugin fetcher skidded into meteor");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(0, "/"));
    }

    public void onReceiveSuggestions(CommandSuggestionsS2CPacket packet) {
        List<String> plugins = packet.getSuggestions().getList().stream()
            .map(Suggestion::getText)
            .filter(s -> s.contains(":"))
            .map(s -> s.split(":")[0].replace("/", ""))
            .distinct()
            .collect(Collectors.toList());

        if (plugins.isEmpty()) {
            error("No plugins found.");
        } else {
            info("Plugins found (%d): %s", plugins.size(), String.join(", ", plugins));
        }
        
        this.toggle();
    }
}