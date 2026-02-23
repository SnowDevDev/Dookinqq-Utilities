package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

public class SnowAntiOpium extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> entityThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("entity-threshold")
        .description("Maximum entities per chunk before warning.")
        .defaultValue(200)
        .range(100, 500)
        .build()
    );

    private final Setting<AlertMode> alertMode = sgGeneral.add(new EnumSetting.Builder<AlertMode>()
        .name("alert-mode")
        .description("How to alert the player.")
        .defaultValue(AlertMode.Chat)
        .build()
    );

    public enum AlertMode {
        Chat, Disconnect
    }

    public SnowAntiOpium() {
        super(AddonTemplate.CATEGORY, "Dookinqq Anti Opium", "Warns of potential chunk ban exploits.");
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        ChunkPos chunk = event.chunk().getPos();
        int entityCount = mc.world.getEntitiesByClass(Entity.class, new Box(
            chunk.getStartX(), -64, chunk.getStartZ(),
            chunk.getEndX(), 320, chunk.getEndZ()
        ), e -> true).size();

        if (entityCount > entityThreshold.get()) {
            String warning = "Warning: High entity count (" + entityCount + ") in chunk " + chunk;
            if (alertMode.get() == AlertMode.Chat) {
                mc.player.sendMessage(Text.of(warning), false);
            } else {
                mc.player.networkHandler.getConnection().disconnect(Text.of(warning));
            }
        }
    }
}



