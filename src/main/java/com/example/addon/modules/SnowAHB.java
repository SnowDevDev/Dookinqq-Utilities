package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;

public class SnowAHB extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Prevent hunger depletion while sprinting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> mining = sgGeneral.add(new BoolSetting.Builder()
        .name("mining")
        .description("Prevent hunger depletion while mining.")
        .defaultValue(false)
        .build()
    );

    public SnowAHB() {
        super(AddonTemplate.CATEGORY, "Dookinqq AHB", "Reduces hunger depletion during activities.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sprint.get() && event.packet instanceof ClientCommandC2SPacket packet) {
            if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                event.cancel();
            }
        }
        if (mining.get() && event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                event.cancel();
            }
        }
    }
}



