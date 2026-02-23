package com.example.addon.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import com.example.addon.utils.Compat;
import net.minecraft.entity.player.PlayerEntity;
import com.example.addon.AddonTemplate;

import java.util.List;
import java.util.Random;

public class SnowMsgLag extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> messageLength = sgGeneral.add(new IntSetting.Builder()
        .name("message-length")
        .description("The length of the message.")
        .defaultValue(128)
        .min(1)
        .sliderMin(1)
        .sliderMax(1000)
        .build());

    private final Setting<Boolean> keepSending = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-sending")
        .description("Keeps sending the lag messages repeatedly.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between lag messages in ticks.")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .visible(keepSending::get)
        .build());

    private final Setting<Boolean> whisper = sgGeneral.add(new BoolSetting.Builder()
        .name("whisper")
        .description("Whispers the lag message to a random person on the server.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Disables module on kick.")
        .defaultValue(false)
        .build());

    public SnowMsgLag() {
        super(AddonTemplate.CATEGORY, "Dookinqq Message Lag", "Sends dense messages that lag other players on the server.");
    }

    private int timer;

    @Override
    public void onActivate() {
        if (Utils.canUpdate() && !keepSending.get()) {
            if (!whisper.get()) {
                sendLagMessage();
            }
            else {
                sendLagWhisper();
            }
            toggle();
        }
        if (keepSending.get()) timer = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer <= 0) {
            if (Utils.canUpdate() && keepSending.get()) {
                if (!whisper.get()) {
                    sendLagMessage();
                }
                else {
                    sendLagWhisper();
                }
            }
            timer = delay.get();
        }
        else {
            timer--;
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (autoDisable.get() && isActive()) toggle();
    }

    private void sendLagMessage() {
        String message = generateLagMessage();
         ChatUtils.sendPlayerMsg(message);
    }

    private void sendLagWhisper() {
        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
        PlayerEntity player = players.get(new Random().nextInt(players.size()));
        String message = generateLagMessage();

         ChatUtils.sendPlayerMsg("/msg " + Compat.getProfileName(player.getGameProfile()) + " " + message);
    }

    private String generateLagMessage() {
        String message = null;
        for (int i = 0; i < messageLength.get(); i++) {
            message += (char) (Math.floor(Math.random() * 0x1D300) + 0x800);
        }
        return message;
    }
}



