// SnowGhost.java
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

public class SnowGhost extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> fullFood = sgGeneral.add(new BoolSetting.Builder()
        .name("full-food")
        .description("Sets the food level client-side to max.")
        .defaultValue(true)
        .build()
    );

    public SnowGhost() {
        super(AddonTemplate.CATEGORY, "Snow Ghost", "Allows you to keep playing after you die.");
    }

    private boolean active = false;

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        active = false;
        warning("You are no longer in a ghost mode!");
        // Cannot respawn client-side directly; just reset health/food if needed
        if (mc.player != null) {
            mc.player.setHealth(20f);
            mc.player.getHungerManager().setFoodLevel(20);
            info("Health and food reset.");
        }
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        active = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!active || mc.player == null) return;

        if (mc.player.getHealth() < 1f) mc.player.setHealth(20f);
        if (fullFood.get() && mc.player.getHungerManager().getFoodLevel() < 20) {
            mc.player.getHungerManager().setFoodLevel(20);
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof DeathScreen) {
            event.cancel();
            if (!active) {
                active = true;
                info("You are now in ghost mode.");
            }
        }
    }
}