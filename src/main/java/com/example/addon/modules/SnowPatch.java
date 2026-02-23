package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;

public class SnowPatch extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> grimVelocityPatch = sgGeneral.add(
        new BoolSetting.Builder()
            .name("grim-velocity-patch")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> silentSwapFix = sgGeneral.add(
        new BoolSetting.Builder()
            .name("silent-swap-fix")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiTotemPop = sgGeneral.add(
        new BoolSetting.Builder()
            .name("anti-totem-pop")
            .description("Ports Dookinqq No Pop into Patch. Cancels on-ground packets right after mace attacks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> noPopWindow = sgGeneral.add(
        new IntSetting.Builder()
            .name("no-pop-window")
            .description("How many ticks on-ground packets are cancelled after a mace attack.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 20)
            .visible(antiTotemPop::get)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(
        new BoolSetting.Builder()
            .name("debug")
            .defaultValue(false)
            .build()
    );

    private int noPopTimer = 0;

    public SnowPatch() {
        super(AddonTemplate.CATEGORY, "Dookinqq Patch", "Packet hotfixes and desync prevention.");
    }

    // Mirrors vanilla interact packet action order.
    private enum InteractType {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (noPopTimer > 0) noPopTimer--;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (antiTotemPop.get() && mc.player != null) {
            if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
                if (isAttackPacket(packet) && mc.player.getMainHandStack().getItem() == Items.MACE) {
                    noPopTimer = noPopWindow.get();
                    if (debug.get()) info("No Pop armed for " + noPopTimer + " ticks.");
                }
            }

            if (noPopTimer > 0 && event.packet instanceof PlayerMoveC2SPacket movePacket) {
                if (movePacket.isOnGround()) {
                    event.cancel();
                    if (debug.get()) info("Blocked on-ground packet (No Pop).");
                    return;
                }
            }
        }

        if (!grimVelocityPatch.get()) return;

        if (event.packet instanceof ClientCommandC2SPacket packet) {
            if (packet.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                event.cancel();
                if (debug.get()) info("Blocked STOP_SPRINTING packet.");
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!silentSwapFix.get()) return;
        if (mc.player == null) return;

        if (event.packet instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            if (packet.getSyncId() != 0) return;

            int slot = packet.getSlot();
            if (slot >= 36 && slot <= 44) {
                ItemStack packetStack = packet.getStack();
                ItemStack handStack = mc.player.getMainHandStack();

                if (!packetStack.isEmpty()
                    && !handStack.isEmpty()
                    && ItemStack.areItemsEqual(packetStack, handStack)
                    && packetStack.getCount() == handStack.getCount()) {
                    event.cancel();
                    if (debug.get()) info("Cancelled hotbar sync packet.");
                }
            }
        }
    }

    private boolean isAttackPacket(PlayerInteractEntityC2SPacket packet) {
        Object typeObj = getPacketType(packet);
        if (!(typeObj instanceof Enum<?> typeEnum)) return false;
        return typeEnum.ordinal() == InteractType.ATTACK.ordinal();
    }

    private Object getPacketType(PlayerInteractEntityC2SPacket packet) {
        try {
            var field = PlayerInteractEntityC2SPacket.class.getDeclaredField("type");
            field.setAccessible(true);
            return field.get(packet);
        } catch (Exception ignored) {
            return null;
        }
    }
}
