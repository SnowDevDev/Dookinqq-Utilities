package com.example.addon.modules;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.orbit.EventHandler;

import com.example.addon.AddonTemplate;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public class SnowSwap extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay before swapping back.")
        .defaultValue(0)
        .sliderRange(0, 1000)
        .build()
    );

    public SnowSwap() {
        super(AddonTemplate.CATEGORY, "Dookinqq Swap", "Attribute swapping.");
    }

    private void swapBack(int slot) {
        try {
            Thread.sleep(delay.get());
        } catch (Exception ignored) {}

        InvUtils.swap(slot, false);
    }

@EventHandler
private void onSendPacket(PacketEvent.Send event) {
    if (mc.player == null) return;

    if (event.packet instanceof PlayerInteractEntityC2SPacket packet
        && packet.getClass().getSimpleName().equals("Attack")) {

        int oldSlot = com.example.addon.utils.Compat.getSelectedSlot(mc.player.getInventory());
        ItemStack stack = mc.player.getMainHandStack();

        if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)) {

            for (int i = 0; i < 9; i++) {
                ItemStack hotbarStack = mc.player.getInventory().getStack(i);

                if (!hotbarStack.isDamageable()) {
                    InvUtils.swap(i, false);
                    MeteorExecutor.execute(() -> swapBack(oldSlot));
                    break;
                }
            }
        }
    }
}
}


