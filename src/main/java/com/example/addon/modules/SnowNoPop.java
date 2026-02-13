package com.example.addon.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import com.example.addon.AddonTemplate;

public class SnowNoPop extends Module {
    private int attackTimer = 0;

    public SnowNoPop() {
        super(AddonTemplate.CATEGORY, "Snow No Pop", "Cancels OnGround packets specifically when attacking with Mace.");
    }

    // Local enum to mirror private enum (for ordinals)
    private enum InteractType {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (attackTimer > 0) attackTimer--;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;

        // Interact packet
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            Object typeObj = getPacketType(packet);

            // Compare ordinal with ATTACK
            if (typeObj instanceof Enum<?> typeEnum) {
                if (typeEnum.ordinal() == InteractType.ATTACK.ordinal()) {
                    if (mc.player.getMainHandStack().getItem() == Items.MACE) {
                        attackTimer = 5;
                    }
                }
            }
        }

        // Cancel OnGround move packets
        if (attackTimer > 0 && event.packet instanceof PlayerMoveC2SPacket movePacket) {
            if (movePacket.isOnGround()) event.cancel();
        }
    }

    // Uses reflection to get the private enum field
    private Object getPacketType(PlayerInteractEntityC2SPacket packet) {
        try {
            var field = PlayerInteractEntityC2SPacket.class.getDeclaredField("type"); // name might be obfuscated
            field.setAccessible(true);
            return field.get(packet);
        } catch (Exception e) {
            // Fallback: return null if reflection fails
            return null;
        }
    }
}