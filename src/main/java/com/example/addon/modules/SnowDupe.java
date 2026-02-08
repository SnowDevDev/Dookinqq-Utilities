package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;


public class SnowDupe extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> apDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("apDelay")
        .description("dupe me trooper 1.")
        .defaultValue(30.0)
        .range(1.0, 100.0)
        .build()
    );

    private final Setting<Double> apbDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("apbDelay")
        .description("dupe me trooper 2.")
        .defaultValue(20.0)
        .range(1.0, 100.0)
        .build()
    );

    private final Setting<Double> packetcount = sgGeneral.add(new DoubleSetting.Builder()
        .name("packetcount")
        .description("dupe me trooper 3.")
        .defaultValue(20.0)
        .range(1.0, 100.0)
        .build()
    );
    public static boolean soundsenabled = true;
    private static Thread t;


    public SnowDupe() {
        super(AddonTemplate.CATEGORY, "Snow Dupe", "all heil hausemaster and buddy");
    }



    @Override
    public void onActivate() {
        System.out.println("FUNGUJU");
        MinecraftClient client = MinecraftClient.getInstance();

        if (t != null) {
            client.player.sendMessage(Text.literal("Already started"), false);
            return;
        }

        if (!checkConditions()) return;

        ClientPlayerEntity player = client.player;
        client.player.sendMessage(Text.literal("allahuakbar"), false);

        t = Thread.ofVirtual().start(() -> {
            // Prepare modified stacks map
            Int2ObjectMap<ItemStack> state = new Int2ObjectArrayMap<>();

            client.player.sendMessage(Text.literal("bismillah"), false);

            while (!Thread.interrupted()) {
                // Pickup bundle
                client.getNetworkHandler().sendPacket(
                    new ClickSlotC2SPacket(
                        player.currentScreenHandler.syncId,
                        player.currentScreenHandler.getRevision(),
                        0, // slot index
                        0, // button (0 = left click)
                        SlotActionType.PICKUP,
                        player.getInventory().getMainHandStack(),
                        state
                    )
                );

                // Send extra packets
int packets = packetcount.get().intValue();

for (int i = 0; i < packets; i++) {
    client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(36));
}

                try {
                    Thread.sleep(apDelay.get().longValue());
                } catch (InterruptedException e) {
                    break;
                }

                // Pickup bundle again
                client.getNetworkHandler().sendPacket(
                    new ClickSlotC2SPacket(
                        player.currentScreenHandler.syncId,
                        player.currentScreenHandler.getRevision(),
                        0,
                        0,
                        SlotActionType.PICKUP,
                        player.getInventory().getMainHandStack(),
                        state
                    )
                );

                try {
                    Thread.sleep(apbDelay.get().longValue());
                } catch (InterruptedException e) {
                    break;
                }

                // Drop first item if bundle changed
                ItemStack firstStack = player.getInventory().getMainHandStack();
                if (!firstStack.isOf(Items.BUNDLE) && !firstStack.isEmpty()) {
                    player.dropItem(firstStack, true);

                    // Pickup after drop
                    client.getNetworkHandler().sendPacket(
                        new ClickSlotC2SPacket(
                            player.currentScreenHandler.syncId,
                            player.currentScreenHandler.getRevision(),
                            0,
                            0,
                            SlotActionType.PICKUP,
                            player.getInventory().getMainHandStack(),
                            state
                        )
                    );

                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        break;
                    }

                    // Drop outside inventory
                    client.getNetworkHandler().sendPacket(
                        new ClickSlotC2SPacket(
                            player.currentScreenHandler.syncId,
                            player.currentScreenHandler.getRevision(),
                            0,
                            -999, // drop outside inventory
                            SlotActionType.PICKUP,
                            player.getInventory().getMainHandStack(),
                            state
                        )
                    );

                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            client.player.sendMessage(Text.literal("t1 end"), false);
        });
    }

    @Override
    public void onDeactivate() {
        if (t == null) {
            System.out.println("Already stopped");
            return;
        }

        t.interrupt();
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        t = null;
        System.out.println("Dupe thread stopped.");
    }

    public static boolean checkConditions() {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack stack = client.player.getInventory().getMainHandStack();

        if (!stack.isOf(Items.BUNDLE)) {
            client.player.sendMessage(Text.literal("No bundle in first slot"), false);
            return false;
        }

        if (stack.isEmpty()) {
            client.player.sendMessage(Text.literal("Bundle empty"), false);
            return false;
        }

        if (!(client.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            client.player.sendMessage(Text.literal("Not default screen handler, close all screens for real"), false);
            return false;
        }

        return true;
    }
}
