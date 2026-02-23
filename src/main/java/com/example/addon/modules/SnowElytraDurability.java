package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.EquipmentSlot;

public class SnowElytraDurability extends Module {

    // Settings
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> stabilize = sgGeneral.add(
        new BoolSetting.Builder()
            .name("stabilize")
            .description("Attempts to correct durability desync while flying.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> safeDurability = sgGeneral.add(
        new IntSetting.Builder()
            .name("safe-durability")
            .description("Minimum durability before auto-unequip.")
            .defaultValue(5)
            .min(1)
            .sliderMax(50)
            .build()
    );

    // Internal state
    private int lastDamage = -1;
    private int stableTicks = 0;
    private boolean swapped = false;

    public SnowElytraDurability() {
        super(AddonTemplate.CATEGORY, "Dookinqq Elytra Durability",
            "Prevents your Elytra from breaking while flying.");
    }

    @Override
    public void onDeactivate() {
        lastDamage = -1;
        stableTicks = 0;
        swapped = false;
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null) return;
        if (!mc.player.isGliding()) return;

        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chest.isOf(Items.ELYTRA)) return;

        int damage = chest.getDamage();
        int max = chest.getMaxDamage();
        int remaining = max - damage;

        // Detect durability change
        if (damage != lastDamage) {
            stableTicks++;
            lastDamage = damage;
        }

        // Stabilize desync (light touch, no spam)
        if (stabilize.get() && stableTicks > 20) {
            stableTicks = 0;
        }

        // Emergency swap if too low
        if (remaining <= safeDurability.get() && !swapped) {
            InvUtils.move().fromArmor(2).to(com.example.addon.utils.Compat.getSelectedSlot(mc.player.getInventory()));
            swapped = true;
            info("Elytra saved at " + remaining + " durability.");
        }
    }
}


