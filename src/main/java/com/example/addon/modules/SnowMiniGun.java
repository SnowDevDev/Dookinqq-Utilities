//written by [agreed](https://github.com/aisiaiiad)
package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import com.example.addon.AddonTemplate;

public class SnowMiniGun extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between shots (in ticks)")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> correctSequence = sgGeneral.add(new BoolSetting.Builder()
            .name("correct-sequence")
            .description("Use correct sequence.")
            .defaultValue(true)
            .build()
    );

    private int timer = 0;

    public SnowMiniGun() {
        super(AddonTemplate.CATEGORY, "Dookinqq Mini Gun", "Turns your crossbow into a machine gun. Hold right click to activate! Thank you to agreed!");
    }

@EventHandler
private void onTick(TickEvent.Pre event) {
    if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

    // Handle Delay
    if (delay.get() > 0) {
        if (timer < delay.get()) {
            timer++;
            return;
        }
        timer = 0;
    }

    // Check if holding a crossbow and pressing use key
    boolean mainHand = mc.player.getMainHandStack().getItem() == Items.CROSSBOW;
    boolean offHand = mc.player.getOffHandStack().getItem() == Items.CROSSBOW;

    if (!(mainHand || offHand) || !mc.options.useKey.isPressed()) return;

    Hand hand = mainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;

    // 1. Tell the client-side interaction manager to use the item
    mc.interactionManager.interactItem(mc.player, hand);

    // 2. Send the packet to the server with the correct sequence
    int sequence = correctSequence.get() ? 0 : 0; // Standard fallback
// Or if you want to properly track sequences:
// int sequence = mc.getNetworkHandler().getPendingSequence().increment();
    
    mc.getNetworkHandler().sendPacket(
        new PlayerInteractItemC2SPacket(hand, sequence, mc.player.getYaw(), mc.player.getPitch())
    );
}
}


